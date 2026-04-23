package com.cetotos.polydroid2

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import kotlin.concurrent.thread

object ClientProxy {
    private const val TAG = "PolyDroid2-ClientProxy"
    private const val UPSTREAM = "api.polytoria.com"
    private const val KEYSTORE_PASS = "polydroid2"

    @Volatile private var server: SSLServerSocket? = null
    @Volatile private var running = false
    @Volatile private var actualPort = 0

    fun port(): Int = actualPort

    fun start(ctx: Context): Int {
        if (running) return actualPort
        val ks = KeyStore.getInstance("PKCS12")
        ctx.assets.open("proxy_server.p12").use { ks.load(it, KEYSTORE_PASS.toCharArray()) }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, KEYSTORE_PASS.toCharArray())
        val sc = SSLContext.getInstance("TLS")
        sc.init(kmf.keyManagers, null, null)

        val srv = (sc.serverSocketFactory.createServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            as SSLServerSocket)
        server = srv
        actualPort = srv.localPort
        running = true
        Log.i(TAG, "ClientProxy:$actualPort -> $UPSTREAM")
        thread(name = "clientProxy-accept", isDaemon = true) {
            while (running) {
                val sock = try { srv.accept() } catch (_: Exception) { null } ?: continue
                thread(name = "clientProxy-client", isDaemon = true) { handle(sock) }
            }
        }
        return actualPort
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        server = null
    }

    private fun handle(sock: java.net.Socket) {
        try {
            sock.use { s ->
                val inBuf = BufferedInputStream(s.getInputStream())
                val out = s.getOutputStream()
                while (running) {
                    if (!proxyOne(inBuf, out)) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "client error: ${e.message}")
        }
    }

    // returns true if another request follow on the socket
    private fun proxyOne(inBuf: BufferedInputStream, out: OutputStream): Boolean {
        val head = readHead(inBuf) ?: return false
        val lines = String(head, Charsets.ISO_8859_1).split("\r\n")
        if (lines.isEmpty()) return false
        val request = lines[0]
        val parts = request.split(" ")
        if (parts.size < 3) return false
        val method = parts[0]
        val path = parts[1]

        val headers = linkedMapOf<String, String>()
        for (i in 1 until lines.size) {
            val l = lines[i]
            if (l.isEmpty()) break
            val c = l.indexOf(':')
            if (c <= 0) continue
            headers[l.substring(0, c).trim()] = l.substring(c + 1).trim()
        }

        val contentLength = headers.entries.firstOrNull { it.key.equals("Content-Length", true) }
            ?.value?.toIntOrNull() ?: 0
        var body = if (contentLength > 0) ByteArray(contentLength).also { readFully(inBuf, it) } else null

        val keepAlive = (headers.entries.firstOrNull { it.key.equals("Connection", true) }?.value
            ?: "keep-alive").lowercase().contains("keep-alive")


        if (path == "/v1/game/client/connect" && body != null) {
            try {
                body = rebuildConnectBody(body)
                headers["Content-Length"] = body.size.toString()
                Log.i(TAG, "connect body rebuilt (${body.size}B)")
            } catch (e: Exception) {
                Log.w(TAG, "connect body rebuild failed: ${e.message}")
            }
        }

        Log.i(TAG, "$method $UPSTREAM$path")

        val url = URL("https://$UPSTREAM$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        for ((k, v) in headers) {
            if (k.equals("Host", true)) continue
            if (k.equals("Connection", true)) continue
            if (k.equals("Content-Length", true)) continue
            if (k.equals("Transfer-Encoding", true)) continue
            try { conn.setRequestProperty(k, v) } catch (_: Exception) {}
        }
        if (body != null) {
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }
        }

        val code = try { conn.responseCode } catch (e: Exception) {
            Log.w(TAG, "upstream failed: ${e.message}")
            return writeError(out, 502, "proxy upstream: ${e.message}")
        }
        val respBody = (if (code >= 400) conn.errorStream else conn.inputStream)?.use { it.readBytes() } ?: ByteArray(0)

        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(code).append(' ').append(conn.responseMessage ?: "").append("\r\n")
        for ((k, v) in conn.headerFields) {
            if (k == null) continue
            if (k.equals("Transfer-Encoding", true)) continue
            if (k.equals("Content-Length", true)) continue
            if (k.equals("Connection", true)) continue
            for (vv in v) sb.append(k).append(": ").append(vv).append("\r\n")
        }
        sb.append("Content-Length: ").append(respBody.size).append("\r\n")
        sb.append("Connection: ").append(if (keepAlive) "keep-alive" else "close").append("\r\n")
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        if (respBody.isNotEmpty()) out.write(respBody)
        out.flush()
        return keepAlive
    }

    private fun writeError(out: OutputStream, code: Int, msg: String): Boolean {
        val body = msg.toByteArray(Charsets.UTF_8)
        val hdr = "HTTP/1.1 $code proxy error\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
        try {
            out.write(hdr.toByteArray(Charsets.ISO_8859_1))
            out.write(body)
            out.flush()
        } catch (_: Exception) {}
        return false
    }
    private const val PASSPHRASE = "5ZnNWJHc7KmntXxc"

    private var cachedDeviceUuid: String? = null
    private fun deviceUuid(): String {
        cachedDeviceUuid?.let { return it }
        val rng = java.security.SecureRandom()
        val bytes = ByteArray(16).also { rng.nextBytes(it) }
        val hex = bytes.joinToString("") { "%02x".format(it) }
        cachedDeviceUuid = hex
        return hex
    }

    private fun rebuildConnectBody(original: ByteArray): ByteArray {
        val text = String(original, Charsets.ISO_8859_1)
        val fields = linkedMapOf<String, String>()
        for (kv in text.split('&')) {
            val eq = kv.indexOf('='); if (eq <= 0) continue
            fields[kv.substring(0, eq)] = java.net.URLDecoder.decode(kv.substring(eq + 1), "UTF-8")
        }
        val token = fields["token"] ?: error("body missing token")
        val timestamp = fields["timestamp"] ?: ((System.currentTimeMillis() / 1000).toString())
        val origTelemetryB64 = fields["telemetry"] ?: error("body missing telemetry")

        val key = MessageDigest.getInstance("SHA-256").digest(PASSPHRASE.toByteArray(Charsets.US_ASCII))
        val zeroIv = IvParameterSpec(ByteArray(16))
        val aesKey = SecretKeySpec(key, "AES")

        val decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decCipher.init(Cipher.DECRYPT_MODE, aesKey, zeroIv)
        val plaintext = String(
            decCipher.doFinal(android.util.Base64.decode(origTelemetryB64, android.util.Base64.DEFAULT)),
            Charsets.UTF_8,
        )

        val patched = plaintext.replace(
            Regex("\"DeviceUniqueIdentifier\"\\s*:\\s*\"[^\"]*\""),
            "\"DeviceUniqueIdentifier\":\"${deviceUuid()}\"",
        )

        val encCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encCipher.init(Cipher.ENCRYPT_MODE, aesKey, zeroIv)
        val ctext = encCipher.doFinal(patched.toByteArray(Charsets.UTF_8))
        val telemetryB64 = android.util.Base64.encodeToString(ctext, android.util.Base64.NO_WRAP)

        val sigInput = (telemetryB64 + timestamp + token).toByteArray(Charsets.US_ASCII)
        val sigBytes = MessageDigest.getInstance("SHA-256").digest(sigInput)
        val signatureB64 = android.util.Base64.encodeToString(sigBytes, android.util.Base64.NO_WRAP)

        val encoded = buildString {
            append("telemetry=").append(java.net.URLEncoder.encode(telemetryB64, "UTF-8"))
            append("&timestamp=").append(java.net.URLEncoder.encode(timestamp, "UTF-8"))
            append("&token=").append(java.net.URLEncoder.encode(token, "UTF-8"))
            append("&signature=").append(java.net.URLEncoder.encode(signatureB64, "UTF-8"))
        }
        return encoded.toByteArray(Charsets.ISO_8859_1)
    }

    private fun readHead(inp: InputStream): ByteArray? {
        val buf = ByteArrayOutputStream(512)
        var state = 0
        while (true) {
            val b = inp.read()
            if (b < 0) return if (buf.size() == 0) null else buf.toByteArray()
            buf.write(b)
            state = when {
                b == '\r'.code && (state == 0 || state == 2) -> state + 1
                b == '\n'.code && (state == 1 || state == 3) -> state + 1
                else -> 0
            }
            if (state == 4) {
                val all = buf.toByteArray()
                return all.copyOf(all.size - 4)
            }
            if (buf.size() > 32 * 1024) return null
        }
    }

    private fun readFully(inp: InputStream, dst: ByteArray) {
        var off = 0
        while (off < dst.size) {
            val n = inp.read(dst, off, dst.size - off)
            if (n <= 0) throw java.io.EOFException("short body")
            off += n
        }
    }
}
