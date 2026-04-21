package com.cetotos.polydroid2

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.util.Log
import kotlin.concurrent.thread

object AudioBridge {
    private const val TAG = "PolyDroid2-Audio"
    private const val SOCKET_NAME = "polydroid_audio"
    private const val HEADER_BYTES = 12
    private const val MAGIC = 0x31414450

    private const val PA_SAMPLE_U8 = 0
    private const val PA_SAMPLE_S16LE = 3
    private const val PA_SAMPLE_FLOAT32LE = 5

    @Volatile private var running = false
    private var server: LocalServerSocket? = null

    fun start() {
        if (running) return
        running = true
        thread(name = "audio-bridge", isDaemon = true) { acceptLoop() }
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        server = null
    }

    private fun acceptLoop() {
        try {
            val s = LocalServerSocket(SOCKET_NAME)
            server = s
            Log.i(TAG, "listening on abstract @$SOCKET_NAME")
            while (running) {
                val client = try { s.accept() } catch (_: Exception) { null } ?: continue
                thread(name = "audio-client", isDaemon = true) { handleClient(client) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "bridge failed: ${e.message}", e)
        }
    }

    private fun handleClient(client: LocalSocket) {
        var track: AudioTrack? = null
        try {
            val input = client.inputStream
            val header = ByteArray(HEADER_BYTES)
            if (!readExact(input, header)) return
            val magic = u32(header, 0)
            val rate = u32(header, 4)
            val channels = header[8].toInt() and 0xff
            val format = header[9].toInt() and 0xff
            if (magic != MAGIC) {
                Log.w(TAG, "bad magic 0x${"%08x".format(magic)}"); return
            }
            val encoding = when (format) {
                PA_SAMPLE_U8 -> AudioFormat.ENCODING_PCM_8BIT
                PA_SAMPLE_S16LE -> AudioFormat.ENCODING_PCM_16BIT
                PA_SAMPLE_FLOAT32LE -> AudioFormat.ENCODING_PCM_FLOAT
                else -> {
                    Log.w(TAG, "unsupported pa format $format, forcing s16"); AudioFormat.ENCODING_PCM_16BIT
                }
            }
            val channelMask = if (channels >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
            val bytesPerSample = when (encoding) {
                AudioFormat.ENCODING_PCM_8BIT -> 1
                AudioFormat.ENCODING_PCM_16BIT -> 2
                AudioFormat.ENCODING_PCM_FLOAT -> 4
                else -> 2
            }
            val frameSize = bytesPerSample * if (channels >= 2) 2 else 1
            val minBuf = AudioTrack.getMinBufferSize(rate, channelMask, encoding)
            val bufSize = maxOf(minBuf * 2, 16384)
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(rate)
                    .setChannelMask(channelMask)
                    .build())
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            track.play()
            Log.i(TAG, "opened track rate=$rate ch=$channels encoding=$encoding buf=$bufSize")

            val buf = ByteArray(8192)
            var leftover = 0
            while (running) {
                val n = input.read(buf, leftover, buf.size - leftover)
                if (n <= 0) break
                val total = n + leftover
                val aligned = total - (total % frameSize)
                var off = 0
                while (off < aligned) {
                    val w = track.write(buf, off, aligned - off, AudioTrack.WRITE_BLOCKING)
                    if (w <= 0) break
                    off += w
                }
                leftover = total - aligned
                if (leftover > 0) {
                    System.arraycopy(buf, aligned, buf, 0, leftover)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "client error: ${e.message}")
        } finally {
            try { track?.stop(); track?.release() } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun readExact(input: java.io.InputStream, buf: ByteArray): Boolean {
        var read = 0
        while (read < buf.size) {
            val n = input.read(buf, read, buf.size - read)
            if (n <= 0) return false
            read += n
        }
        return true
    }

    private fun u32(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xff) or
        ((b[o + 1].toInt() and 0xff) shl 8) or
        ((b[o + 2].toInt() and 0xff) shl 16) or
        ((b[o + 3].toInt() and 0xff) shl 24)
}
