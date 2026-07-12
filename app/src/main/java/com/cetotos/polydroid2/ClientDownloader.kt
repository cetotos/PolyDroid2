package com.cetotos.polydroid2

import android.content.Context
import android.system.Os
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.json.JSONObject
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipInputStream

object ClientDownloader {
    private const val TAG = "PolyDroid2"
    private const val LAUNCHER_VERSION = "4.13.0"
    private const val UA = "PolytoriaLauncher/$LAUNCHER_VERSION"
    private const val UPDATES_URL = "https://api.polytoria.com/v1/launcher/updates"
    private const val BINARY = "Polytoria Client.x86_64"
    private const val MARKER = ".pd_client_version"
    private const val SEGMENTS = 6

    // 2.0 still uses "beta" internally
    enum class Channel(val api: String, val label: String) {
        STABLE("stable", "1.0"),
        BETA("beta", "2.0");

        companion object {
            fun fromDeepLinkType(type: String): Channel =
                if (type.endsWith("beta", ignoreCase = true)) BETA else STABLE
        }
    }

    data class ClientInfo(val version: String, val downloadUrl: String)

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun channelDir(ctx: Context, channel: Channel): File =
        File(File(ctx.filesDir, "clients"), channel.api)

    fun installedVersion(ctx: Context, channel: Channel): String? {
        val m = File(channelDir(ctx, channel), MARKER)
        return if (m.exists()) m.readText().trim().ifEmpty { null } else null
    }

    fun delete(ctx: Context, channel: Channel) {
        val dir = channelDir(ctx, channel)
        val link = File(RootFs.rootDir(ctx), "polytoria")
        try {
            val lp = link.toPath()
            if (Files.isSymbolicLink(lp) &&
                Files.readSymbolicLink(lp).toFile().absolutePath.startsWith(dir.absolutePath)) {
                Files.delete(lp)
                RootFs.invalidateClientCache()
            }
        } catch (e: Exception) {
            Log.w(TAG, "symlink delete failed: ${e.message}")
        }
        if (dir.exists()) dir.deleteRecursively()
    }

    fun prepare(ctx: Context, token: String, channel: Channel, onProgress: (Int, String) -> Unit) {
        val dir = channelDir(ctx, channel)
        val installed = installedVersion(ctx, channel)

        var info: ClientInfo? = null
        if (token.isNotBlank()) {
            try {
                info = fetchInfo(token, channel)
            } catch (e: Exception) {
                Log.w(TAG, "client info fetch failed: ${e.message}")
            }
        }
        if (info == null || info.version == installed) {
            if (installed != null) {
                activate(ctx, resolveClientRoot(dir))
                return
            }
            if (File(File(RootFs.rootDir(ctx), "polytoria"), BINARY).exists()) {
                Log.w(TAG, "using already present client, no update info")
                return
            }
            throw IOException("no ${channel.label} client installed and update info unavailable")
        }

        onProgress(0, "Downloading ${channel.label} client…")
        val tmp = File(ctx.cacheDir, "client_${channel.api}.7z")
        try {
            downloadTo(info.downloadUrl, tmp) { done, total ->
                val p = if (total > 0) (done * 90 / total).toInt() else 0
                onProgress(p.coerceIn(0, 90), "Downloading ${channel.label} client…")
            }
            onProgress(92, "Installing ${channel.label} client…")
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()
            extractArchive(tmp, dir, info.downloadUrl)
            finalizeClient(dir)
            File(dir, MARKER).writeText(info.version)
        } finally {
            tmp.delete()
        }

        onProgress(100, "Ready")
        Log.i(TAG, "installed ${channel.label} client ${info.version}")
        activate(ctx, resolveClientRoot(dir))
    }

