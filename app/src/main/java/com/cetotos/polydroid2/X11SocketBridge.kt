package com.cetotos.polydroid2

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlin.concurrent.thread

object X11SocketBridge {
    private const val TAG = "PolyDroid2"
    private const val STANDARD_PATH = "/tmp/.X11-unix/X0"
    private var serverSocket: LocalServerSocket? = null

    fun start(realSocketPath: String) {
        thread(name = "x11-bridge", isDaemon = true) {
            try {
                serverSocket = LocalServerSocket(STANDARD_PATH)
                Log.i(TAG, "X11 bridge listening on abstract @$STANDARD_PATH")

                while (true) {
                    val client = serverSocket!!.accept() ?: continue
                    thread(name = "x11-bridge-conn", isDaemon = true) {
                        bridgeConnection(client, realSocketPath)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "X11 bridge error", e)
            }
        }
    }

    private fun bridgeConnection(client: LocalSocket, realSocketPath: String) {
        try {
            val server = LocalSocket()
            server.connect(LocalSocketAddress(realSocketPath, LocalSocketAddress.Namespace.ABSTRACT))

            val clientIn = client.inputStream
            val clientOut = client.outputStream
            val serverIn = server.inputStream
            val serverOut = server.outputStream

            // client -> server
            val t1 = thread(name = "x11-c2s", isDaemon = true) {
                try {
                    val buf = ByteArray(65536)
                    while (true) {
                        val n = clientIn.read(buf)
                        if (n < 0) break
                        serverOut.write(buf, 0, n)
                        serverOut.flush()
                    }
                } catch (_: Exception) {}
                try { server.shutdownOutput() } catch (_: Exception) {}
            }

            // server -> client
            try {
                val buf = ByteArray(65536)
                while (true) {
                    val n = serverIn.read(buf)
                    if (n < 0) break
                    clientOut.write(buf, 0, n)
                    clientOut.flush()
                }
            } catch (_: Exception) {}

            try { client.shutdownOutput() } catch (_: Exception) {}
            t1.join(1000)

            client.close()
            server.close()
        } catch (e: Exception) {
            Log.w(TAG, "X11 bridge connection error: ${e.message}")
            try { client.close() } catch (_: Exception) {}
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }
}
