package com.cetotos.polydroid2

import android.content.Context
import android.util.Log
import java.io.File

object Box64Launcher {
    private const val TAG = "PolyDroid2"

    private var process: Process? = null

    fun launch(ctx: Context, tmpDir: String, gameArgs: String = "", screenWidth: Int = 1280, screenHeight: Int = 720, onLog: (String) -> Unit): Process {
        val root = RootFs.rootDir(ctx)
        val rootPath = root.absolutePath
        val nativeDir = ctx.applicationInfo.nativeLibraryDir

        val polytoria = File(root, "polytoria/Polytoria Client.x86_64")
        require(polytoria.exists()) { "Polytoria binary not found at ${polytoria.absolutePath}" }
        val box64 = File(nativeDir, "libbox64.so")
        require(box64.exists()) { "Box64 binary not found at ${box64.absolutePath}" }
        File(root, "polytoria/libdecor-0.so.0").delete()
        File(root, "polytoria/libdecor-cairo.so").delete()
        File(root, "polytoria/unity.lock").delete()
        File("$rootPath/tmp").mkdirs()
        File("$rootPath/tmp/.X11-unix").apply { mkdirs(); setReadable(true, false); setExecutable(true, false); setWritable(true, false) }
        File("$rootPath/home/user").mkdirs()
        File("$rootPath/proc").mkdirs()
        File("$rootPath/dev").mkdirs()
        val shmDir = File("$rootPath/dev/shm")
        shmDir.mkdirs()
        shmDir.setReadable(true, false)
        shmDir.setWritable(true, false)
        shmDir.setExecutable(true, false)
        File("$rootPath/sys").mkdirs()
        File("$rootPath/etc").mkdirs()
        File("$rootPath/usr/bin").mkdirs()
        File("$rootPath/system").mkdirs()
        File("$rootPath/vendor").mkdirs()
        File("$rootPath/apex").mkdirs()
        val etcDir = File("$rootPath/etc")
        etcDir.mkdirs()
        File(etcDir, "passwd").takeIf { !it.exists() }?.writeText(
            "root:x:0:0:root:/root:/bin/sh\nuser:x:1000:1000:user:/home/user:/bin/sh\n"
        )
        File(etcDir, "group").takeIf { !it.exists() }?.writeText(
            "root:x:0:\nuser:x:1000:\n"
        )
        val hostsBuilder = StringBuilder("127.0.0.1 localhost\n")
        for (hostname in listOf("api.polytoria.com", "polytoria.com")) {
            try {
                val addr = java.net.InetAddress.getByName(hostname)
                hostsBuilder.append("${addr.hostAddress} $hostname\n")
                Log.i(TAG, "Resolved $hostname -> ${addr.hostAddress}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve $hostname: ${e.message}")
            }
        }
        File(etcDir, "hosts").writeText(hostsBuilder.toString())
        File(etcDir, "resolv.conf").delete()
        val dnsServers = getSystemDnsServers(ctx)
        File(etcDir, "resolv.conf").writeText(
            dnsServers.joinToString("\n") { "nameserver $it" } + "\n"
        )
        Log.i(TAG, "resolv.conf DNS: ${dnsServers.joinToString(", ")}")
        File(etcDir, "ld.so.conf").writeText(
            "/usr/lib/x86_64-linux-gnu\n/polytoria\n"
        )

        val sslDir = File("$rootPath/etc/ssl/certs")
        sslDir.mkdirs()
        val caBundle = File(sslDir, "ca-certificates.crt")
        if (!caBundle.exists()) {
            val systemCertsDir = java.io.File("/system/etc/security/cacerts")
            if (systemCertsDir.isDirectory) {
                val sb = StringBuilder()
                systemCertsDir.listFiles()?.sorted()?.forEach { certFile ->
                    try {
                        sb.append(certFile.readText())
                    } catch (_: Exception) {}
                }
                caBundle.writeText(sb.toString())
                Log.i(TAG, "Built CA bundle from ${systemCertsDir.listFiles()?.size ?: 0} system certs")
            } else {
                Log.w(TAG, "No certs found at /system/etc/security/cacerts")
            }
        }
        val libDir = File("$rootPath/usr/lib/aarch64-linux-gnu")
        libDir.mkdirs()
        // dbus stub (Unity tries to dlopen it)
        for (lib in listOf("libdbus-1.so")) {
            val src = File(nativeDir, lib)
            if (src.exists()) {
                src.copyTo(File(libDir, lib), overwrite = true)
            }
        }
        val versionedNativeLibs = mapOf(
            "libdbus-1.so" to "libdbus-1.so.3",
        )
        for ((src, versioned) in versionedNativeLibs) {
            val srcFile = File(libDir, src)
            if (srcFile.exists()) {
                srcFile.copyTo(File(libDir, versioned), overwrite = true)
            }
        }

        val arm64NativeDir = File("$rootPath/usr/lib/arm64-native")
        arm64NativeDir.mkdirs()
        for (lib in listOf("libvulkan_freedreno.so", "libc++_shared.so", "libdrm.so.2")) {
            val dest = File(arm64NativeDir, lib)
            try {
                ctx.assets.open("turnip/$lib").use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) { }
            dest.setReadable(true, false)
            dest.setExecutable(true, false)
        }

        // write Turnip ICD JSON so the vulkan loader finds it
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
                ctx.assets.open("xvfb-bundle.bin").use { asset ->
                    java.util.zip.GZIPInputStream(asset).use { gzip ->
                        org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzip).use { tar ->
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
            "libtimer_shim.so"
        )
        for (lib in glibcLibs) {
            try {
                val dest = File(x86LibDir, lib)
                ctx.assets.open("glibc-x86_64/$lib").use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.setExecutable(true, false)
                dest.setReadable(true, false)
            } catch (e: Exception) {
                Log.w(TAG, "glibc asset $lib not found: ${e.message}")
            }
        }
        for (stale in listOf("libbsd.so.0", "libbsd.so", "libmd.so.0", "libmd.so")) {
            File(x86LibDir, stale).delete()
        }
        Log.i(TAG, "Deployed x86_64 glibc libs (Ubuntu 22.04 / glibc 2.35): ${glibcLibs.size} files")

        try {
            ctx.assets.open("libudev_stub.so").use { input ->
                File(x86LibDir, "libudev.so.1").outputStream().use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "Deployed libudev.so.1 stub")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deploy libudev stub: ${e.message}")
        }

        for (lib in listOf("libssl.so.1.0.0", "libcrypto.so.1.0.0",
                           "libssl.so.1.1", "libcrypto.so.1.1")) {
            try {
                ctx.assets.open(lib).use { input ->
                    File(x86LibDir, lib).outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) {
                Log.w(TAG, "Asset $lib not found, skipping")
            }
        }
        Log.i(TAG, "Deployed x86_64 OpenSSL libs")


        try {
            val dest = File(x86LibDir, "libasound.so.2")
            ctx.assets.open("libasound.so.2.0.0").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.setExecutable(true, false)
            dest.setReadable(true, false)
            Log.i(TAG, "Deployed ALSA stub")
        } catch (e: Exception) {
            Log.w(TAG, "ALSA stub deploy failed: ${e.message}")
        }

        File(x86LibDir, "libglibc_shim.so").delete()
        File(x86LibDir, "libxmissing_stub.so").delete()
        File(x86LibDir, "x11_glx_inject.so").delete()

        try {
            val dest = File(x86LibDir, "libdns_resolver.so")
            ctx.assets.open("x86_64-libs/libdns_resolver.so").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.setExecutable(true, false)
            dest.setReadable(true, false)
            Log.i(TAG, "Deployed x86_64 DNS resolver shim")
        } catch (e: Exception) {
            Log.w(TAG, "DNS resolver shim not found: ${e.message}")
        }

        try {
            ctx.assets.open("x86_64-libs/libX11_stub.so").use { input ->
                File(x86LibDir, "libX11.so.6").outputStream().use { output -> input.copyTo(output) }
            }
            File(x86LibDir, "libX11.so.6").setExecutable(true, false)
            Log.i(TAG, "Deployed libX11_stub.so as libX11.so.6")
        } catch (e: Exception) {
            Log.w(TAG, "libX11_stub.so not found in assets: ${e.message}")
        }

        for (lib in listOf("libandroid-support.so", "libXau.so", "libXdmcp.so", "libxcb.so", "libX11.so", "libXext.so", "libstdc++.so.6")) {
            val dest = File(arm64NativeDir, lib)
            ctx.assets.open("arm64-x11-libs/$lib").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
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

        val env = buildMap {
            put("HOME", "$rootPath/home/user")
            put("USER", "user")
            put("TMPDIR", "$rootPath/tmp")
            put("PATH", "$rootPath/usr/bin:$rootPath/bin")
            put("PREFIX", "$rootPath/usr")
            put("XKB_CONFIG_ROOT", "$rootPath/usr/share/X11/xkb")
            put("XKB_CONFIG_EXTRA_PATH", "$rootPath/usr/share/X11/xkb")
            put("LD_LIBRARY_PATH", "$rootPath/usr/lib/arm64-native:$nativeDir:/system/lib64")
            put("VK_ICD_FILENAMES", "$rootPath/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json")
            put("MESA_VK_WSI_PRESENT_MODE", "mailbox")
            put("BOX64_LD_LIBRARY_PATH",
                "$rootPath/polytoria:" +
                "$rootPath/polytoria/dotnet:" +
                "$rootPath/usr/lib/x86_64-linux-gnu:" +
                "$rootPath/usr/lib")
            put("BOX64_EMULATED_LIBS",
                "libudev.so.1:" +
                "libstdc++.so.6:libgcc_s.so.1:" +
                "libX11.so.6:libxcb.so.1:libXext.so.6:" +
                "libXau.so.6:libXdmcp.so.6:" +
                "libXrandr.so.2:libXi.so.6:libXcursor.so.1:" +
                "libXinerama.so.1:libXss.so.1:libXxf86vm.so.1:" +
                "libXfixes.so.3:libXrender.so.1:" +
                "libasound.so.2:" +
                "libpulse.so.0:libpulse-simple.so.0:" +
                "librt.so.1")

            var ldPreload = ""
            ldPreload = "$rootPath/usr/lib/x86_64-linux-gnu/libpath_remap.so:$rootPath/polytoria/libunity_crash_fix.so:$rootPath/usr/lib/x86_64-linux-gnu/libsysconf_fix.so:$rootPath/usr/lib/x86_64-linux-gnu/libdns_resolver.so:$ldPreload"
            put("BOX64_LD_PRELOAD", ldPreload.trimEnd(':'))
            put("BOX64_LOG", "1") // level 2 will give WAY too much do not use it the Player.log ends up being GIGABYTES big
            put("BOX64_SHOWSEGV", "1")
            put("BOX64_SHOWBT", "1")
            put("BOX64_DLSYM_ERROR", "0")   // reduces log spam from missing symbols
            put("BOX64_CRASHHANDLER", "1")
            // optimizations
            put("BOX64_DYNAREC", "1")
            put("BOX64_DYNAREC_BIGBLOCK", "2")
            put("BOX64_DYNAREC_STRONGMEM", "0")
            put("BOX64_DYNAREC_FASTNAN", "1")
            put("BOX64_DYNAREC_FASTROUND", "1")
            put("BOX64_DYNAREC_SAFEFLAGS", "0")
            put("BOX64_DYNAREC_CALLRET", "1")
            put("BOX64_DYNAREC_ALIGNED_ATOMICS", "1")
            put("BOX64_DYNAREC_BLEEDING_EDGE", "1")
            put("BOX64_DYNAREC_WAIT", "0")
            put("BOX64_DYNAREC_THP", "1")
            put("BOX64_DYNAREC_HOTPAGE", "2")
            put("BOX64_DYNAREC_NATIVEFLAGS", "1")
            put("BOX64_NORCFILES", "1")
            put("BOX64_ALLOWMISSINGLIBS", "1")
            put("BOX64_MMAP32", "0")
            put("BOX64_AVX", "2")
            // rootfs
            put("POLYDROID_ROOTDIR", rootPath)
            put("POLYDROID_NATIVE_DIR", nativeDir)
            val ptrFile = java.io.File(ctx.filesDir, "vulkan_surface_ptr")
            if (ptrFile.exists()) {
                put("POLYDROID_VULKAN_SURFACE_PTR", ptrFile.readText().trim())
            }

            // vulkan driver preference
            val vulkanDriver = SettingsActivity.getVulkanDriver(ctx)
            if (vulkanDriver == SettingsActivity.VULKAN_DRIVER_SYSTEM) {
                put("POLYDROID_FORCE_SYSTEM_DRIVER", "1")
            } else if (vulkanDriver == SettingsActivity.VULKAN_DRIVER_TURNIP) {
                put("POLYDROID_FORCE_TURNIP", "1")
            }

            // screen resolution for surface
            put("POLYDROID_SCREEN_WIDTH", "$screenWidth")
            put("POLYDROID_SCREEN_HEIGHT", "$screenHeight")
            put("SSL_CERT_FILE", "$rootPath/etc/ssl/certs/ca-certificates.crt")
            put("SSL_CERT_DIR", "$rootPath/etc/ssl/certs")
            put("CURL_CA_BUNDLE", "$rootPath/etc/ssl/certs/ca-certificates.crt")
            put("LC_ALL", "C")
            put("LANG", "C")
            put("MONO_THREADS_SUSPEND", "preemptive")
            put("IL2CPP_GC_SUSPEND_SIGNAL", "0")
            put("SDL_VIDEODRIVER", "x11")
            put("DISPLAY", ":0")
            put("XMODIFIERS", "@im=none")
            put("GTK_IM_MODULE", "")
            put("QT_IM_MODULE", "")
            put("SDL_VIDEO_X11_VISUALID", "0x21")
            put("SDL_VIDEO_X11_XRANDR", "0")
            put("SDL_LOG_PRIORITY", "critical")
        }
        val box64Guest = File("$rootPath/usr/bin/box64")
        box64.copyTo(box64Guest, overwrite = true)
        box64Guest.setExecutable(true, false)

        val gameArgStr = if (gameArgs.isNotBlank()) " $gameArgs" else ""

        val launchScript = File("$rootPath/tmp/launch.sh")
        val envExports = env.entries.joinToString("\n") { (k, v) ->
            "export $k=\"$v\""
        }
        launchScript.writeText("""#!/bin/sh
$envExports

cd "$rootPath/polytoria"
exec "$nativeDir/libbox64.so" "$rootPath/polytoria/Polytoria Client.x86_64" -force-vulkan$gameArgStr
""")
        launchScript.setExecutable(true, false)

        val cmd = arrayOf("/system/bin/sh", launchScript.absolutePath)
        val launchEnv = System.getenv().map { (k, v) -> "$k=$v" }.toTypedArray()

        val cmdStr = cmd.joinToString(" ")
        Log.i(TAG, "Launching: $cmdStr")
        Log.i(TAG, "Guest env: ${env.entries.joinToString("\n  ")}")

        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            if (network != null) {
                cm.bindProcessToNetwork(network)
                Log.i(TAG, "Bound process to network: $network")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bind process to network: ${e.message}")
        }

        val proc = Runtime.getRuntime().exec(cmd, launchEnv, root)
        process = proc

        fun logStream(stream: java.io.InputStream, label: String) {
            Thread({
                stream.bufferedReader().use { reader ->
                    reader.lineSequence().forEach { line ->
                        Log.i(TAG, "[$label] $line")
                        onLog(line)
                    }
                }
            }, "box64-$label").start()
        }
        logStream(proc.inputStream, "Box64")
        logStream(proc.errorStream, "Box64")

        Thread({
            val exit = proc.waitFor()
            Log.i(TAG, "Box64 exited with code $exit")
            onLog("Box64 exited with code $exit")
        }, "box64-wait").start()

        return proc
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    private fun getSystemDnsServers(ctx: Context): List<String> {
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            if (network != null) {
                val lp = cm.getLinkProperties(network)
                if (lp != null) {
                    val servers = lp.dnsServers.map { it.hostAddress!! }.filter { it.isNotEmpty() }
                    if (servers.isNotEmpty()) return servers
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get system DNS: ${e.message}")
        }
        // fallback to google dns
        return listOf("8.8.8.8", "8.8.4.4")
    }
}