    private fun fetchInfo(token: String, channel: Channel): ClientInfo? {
        val url = "$UPDATES_URL?os=linux&release=${channel.api}"
        val req = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .header("Authorization", token)
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "updates HTTP ${resp.code}")
                return null
            }
            val json = JSONObject(resp.body?.string() ?: return null)
            if (json.optBoolean("Maintenance")) {
                Log.w(TAG, "launcher API under maintenance")
                return null
            }
            val client = json.optJSONObject("Client") ?: return null
            val dl = client.optString("Download", "")
            val ver = client.optString("Version", "")
            if (dl.isEmpty() || ver.isEmpty()) return null
            return ClientInfo(ver, dl)
        }
    }

    private fun downloadTo(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        var total = -1L
        var ranges = false
        try {
            val head = Request.Builder().url(url).head().header("User-Agent", UA).build()
            http.newCall(head).execute().use { r ->
                total = r.header("Content-Length")?.toLongOrNull() ?: -1L
                ranges = r.header("Accept-Ranges")?.contains("bytes") == true
            }
        } catch (e: Exception) {
            Log.w(TAG, "HEAD failed! single stream: ${e.message}")
        }

        if (total <= 0 || !ranges || total < SEGMENTS * 1_048_576L) {
            downloadSingle(url, dest, onProgress)
            return
        }

        try {
            downloadSegmented(url, dest, total, onProgress)
        } catch (e: RangesNotHonored) {
            Log.w(TAG, "ranges not honored! single stream: ${e.message}")
            downloadSingle(url, dest, onProgress)
            return
        }
        if (dest.length() != total) throw IOException("short download ${dest.length()}/$total")
    }

    private class RangesNotHonored(msg: String) : IOException(msg)

    private fun downloadSegmented(url: String, dest: File, total: Long, onProgress: (Long, Long) -> Unit) {
        RandomAccessFile(dest, "rw").use { it.setLength(total) }
        val done = AtomicLong(0)
        val seg = total / SEGMENTS
        val pool = Executors.newFixedThreadPool(SEGMENTS)
        val errors = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        try {
            (0 until SEGMENTS).map { i ->
                val start = i * seg
                val end = if (i == SEGMENTS - 1) total - 1 else start + seg - 1
                pool.submit {
                    try {
                        downloadRange(url, dest, start, end) { n ->
                            onProgress(done.addAndGet(n), total)
                        }
                    } catch (t: Throwable) {
                        errors.add(t)
                    }
                }
            }.forEach { it.get() }
        } finally {
            pool.shutdown()
        }
        errors.firstOrNull { it is RangesNotHonored }?.let { throw it }
        if (errors.isNotEmpty()) throw errors[0]
    }

    private fun downloadRange(url: String, dest: File, start: Long, end: Long, onBytes: (Long) -> Unit) {
        val req = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("Range", "bytes=$start-$end")
            .build()
        http.newCall(req).execute().use { resp ->
            if (resp.code != 206) throw RangesNotHonored("expected 206 but got ${resp.code}")
            val src = resp.body?.byteStream() ?: throw IOException("empty range body")
            RandomAccessFile(dest, "rw").use { raf ->
                raf.seek(start)
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val n = src.read(buf)
                    if (n < 0) break
                    raf.write(buf, 0, n)
                    onBytes(n.toLong())
                }
            }
        }
    }

    private fun downloadSingle(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("download HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("empty body")
            val total = body.contentLength()
            var done = 0L
            body.byteStream().use { src ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    while (true) {
                        val n = src.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
        }
    }

    private fun extractArchive(archive: File, destDir: File, url: String) {
        val name = url.substringBefore('?').lowercase()
        when {
            name.endsWith(".zip") -> extractZip(archive, destDir)
            name.endsWith(".tar.xz") || name.endsWith(".txz") -> extractTarXz(archive, destDir)
            else -> extract7z(archive, destDir)
        }
    }

    private fun extract7z(archive: File, destDir: File) {
        SevenZFile.builder().setFile(archive).get().use { sz ->
            val buf = ByteArray(1 shl 16)
            var e = sz.nextEntry
            while (e != null) {
                val out = File(destDir, e.name)
                if (e.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { o ->
                        while (true) {
                            val n = sz.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                        }
                    }
                }
                e = sz.nextEntry
            }
        }
    }

    private fun extractZip(archive: File, destDir: File) {
        ZipInputStream(BufferedInputStream(archive.inputStream())).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val out = File(destDir, e.name)
                if (e.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { o -> zip.copyTo(o) }
                }
                e = zip.nextEntry
            }
        }
    }

    private fun extractTarXz(archive: File, destDir: File) {
        archive.inputStream().use { raw ->
            BufferedInputStream(raw, 65536).use { buffered ->
                XZInputStream(buffered).use { xz ->
                    TarArchiveInputStream(xz).use { tar ->
                        var entry = tar.nextEntry
                        while (entry != null) {
                            val out = File(destDir, entry.name)
                            if (entry.isDirectory) {
                                out.mkdirs()
                            } else {
                                out.parentFile?.mkdirs()
                                out.outputStream().use { o -> tar.copyTo(o) }
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        }
    }

    // the binary might be under a subfolder, find the binary directly
    private fun resolveClientRoot(dir: File): File {
        if (File(dir, BINARY).exists()) return dir
        return dir.walkTopDown().firstOrNull { it.isFile && it.name == BINARY }?.parentFile ?: dir
    }

    private fun finalizeClient(dir: File) {
        val root = resolveClientRoot(dir)
        File(root, BINARY).setExecutable(true, false)
        root.walkTopDown().filter { it.isFile && it.name.contains(".so") }.forEach {
            it.setReadable(true, false)
            it.setExecutable(true, false)
        }
    }

    private fun activate(ctx: Context, clientRoot: File) {
        val link = File(RootFs.rootDir(ctx), "polytoria")
        val lp = link.toPath()
        if (Files.isSymbolicLink(lp)) {
            Files.delete(lp)
        } else if (link.exists()) {
            link.deleteRecursively()
        }
        Os.symlink(clientRoot.absolutePath, link.absolutePath)
        RootFs.invalidateClientCache()
    }
}
