package com.cetotos.polydroid2

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

object RootFs {
    private const val TAG = "PolyDroid2"
    private const val VERSION = 7
    private const val LIBS_VERSION = 7

    private val LIB_ASSETS = listOf(
        "turnip/libvulkan_freedreno.so",
        "turnip/libc++_shared.so",
        "turnip/libdrm.so.2",
        "xvfb-bundle.bin",
        "glibc-x86_64/libc.so.6",
        "glibc-x86_64/libpthread.so.0",
        "glibc-x86_64/libdl.so.2",
        "glibc-x86_64/libm.so.6",
        "glibc-x86_64/librt.so.1",
        "glibc-x86_64/libresolv.so.2",
        "glibc-x86_64/ld-linux-x86-64.so.2",
        "glibc-x86_64/libgcc_s.so.1",
        "glibc-x86_64/libstdc++.so.6",
        "glibc-x86_64/libnss_files.so.2",
        "glibc-x86_64/libnss_dns.so.2",
        "glibc-x86_64/libxkbcommon.so.0",
        "glibc-x86_64/libpulse.so.0",
        "glibc-x86_64/libpulse-simple.so.0",
        "glibc-x86_64/libdbus-1.so.3",
        "glibc-x86_64/libfmod_sched.so",
        "glibc-x86_64/libconnect_redirect.so",
        "glibc-x86_64/libtimer_shim.so",
        "libudev_stub.so",
        "libssl.so.1.0.0",
        "libcrypto.so.1.0.0",
        "libssl.so.1.1",
        "libcrypto.so.1.1",
        "libasound.so.2.0.0",
        "x86_64-libs/libdns_resolver.so",
        "x86_64-libs/libunity_crash_fix.so",
        "x86_64-libs/libX11_stub.so",
        "arm64-x11-libs/libandroid-support.so",
        "arm64-x11-libs/libXau.so",
        "arm64-x11-libs/libXdmcp.so",
        "arm64-x11-libs/libxcb.so",
        "arm64-x11-libs/libX11.so",
        "arm64-x11-libs/libXext.so",
        "arm64-x11-libs/libstdc++.so.6",
    )

    fun rootDir(ctx: Context): File = File(ctx.filesDir, "rootfs")

    fun isInstalled(ctx: Context): Boolean {
        val versionFile = File(rootDir(ctx), ".pd_version")
        return versionFile.exists() && versionFile.readText().trim() == VERSION.toString()
    }

    fun areLibsInstalled(ctx: Context): Boolean {
        val versionFile = File(rootDir(ctx), ".pd_libs_version")
        return versionFile.exists() && versionFile.readText().trim() == LIBS_VERSION.toString()
    }

    fun needsExtraction(ctx: Context): Boolean = !isInstalled(ctx) || !areLibsInstalled(ctx)

    private fun assetLen(ctx: Context, name: String): Long = try {
        ctx.assets.openFd(name).use { it.length }
    } catch (_: Exception) { 0L }

    private class CountingInputStream(
        private val wrapped: InputStream,
        private val onDelta: (Long) -> Unit
    ) : InputStream() {
        override fun read(): Int {
            val b = wrapped.read()
            if (b >= 0) onDelta(1L)
            return b
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = wrapped.read(b, off, len)
            if (n > 0) onDelta(n.toLong())
            return n
        }
        override fun close() { wrapped.close() }
        override fun available(): Int = wrapped.available()
    }

    data class Stage(val label: String, val bytes: Long)

