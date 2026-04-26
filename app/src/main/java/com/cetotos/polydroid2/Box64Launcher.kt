package com.cetotos.polydroid2

import android.content.Context
import android.util.Log
import java.io.File

object Box64Launcher {
    private const val TAG = "PolyDroid2"

    private var process: Process? = null

    fun launch(ctx: Context, tmpDir: String, gameArgs: String = "", screenWidth: Int = 1280, screenHeight: Int = 720, onLog: (String) -> Unit, onExit: ((Int) -> Unit)? = null): Process {
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
        RootFs.deleteSfx(File(root, "polytoria"))
        try { PolytoriaPrefs.applyTo(ctx, root) } catch (e: Exception) { Log.w(TAG, "polytoria prefs apply failed: ${e.message}") }
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
                val addrs = java.net.InetAddress.getAllByName(hostname)
                val v4 = addrs.firstOrNull { it is java.net.Inet4Address }
                if (v4 != null) {
                    hostsBuilder.append("${v4.hostAddress} $hostname\n")
                    Log.i(TAG, "Resolved $hostname -> ${v4.hostAddress}")
                } else {
                    Log.w(TAG, "No IPv4 for $hostname (got: ${addrs.joinToString { it.hostAddress ?: "?" }})")
                }
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
        // append proxy ca so the game trusts ClientProxt
        try {
            val proxyCa = ctx.assets.open("proxy_ca.crt").use { it.readBytes().toString(Charsets.UTF_8) }
            val current = caBundle.readText()
            if (!current.contains("PolyDroid2 Local Proxy CA")) {
                caBundle.appendText("\n" + proxyCa)
                Log.i(TAG, "appended proxy ca to ca-certificates.crt")
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to append proxy ca: ${e.message}")
        }

        // start ClientProxy to fix connection
        val proxyPort = ClientProxy.start(ctx)
        val polytoriaIp: String = try {
            java.net.InetAddress.getAllByName("api.polytoria.com")
                .firstOrNull { it is java.net.Inet4Address }?.hostAddress ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "could not resolve api.polytoria.com for redirect: ${e.message}")
            ""
        }

        val arm64NativeDir = File("$rootPath/usr/lib/arm64-native")
        val libDir = File("$rootPath/usr/lib/aarch64-linux-gnu")
        val x86LibDir = File("$rootPath/usr/lib/x86_64-linux-gnu")

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
                "libdbus-1.so.3:" +
                "librt.so.1")

            var ldPreload = ""
            ldPreload = "$rootPath/usr/lib/x86_64-linux-gnu/libpath_remap.so:$rootPath/polytoria/libunity_crash_fix.so:$rootPath/usr/lib/x86_64-linux-gnu/libsysconf_fix.so:$rootPath/usr/lib/x86_64-linux-gnu/libdns_resolver.so:$rootPath/usr/lib/x86_64-linux-gnu/libaudio_trace.so:$rootPath/usr/lib/x86_64-linux-gnu/libconnect_redirect.so:$ldPreload"
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
            val maxFps = SettingsActivity.getMaxFps(ctx)
            if (maxFps > 0) put("POLYDROID_MAX_FPS", "$maxFps")
            // connect redirect: send api.polytoria.com traffic to our local proxy
            if (polytoriaIp.isNotBlank() && proxyPort > 0) {
                put("POLYDROID_REDIRECT_FROM_IP", polytoriaIp)
                put("POLYDROID_REDIRECT_TO_IP", "127.0.0.1")
                put("POLYDROID_REDIRECT_TO_PORT", "$proxyPort")
            }
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
        val gameArgStr = if (gameArgs.isNotBlank()) " $gameArgs" else ""

        val bigMask = getBigCoresMask()
        val sysTaskset = listOf("/system/bin/taskset", "/system/xbin/taskset")
            .firstOrNull { File(it).canExecute() }
        val execPrefix = if (sysTaskset != null && bigMask != null) {
            Log.i(TAG, "Pinning Box64 via $sysTaskset 0x$bigMask")
            "$sysTaskset $bigMask "
        } else {
            Log.i(TAG, "Not pinning Box64 (taskset=$sysTaskset mask=$bigMask)")
            ""
        }

        val launchScript = File("$rootPath/tmp/launch.sh")
        val envExports = env.entries.joinToString("\n") { (k, v) ->
            "export $k=\"$v\""
        }

        val box64Cmd = "\"$nativeDir/libbox64.so\" \"$rootPath/polytoria/Polytoria Client.x86_64\" -force-vulkan$gameArgStr"
        val execLine = if (execPrefix.isNotBlank())
            "${execPrefix}/system/bin/true >/dev/null 2>&1 && exec $execPrefix$box64Cmd\nexec $box64Cmd"
        else
            "exec $box64Cmd"
        launchScript.writeText("""#!/bin/sh
$envExports
cd "$rootPath/polytoria"
$execLine
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
                try {
                    stream.bufferedReader().use { reader ->
                        reader.lineSequence().forEach { line ->
                            Log.i(TAG, "[$label] $line")
                            onLog(line)
                        }
                    }
                } catch (_: java.io.IOException) {
                    // box64 exited and its pipe got closed mid-read. expected.
                }
            }, "box64-$label").start()
        }
        logStream(proc.inputStream, "Box64")
        logStream(proc.errorStream, "Box64")

        Thread({
            val exit = proc.waitFor()
            Log.i(TAG, "Box64 exited with code $exit")
            onLog("Box64 exited with code $exit")
            onExit?.invoke(exit)
        }, "box64-wait").start()

        return proc
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    private fun getBigCoresMask(): String? {
        return try {
            val freqs = (0 until 64).mapNotNull { i ->
                try {
                    val f = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                        .readText().trim().toLong()
                    if (f > 0) Pair(i, f) else null
                } catch (_: Exception) { null }
            }
            if (freqs.isEmpty()) return null
            val tiers = freqs.map { it.second }.distinct().sorted()
            if (tiers.size == 1) return null
            val skipFreq: Long? = if (tiers.size >= 3) tiers.first() else null
            var mask = 0L
            for ((i, f) in freqs) if (f != skipFreq) mask = mask or (1L shl i)
            if (mask == 0L) return null
            val allowed = readCpusAllowed()
            if (allowed != 0L && (mask and allowed) == 0L) {
                Log.i(TAG, "big cores ${mask.toString(2)} disjoint from cpuset ${allowed.toString(2)}, not pinning")
                return null
            }
            Log.i(TAG, "cpu tiers: $tiers | skipping ${skipFreq ?: "none"}")
            mask.toString(16)
        } catch (_: Exception) { null }
    }

    private fun readCpusAllowed(): Long {
        return try {
            val line = File("/proc/self/status").readLines()
                .firstOrNull { it.startsWith("Cpus_allowed:") } ?: return 0L
            val hex = line.removePrefix("Cpus_allowed:").trim().replace(",", "")
            if (hex.isEmpty()) 0L else java.math.BigInteger(hex, 16).toLong()
        } catch (_: Exception) { 0L }
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
