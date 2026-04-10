package com.cetotos.polydroid2

import android.app.ActivityManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.system.Os
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.termux.x11.CmdEntryPoint
import com.termux.x11.LorieView
import com.termux.x11.MainActivity as LorieMainActivity
import com.termux.x11.input.InputEventSender
import com.termux.x11.input.TouchInputHandler
import java.io.File
import kotlin.concurrent.thread

class GameActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var statsView: TextView
    private lateinit var lorieView: LorieView
    private lateinit var vulkanSurface: SurfaceView
    private var inputHandler: TouchInputHandler? = null
    private var cmdEntryPoint: CmdEntryPoint? = null
    private var lorieShim: LorieMainActivity? = null
    private var box64Started = false
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var vulkanSurfaceReady = false

    private val statsHandler = Handler(Looper.getMainLooper())
    private var statsUpdateCount = 0
    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L
    private var renderWidth = 0
    private var renderHeight = 0
    private var startTimeMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PolyDroid2::Box64").apply {
            acquire()
        }

        // fullscreen
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        actionBar?.hide()
        supportActionBar?.hide()
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // hardcoded to 720p for now
        // TODO: make this adjustable by user
        val shortEdge = minOf(screenWidth, screenHeight)
        val scale = if (shortEdge > 720) 720.0 / shortEdge else 1.0
        renderWidth = (screenWidth * scale).toInt()
        renderHeight = (screenHeight * scale).toInt()

        startTimeMs = System.currentTimeMillis()

        val execArgs = intent.getStringExtra("exec_args") ?: ""
        val fullArgs = if (execArgs.isNotEmpty()) {
            "$execArgs -screen-fullscreen 1 -screen-width $renderWidth -screen-height $renderHeight" // it seems making it fullscreen wil almost 4x your fps. i dont know how it increases it that much but it works.
        } else {
            ""
        }
        lorieShim = LorieMainActivity(this)
        val frame = FrameLayout(this)
        lorieView = LorieView(this)
        lorieShim!!.setLorieView(lorieView)
        frame.addView(lorieView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        val injector = InputEventSender(lorieView)
        inputHandler = TouchInputHandler(lorieShim, injector)
        lorieView.setCallback { surfaceWidth, surfaceHeight, clientWidth, clientHeight ->
            inputHandler?.handleHostSizeChanged(surfaceWidth, surfaceHeight)
            inputHandler?.handleClientSizeChanged(clientWidth, clientHeight)
        }

        // Vulkan SurfaceView
        vulkanSurface = SurfaceView(this)

        vulkanSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val surface = holder.surface
                val windowPtr = nativeGetWindow(surface)
                val ptrFile = java.io.File(filesDir, "vulkan_surface_ptr")
                ptrFile.writeText(String.format("%x", windowPtr))
                appendLog("wrote vulkan_surface_ptr: 0x${String.format("%x", windowPtr)}")

                // start compositor
                val compositorOk = nativeStartFrameCompositor(surface)
                if (compositorOk) {
                    appendLog("Frame compositor started on Vulkan surface")
                } else {
                    appendLog("Frame compositor failed to start!")
                }
                vulkanSurfaceReady = true
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                vulkanSurfaceReady = false
            }
        })
        frame.addView(vulkanSurface, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // touch controls
        val touchOverlay = TouchControlOverlay(
            context = this,
            renderWidth = renderWidth,
            renderHeight = renderHeight,
            sendInput = { type, button, x, y -> nativeSendInputEvent(type, button, x, y) },
            sendKey = { scanCode, keyCode, down -> nativeSendKeyEvent(scanCode, keyCode, down) },
        )
        frame.addView(touchOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        scrollView = ScrollView(this)
        logView = TextView(this).apply {
            setPadding(0, 0, 0, 0)
            textSize = 1f
        }
        scrollView.addView(logView)
        scrollView.visibility = android.view.View.GONE
        frame.addView(scrollView)
        statsView = TextView(this).apply {
            setPadding(12, 6, 12, 6)
            textSize = 9f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x99000000.toInt())
            setShadowLayer(2f, 1f, 1f, 0xFF000000.toInt())
            isClickable = false
            isFocusable = false
        }
        val statsParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        frame.addView(statsView, statsParams)

        setContentView(frame)


        statsHandler.post(statsRunnable)
        if (savedInstanceState != null) {
            return
        }

        appendLog("Polytoria Client launching...")
        if (fullArgs.isNotEmpty()) {
            appendLog("Launch args recieved: $fullArgs")
        }

        box64Started = true

        thread {
            Looper.prepare()

            // extract rootfs if not extracted
            if (!RootFs.isInstalled(this)) {
                appendLog("Extracting rootfs...")
                RootFs.install(this) { msg -> appendLog(msg) }
            } else {
                appendLog("Rootfs already installed.")
            }

            val rootPath = RootFs.rootDir(this).absolutePath
            val tmpDir = "$rootPath/tmp"
            java.io.File(tmpDir).mkdirs()
            java.io.File("$tmpDir/.X11-unix").apply { mkdirs(); setReadable(true, false); setExecutable(true, false); setWritable(true, false) }
            Os.setenv("TMPDIR", tmpDir, true)
            Os.setenv("XKB_CONFIG_ROOT", "$rootPath/usr/share/X11/xkb", true)

            appendLog("TMPDIR=$tmpDir")
            appendLog("XKB_CONFIG_ROOT=$rootPath/usr/share/X11/xkb")

            // start Lorie
            appendLog("Starting X server...")
            val started = CmdEntryPoint.start(arrayOf(":0"))
            if (!started) {
                appendLog("Failed to start X server!")
                return@thread
            }
            appendLog("X server started successfully!")
            val bridgeOk = nativeStartX11Bridge("$tmpDir/.X11-unix/X0", rootPath)
            if (bridgeOk) {
                appendLog("Socket bridge started")
            } else {
                appendLog("Socket bridge failed to start!")
            }

            val entry = CmdEntryPoint()
            cmdEntryPoint = entry
            entry.spawnListeningThread()
            appendLog("X server listening for GUI connection")

            var connected = false
            for (i in 1..167) { // AYO SUS😂😂 67
                if (LorieView.requestConnection()) {
                    connected = true
                    break
                }
                Thread.sleep(67) // enough 67.
            }

            if (connected) {
                appendLog("Lorie connected to X server")
                val pfd = entry.getXConnection()
                if (pfd != null) {
                    val fd = pfd.detachFd()
                    LorieView.connect(fd)
                    appendLog("Lorie event channel connected (fd=$fd)")
                } else {
                    appendLog("getXConnection() returned null!")
                }
                runOnUiThread {
                    LorieView.sendWindowChange(renderWidth, renderHeight, 60, null)
                }
            } else {
                appendLog("Failed to connect LorieView to X server after 5s!")
            }

            appendLog("Launching Box64...")
            try {
                Box64Launcher.launch(this, tmpDir, fullArgs, renderWidth, renderHeight) { line ->
                    appendLog(line)
                }
            } catch (e: Exception) {
                appendLog("Error! ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private var dispatchTouchLogCount = 0
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        val result = super.dispatchTouchEvent(ev)
        return result
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val scanCode = event.scanCode
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    nativeSendKeyEvent(scanCode, keyCode, true)
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                nativeSendKeyEvent(scanCode, keyCode, false)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        statsHandler.removeCallbacks(statsRunnable)
        if (isFinishing) {
            Box64Launcher.stop()
        }
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun appendLog(msg: String) {
        Log.i("PolyDroid2", msg)
    }

    // -------------------------- overlay ---------------

    private val statsRunnable = object : Runnable {
        override fun run() {
            updateStatsOverlay()
            statsUpdateCount++
            statsHandler.postDelayed(this, 1000)
        }
    }

    private fun updateStatsOverlay() {
        val unityFps = nativeGetUnityFps()
        val compFps = nativeGetCompositorFps()
        val unityFrameTime = if (unityFps > 0) "%.1f".format(1000.0 / unityFps) else "---"
        val totalFrames = nativeGetTotalFrames()
        val cpuUsage = getCpuUsage()
        val gpuUsage = getGpuUsage()
        val cpuTemp = getCpuTemp()
        val gpuTemp = getGpuTemp()
        val mem = getMemoryInfo()
        val gpuName = getGpuName()
        val vulkanInfo = nativeGetVulkanInfo()

        val usedMb = mem.first / (1024 * 1024)
        val totalMb = mem.second / (1024 * 1024)
        val ramPct = if (mem.second > 0) (mem.first * 100 / mem.second).toInt() else 0

        val sb = StringBuilder()
        sb.appendLine("Unity: $unityFps FPS ($unityFrameTime ms) | Comp: $compFps FPS")
        sb.appendLine("Frames: $totalFrames")
        sb.appendLine("GPU: $gpuName | Usage: ${if (gpuUsage >= 0) "$gpuUsage%" else "N/A"}")
        if (gpuTemp > 0) sb.appendLine("  GPU Temp: ${gpuTemp}\u00B0C")
        sb.appendLine("CPU: ${if (cpuUsage >= 0) "$cpuUsage%" else "N/A"} | Temp: ${if (cpuTemp > 0) "${cpuTemp}\u00B0C" else "N/A"}")
        sb.appendLine("RAM: ${usedMb}MB / ${totalMb}MB ($ramPct%)")
        sb.appendLine("Res: ${renderWidth}x${renderHeight}")
        if (vulkanInfo.isNotEmpty()) sb.append("Vulkan: $vulkanInfo")

        statsView.text = sb.toString()


    private fun getCpuUsage(): Int {
        return try {
            val line = File("/proc/stat").readLines().first { it.startsWith("cpu ") }
            val parts = line.split("\\s+".toRegex()).drop(1).map { it.toLong() }
            val idle = parts[3] + parts.getOrElse(4) { 0L }
            val total = parts.sum()
            val diffIdle = idle - lastCpuIdle
            val diffTotal = total - lastCpuTotal
            lastCpuIdle = idle
            lastCpuTotal = total
            if (diffTotal == 0L) 0 else ((diffTotal - diffIdle) * 100 / diffTotal).toInt()
        } catch (_: Exception) { -1 }
    }

    private fun getGpuUsage(): Int {
        return try {
            val text = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage").readText().trim()
            text.replace("%", "").trim().toInt()
        } catch (_: Exception) {
            try {
                val parts = File("/sys/class/kgsl/kgsl-3d0/gpubusy").readText().trim()
                    .split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val busy = parts[0].toLong()
                    val total = parts[1].toLong()
                    if (total > 0) (busy * 100 / total).toInt() else -1
                } else -1
            } catch (_: Exception) { -1 }
        }
    }

    private fun getGpuFreqMHz(): Int {
        return try {
            (File("/sys/class/kgsl/kgsl-3d0/gpuclk").readText().trim().toLong() / 1_000_000).toInt()
        } catch (_: Exception) {
            try {
                (File("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq").readText().trim().toLong() / 1_000_000).toInt()
            } catch (_: Exception) { -1 }
        }
    }

    private fun getCpuTemp(): Int {
        for (i in 0..30) {
            try {
                val type = File("/sys/class/thermal/thermal_zone$i/type").readText().trim().lowercase()
                if (type.contains("cpu") || type == "tsens_tz_sensor1" || type == "soc_thermal") {
                    val raw = File("/sys/class/thermal/thermal_zone$i/temp").readText().trim().toInt()
                    return if (raw > 1000) raw / 1000 else raw
                }
            } catch (_: Exception) { continue }
        }
        return -1
    }

    private fun getGpuTemp(): Int {
        for (i in 0..30) {
            try {
                val type = File("/sys/class/thermal/thermal_zone$i/type").readText().trim().lowercase()
                if (type.contains("gpu") || type.contains("gpuss")) {
                    val raw = File("/sys/class/thermal/thermal_zone$i/temp").readText().trim().toInt()
                    return if (raw > 1000) raw / 1000 else raw
                }
            } catch (_: Exception) { continue }
        }
        return -1
    }

    private fun getMemoryInfo(): Pair<Long, Long> {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return Pair(mi.totalMem - mi.availMem, mi.totalMem)
    }

    private fun getBatteryPercent(): Int {
        return try {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (_: Exception) { -1 }
    }

    private fun getGpuName(): String {
        return try {
            File("/sys/class/kgsl/kgsl-3d0/gpu_model").readText().trim()
        } catch (_: Exception) {
            try { android.os.Build.SOC_MODEL } catch (_: Exception) { android.os.Build.HARDWARE }
        }
    }

    private fun getPerCoreCpuFreqs(): String {
        val freqs = mutableListOf<String>()
        for (i in 0..7) {
            try {
                val freq = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                    .readText().trim().toLong() / 1000
                freqs.add("${freq}MHz")
            } catch (_: Exception) { break }
        }
        return freqs.joinToString(", ")
    }

    private fun formatUptime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
    }

    private external fun nativeGetWindow(surface: Surface): Long
    private external fun nativeStartX11Bridge(realPath: String, rootfsPath: String): Boolean
    private external fun nativeStartFrameCompositor(surface: Surface): Boolean
    private external fun nativeGetCompositorFps(): Int
    private external fun nativeGetUnityFps(): Int
    private external fun nativeGetTotalFrames(): Int
    private external fun nativeGetVulkanInfo(): String
    private external fun nativeSendInputEvent(type: Int, button: Int, x: Int, y: Int)
    private external fun nativeSendKeyEvent(scanCode: Int, keyCode: Int, keyDown: Boolean)
    companion object {
        init {
            System.loadLibrary("native_window_jni")
        }
    }
}