    class Progress(
        private val stages: List<Stage>,
        private val cb: (detailPct: Int, stagesDone: Int, totalStages: Int, label: String) -> Unit
    ) {
        private var idx = 0
        private var bytesInStage = 0L
        private var lastPct = -1
        private var bytesSinceEmit = 0L

        @Synchronized
        fun start() {
            if (stages.isNotEmpty()) emit(force = true)
        }
        @Synchronized
        fun addBytes(n: Long) {
            bytesInStage += n
            bytesSinceEmit += n
            if (bytesSinceEmit >= 65536) emit(force = false)
        }
        @Synchronized
        fun completeStage() {
            idx++
            bytesInStage = 0L
            lastPct = -1
            bytesSinceEmit = 0L
            if (idx < stages.size) emit(force = true)
            else cb(100, stages.size, stages.size, "Extraction complete")
        }
        private fun emit(force: Boolean) {
            if (idx >= stages.size) return
            val stage = stages[idx]
            val pct = if (stage.bytes > 0) (bytesInStage * 100 / stage.bytes).toInt().coerceIn(0, 100) else 0
            if (force || pct != lastPct) {
                lastPct = pct
                bytesSinceEmit = 0
                cb(pct, idx, stages.size, stage.label)
            }
        }
    }

    fun extractAll(ctx: Context, onProgress: (Int, Int, Int, String) -> Unit) {
        val needRootfs = !isInstalled(ctx)
        val needLibs = !areLibsInstalled(ctx)

        val stages = mutableListOf<Stage>()
        if (needRootfs) {
            val rootfsBytes = assetLen(ctx, "rootfs.tar.xz")
            val polyBytes = assetLen(ctx, "polytoria_client.txz")
            stages.add(Stage("Extracting rootfs and client...", rootfsBytes + polyBytes))
        }
        if (needLibs) {
            stages.add(Stage("Extracting libraries...", LIB_ASSETS.sumOf { assetLen(ctx, it) }))
        }

        val progress = Progress(stages, onProgress)
        progress.start()

        if (needRootfs) {
            val root = rootDir(ctx)
            if (root.exists()) root.deleteRecursively()
            root.mkdirs()

            val polyError = arrayOfNulls<Throwable>(1)
            val polyThread = Thread({
                try {
                    extractPolytoria(ctx, progress)
                } catch (t: Throwable) {
                    polyError[0] = t
                }
            }, "PolyExtract").apply { start() }

            install(ctx, progress)
            polyThread.join()
            polyError[0]?.let { throw it }

            progress.completeStage()
        }
        if (needLibs) {
            extractLibs(ctx, progress)
            progress.completeStage()
        }
    }

    private fun install(ctx: Context, progress: Progress) {
        val root = rootDir(ctx)
        root.mkdirs()

        Log.i(TAG, "Extracting rootfs...")

        ctx.assets.open("rootfs.tar.xz").use { raw ->
            CountingInputStream(raw) { progress.addBytes(it) }.use { counted ->
                BufferedInputStream(counted, 65536).use { buffered ->
                    XZInputStream(buffered).use { xz ->
                        TarArchiveInputStream(xz).use { tar ->
                            var entry = tar.nextEntry
                            while (entry != null) {
                                val outFile = File(root, entry.name)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else if (entry.isSymbolicLink) {
                                    try {
                                        val target = java.nio.file.Paths.get(entry.linkName)
                                        val link = outFile.toPath()
                                        outFile.parentFile?.mkdirs()
                                        java.nio.file.Files.createSymbolicLink(link, target)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "failed to create symlink: ${entry.name} -> ${entry.linkName}")
                                    }
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { out ->
                                        tar.copyTo(out)
                                    }
                                    if (entry.mode and 0b001_001_001 != 0) {
                                        outFile.setExecutable(true, false)
                                    }
                                }
                                entry = tar.nextEntry
                            }
                        }
                    }
                }
            }
        }
        val polyBinary = File(root, "polytoria/Polytoria Client.x86_64")
        if (polyBinary.exists()) polyBinary.setExecutable(true, false)
        root.resolve("usr/bin").listFiles()?.forEach { it.setExecutable(true, false) }
        root.resolve("usr/sbin").listFiles()?.forEach { it.setExecutable(true, false) }
        File(root, ".pd_version").writeText(VERSION.toString())
        Log.i(TAG, "Rootfs extraction complete")
    }

    private fun extractPolytoria(ctx: Context, progress: Progress) {
        val polyDir = File(rootDir(ctx), "polytoria")
        if (File(polyDir, "Polytoria Client.x86_64").exists()) {
            return
        }

        polyDir.mkdirs()
        Log.i(TAG, "Extracting client...")

        ctx.assets.open("polytoria_client.txz").use { raw ->
            CountingInputStream(raw) { progress.addBytes(it) }.use { counted ->
                BufferedInputStream(counted, 65536).use { buffered ->
                    XZInputStream(buffered).use { xz ->
                        TarArchiveInputStream(xz).use { tar ->
                            var entry = tar.nextEntry
                            while (entry != null) {
                                val outFile = File(polyDir, entry.name)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else if (entry.isSymbolicLink) {
                                    try {
                                        val target = java.nio.file.Paths.get(entry.linkName)
                                        val link = outFile.toPath()
                                        outFile.parentFile?.mkdirs()
                                        java.nio.file.Files.createSymbolicLink(link, target)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "failed to create symlink: ${entry.name} -> ${entry.linkName}")
                                    }
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { out ->
                                        tar.copyTo(out)
                                    }
                                    if (entry.mode and 0b001_001_001 != 0) {
                                        outFile.setExecutable(true, false)
                                    }
                                }
                                entry = tar.nextEntry
                            }
                        }
                    }
                }
            }
        }

        File(polyDir, "Polytoria Client.x86_64").setExecutable(true, false)
        Log.i(TAG, "Extracted successfuly")
    }

    fun deleteSfx(polyDir: File) {
        val res = File(polyDir, "Polytoria Client_Data/resources.resource")
        if (!res.exists()) return
        val marker = File(polyDir, ".pd_sfx_silenced")
        val sizeStr = res.length().toString()
        if (marker.exists() && marker.readText().trim() == sizeStr) return
        try {
            val bytes = res.readBytes()
            var banks = 0
            var bytesZeroed = 0L
            var pos = 0
            while (pos + 24 <= bytes.size) {
                if (bytes[pos] != 'F'.code.toByte() || bytes[pos + 1] != 'S'.code.toByte() ||
                    bytes[pos + 2] != 'B'.code.toByte() || bytes[pos + 3] != '5'.code.toByte()) {
                    pos++
                    continue
                }
                val version = readLE32(bytes, pos + 4)
                val sampleHdrSize = readLE32(bytes, pos + 12)
                val nameTblSize = readLE32(bytes, pos + 16)
                val dataSize = readLE32(bytes, pos + 20)
                val fixedHdr = if (version == 1) 60 else 56
                val dataStart = pos + fixedHdr + sampleHdrSize + nameTblSize
                val dataEnd = dataStart + dataSize
                if (dataEnd > bytes.size || dataSize <= 0) { pos++; continue }
                for (i in dataStart until dataEnd) bytes[i] = 0
                banks++
                bytesZeroed += dataSize.toLong()
                pos = dataEnd
            }
            if (banks > 0) {
                res.writeBytes(bytes)
                marker.writeText(res.length().toString())
                Log.i(TAG, "silenced $banks FSB5 banks ($bytesZeroed bytes)")
            } else {
                Log.i(TAG, "no FSB5 banks found")
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteSfx failed: ${e.message}")
        }
    }

    private fun readLE32(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xff) or
        ((b[o + 1].toInt() and 0xff) shl 8) or
        ((b[o + 2].toInt() and 0xff) shl 16) or
        ((b[o + 3].toInt() and 0xff) shl 24)

    private fun copyAssetCounted(ctx: Context, asset: String, dest: File, progress: Progress) {
        ctx.assets.open(asset).use { raw ->
            CountingInputStream(raw) { progress.addBytes(it) }.use { counted ->
                dest.outputStream().use { output -> counted.copyTo(output) }
            }
        }
    }

    private fun extractLibs(ctx: Context, progress: Progress) {
        val root = rootDir(ctx)
        val rootPath = root.absolutePath
        val nativeDir = ctx.applicationInfo.nativeLibraryDir

        Log.i(TAG, "Extracting libraries...")

        val arm64NativeDir = File("$rootPath/usr/lib/arm64-native")
        arm64NativeDir.mkdirs()
        for (lib in listOf("libvulkan_freedreno.so", "libc++_shared.so", "libdrm.so.2")) {
            val dest = File(arm64NativeDir, lib)
            try {
                copyAssetCounted(ctx, "turnip/$lib", dest, progress)
            } catch (_: Exception) { }
            dest.setReadable(true, false)
            dest.setExecutable(true, false)
        }

        val icdDir = File("$rootPath/usr/share/vulkan/icd.d")
        icdDir.mkdirs()
        File(icdDir, "freedreno_icd.aarch64.json").writeText("""{
    "file_format_version": "1.0.0",
    "ICD": {
        "library_path": "$rootPath/usr/lib/arm64-native/libvulkan_freedreno.so",
        "api_version": "1.3.0"
    }
}
""")

        val xvfbMarker = File("$rootPath/usr/bin/Xvfb")
        if (!xvfbMarker.exists()) {
            try {
                ctx.assets.open("xvfb-bundle.bin").use { raw ->
                    CountingInputStream(raw) { progress.addBytes(it) }.use { counted ->
                        GZIPInputStream(counted).use { gzip ->
                            TarArchiveInputStream(gzip).use { tar ->
                                var entry = tar.nextEntry
                                while (entry != null) {
                                    val dest = File(rootPath, entry.name)
                                    if (entry.isDirectory) {
                                        dest.mkdirs()
                                    } else {
                                        dest.parentFile?.mkdirs()
                                        dest.outputStream().use { out -> tar.copyTo(out) }
                                        if (entry.name.contains("/bin/")) {
                                            dest.setExecutable(true, false)
                                        }
                                        dest.setReadable(true, false)
                                    }
                                    entry = tar.nextEntry
                                }
                            }
                        }
                    }
                }
                Log.i(TAG, "Extracted Xvfb bundle to rootfs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract Xvfb bundle: ${e.message}")
            }
        }

        val x86LibDir = File("$rootPath/usr/lib/x86_64-linux-gnu")
        x86LibDir.mkdirs()
        val glibcLibs = listOf(
            "libc.so.6", "libpthread.so.0", "libdl.so.2", "libm.so.6",
            "librt.so.1", "libresolv.so.2", "ld-linux-x86-64.so.2",
            "libgcc_s.so.1", "libstdc++.so.6",
            "libnss_files.so.2", "libnss_dns.so.2",
            "libxkbcommon.so.0",
            "libpulse.so.0", "libpulse-simple.so.0",
            "libdbus-1.so.3",
            "libfmod_sched.so",
            "libconnect_redirect.so",
            "libtimer_shim.so"
        )
        for (lib in glibcLibs) {
            try {
                val dest = File(x86LibDir, lib)
                copyAssetCounted(ctx, "glibc-x86_64/$lib", dest, progress)
                dest.setExecutable(true, false)
                dest.setReadable(true, false)
            } catch (e: Exception) {
                Log.w(TAG, "glibc asset $lib not found: ${e.message}")
            }
        }
        File(x86LibDir, "libaudio_trace.so").delete()
        Log.i(TAG, "Deployed x86_64 glibc libs: ${glibcLibs.size} files")

        try {
            copyAssetCounted(ctx, "libudev_stub.so", File(x86LibDir, "libudev.so.1"), progress)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deploy libudev stub: ${e.message}")
        }

        for (lib in listOf("libssl.so.1.0.0", "libcrypto.so.1.0.0",
                           "libssl.so.1.1", "libcrypto.so.1.1")) {
            try {
                copyAssetCounted(ctx, lib, File(x86LibDir, lib), progress)
            } catch (_: Exception) {
                Log.w(TAG, "Asset $lib not found, skipping")
            }
        }

        try {
            val dest = File(x86LibDir, "libasound.so.2")
            copyAssetCounted(ctx, "libasound.so.2.0.0", dest, progress)
            dest.setExecutable(true, false)
            dest.setReadable(true, false)
        } catch (e: Exception) {
            Log.w(TAG, "ALSA stub deploy failed: ${e.message}")
        }

        try {
            val dest = File(x86LibDir, "libdns_resolver.so")
            copyAssetCounted(ctx, "x86_64-libs/libdns_resolver.so", dest, progress)
            dest.setExecutable(true, false)
            dest.setReadable(true, false)
        } catch (e: Exception) {
            Log.w(TAG, "DNS resolver shim not found: ${e.message}")
        }

        try {
            val dest = File("$rootPath/polytoria", "libunity_crash_fix.so")
            copyAssetCounted(ctx, "x86_64-libs/libunity_crash_fix.so", dest, progress)
            dest.setExecutable(true, false)
            dest.setReadable(true, false)
        } catch (e: Exception) {
            Log.w(TAG, "Unity crash fix not found: ${e.message}")
        }


        try {
            copyAssetCounted(ctx, "x86_64-libs/libX11_stub.so", File(x86LibDir, "libX11.so.6"), progress)
            File(x86LibDir, "libX11.so.6").setExecutable(true, false)
        } catch (e: Exception) {
            Log.w(TAG, "libX11_stub.so not found in assets: ${e.message}")
        }

        for (lib in listOf("libandroid-support.so", "libXau.so", "libXdmcp.so", "libxcb.so", "libX11.so", "libXext.so", "libstdc++.so.6")) {
            val dest = File(arm64NativeDir, lib)
            copyAssetCounted(ctx, "arm64-x11-libs/$lib", dest, progress)
        }
        val versionedLinks = mapOf(
            "libX11.so" to "libX11.so.6",
            "libXext.so" to "libXext.so.6",
            "libxcb.so" to "libxcb.so.1",
            "libXau.so" to "libXau.so.6",
            "libXdmcp.so" to "libXdmcp.so.6",
        )
        for ((base, versioned) in versionedLinks) {
            val src = File(arm64NativeDir, base)
            if (src.exists()) {
                src.copyTo(File(arm64NativeDir, versioned), overwrite = true)
            }
        }

        val vkShimSrc = File(nativeDir, "libvulkan_surface_shim.so")
        if (vkShimSrc.exists()) {
            vkShimSrc.copyTo(File(arm64NativeDir, "libvulkan.so.1"), overwrite = true)
            File(arm64NativeDir, "libvulkan.so.1").setReadable(true, false)
            File(arm64NativeDir, "libvulkan.so.1").setExecutable(true, false)
        }

        val libDir = File("$rootPath/usr/lib/aarch64-linux-gnu")
        libDir.mkdirs()
        for (lib in listOf("libdbus-1.so")) {
            val src = File(nativeDir, lib)
            if (src.exists()) {
                src.copyTo(File(libDir, lib), overwrite = true)
            }
        }
        val versionedNativeLibs = mapOf("libdbus-1.so" to "libdbus-1.so.3")
        for ((src, versioned) in versionedNativeLibs) {
            val srcFile = File(libDir, src)
            if (srcFile.exists()) {
                srcFile.copyTo(File(libDir, versioned), overwrite = true)
            }
        }

        val overwriteFromArm64Native = mapOf(
            "libX11.so" to listOf("libX11.so", "libX11.so.6", "libX11.so.6.4.0"),
            "libxcb.so" to listOf("libxcb.so", "libxcb.so.1", "libxcb.so.1.1.0"),
            "libXau.so" to listOf("libXau.so", "libXau.so.6"),
            "libXdmcp.so" to listOf("libXdmcp.so", "libXdmcp.so.6"),
            "libandroid-support.so" to listOf("libandroid-support.so"),
        )
        for ((src, dests) in overwriteFromArm64Native) {
            val srcFile = File(arm64NativeDir, src)
            if (srcFile.exists()) {
                for (dest in dests) {
                    srcFile.copyTo(File(libDir, dest), overwrite = true)
                }
            }
        }

        val box64 = File(nativeDir, "libbox64.so")
        if (box64.exists()) {
            val box64Guest = File("$rootPath/usr/bin/box64")
            box64.copyTo(box64Guest, overwrite = true)
            box64Guest.setExecutable(true, false)
        }

        File(root, ".pd_libs_version").writeText(LIBS_VERSION.toString())
        Log.i(TAG, "Library extraction complete")
    }
}
