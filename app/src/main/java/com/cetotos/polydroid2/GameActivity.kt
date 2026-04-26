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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var imeCapture: ImeCaptureView
    private lateinit var vulkanSurface: SurfaceView
    private lateinit var thermalBanner: TextView
    private var inputHandler: TouchInputHandler? = null
    private var cmdEntryPoint: CmdEntryPoint? = null
    private var lorieShim: LorieMainActivity? = null
    private var box64Started = false
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var vulkanSurfaceReady = false
    @Volatile private var crashDialogShown = false
    @Volatile private var overheatDialogShown = false
    private var lastThermalStatus = 0
    private val thermalBannerHideRunnable = Runnable {
        if (::thermalBanner.isInitialized) thermalBanner.visibility = android.view.View.GONE
    }
    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        runOnUiThread { onThermalStatusChanged(status) }
    }

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
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val fullscreenMode = SettingsActivity.getFullscreen(this)
        actionBar?.hide()
        supportActionBar?.hide()
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        @Suppress("DEPRECATION")
        val cutout = windowManager.defaultDisplay.cutout
        val cutL = if (!fullscreenMode) cutout?.safeInsetLeft ?: 0 else 0
        val cutT = if (!fullscreenMode) cutout?.safeInsetTop ?: 0 else 0
        val cutR = if (!fullscreenMode) cutout?.safeInsetRight ?: 0 else 0
        val cutB = if (!fullscreenMode) cutout?.safeInsetBottom ?: 0 else 0
        val effectiveW = (screenWidth - cutL - cutR).coerceAtLeast(1)
        val effectiveH = (screenHeight - cutT - cutB).coerceAtLeast(1)

        val (customEnabled, resParam1, resParam2) = SettingsActivity.getResolution(this)
        if (customEnabled) {
            renderWidth = resParam1
            renderHeight = resParam2
        } else {
            val targetShortEdge = resParam1
            val shortEdge = minOf(effectiveW, effectiveH)
            val scale = if (shortEdge > targetShortEdge) targetShortEdge.toDouble() / shortEdge else 1.0
            renderWidth = (effectiveW * scale).toInt()
            renderHeight = (effectiveH * scale).toInt()
        }

        startTimeMs = System.currentTimeMillis()

        val execArgs = intent.getStringExtra("exec_args") ?: ""
        val fullArgs = if (execArgs.isNotEmpty()) {
            "$execArgs -screen-fullscreen 1 -screen-width $renderWidth -screen-height $renderHeight" // it seems making it fullscreen wil almost 4x your fps. i dont know how it increases it that much but it works.
        } else {
            ""
        }
        lorieShim = LorieMainActivity(this)
        val frame = FrameLayout(this)
        frame.setBackgroundColor(0xFF000000.toInt())
        if (!fullscreenMode) {
            frame.setPadding(cutL, cutT, cutR, cutB)
        }
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
            cameraSensitivity = SettingsActivity.getCameraSensitivity(this),
            newZoom = SettingsActivity.getNewZoom(this),
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

        if (!SettingsActivity.getShowStats(this)) {
            statsView.visibility = android.view.View.GONE
        }

        thermalBanner = TextView(this).apply {
            setPadding(dp(16), dp(10), dp(16), dp(10))
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC8B0000.toInt())
            setShadowLayer(2f, 1f, 1f, 0xFF000000.toInt())
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
        }
        val bannerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.BOTTOM }
        frame.addView(thermalBanner, bannerParams)

        try {
            pm.addThermalStatusListener(mainExecutor, thermalListener)
            lastThermalStatus = pm.currentThermalStatus
        } catch (e: Exception) {
            Log.w("PolyDroid2", "thermal listener register failed: ${e.message}")
        }

        imeCapture = ImeCaptureView(this)
        imeCapture.sendKey = { sc, kc, down -> nativeSendKeyEvent(sc, kc, down) }
        frame.addView(imeCapture, FrameLayout.LayoutParams(1, 1))

        val density = resources.displayMetrics.density
        val kbSize = (64 * density).toInt()
        val imeLayout = SettingsActivity.getOverlayIme(this)
        val kbPad = (14 * density).toInt()
        val kbButton = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.keyboard_alt_24)
            setColorFilter(0xFFFFFFFF.toInt())
            setBackgroundColor(0x99000000.toInt())
            setPadding(kbPad, kbPad, kbPad, kbPad)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            isClickable = true
            isFocusable = false
            scaleX = imeLayout.scale
            scaleY = imeLayout.scale
            setOnClickListener {
                toggleSoftKeyboard()
            }
        }
        val kbParams = FrameLayout.LayoutParams(kbSize, kbSize)
        frame.addView(kbButton, kbParams)
        frame.post {
            kbButton.x = imeLayout.xFrac * frame.width - kbSize / 2f
            kbButton.y = imeLayout.yFrac * frame.height - kbSize / 2f
        }

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

            AudioBridge.start()
            appendLog("Audio bridge started")

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

            if (overheatDialogShown) {
                appendLog("Skipped box64 launch due to overheating!")
                return@thread
            }

            appendLog("Launching Box64...")
            try {
                Box64Launcher.launch(
                    this, tmpDir, fullArgs, renderWidth, renderHeight,
                    onLog = { line -> appendLog(line) },
                    onExit = { code ->
                        when {
                            overheatDialogShown -> {}
                            code != 0 && !isFinishing -> runOnUiThread { showCrashDialog(code) }
                            code == 0 && !isFinishing -> runOnUiThread { finishAndRemoveTask() }
                        }
                    }
                )
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

    override fun onResume() {
        super.onResume()
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            onThermalStatusChanged(pm.currentThermalStatus)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        statsHandler.removeCallbacks(statsRunnable)
        statsHandler.removeCallbacks(thermalBannerHideRunnable)
        try {
            (getSystemService(POWER_SERVICE) as PowerManager).removeThermalStatusListener(thermalListener)
        } catch (_: Exception) {}
        if (isFinishing) {
            Box64Launcher.stop()
            AudioBridge.stop()
            ClientProxy.stop()
        }
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun toggleSoftKeyboard() {
        imeCapture.switchKeyboardState()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun showCrashDialog(exitCode: Int) {
        if (crashDialogShown || overheatDialogShown) return
        crashDialogShown = true

        val pad = dp(24)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(8), pad, dp(20))
        }

        val msg = TextView(this).apply {
            text = "Box64 exited with code $exitCode"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        content.addView(msg, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        val sendBtn = MaterialButton(this).apply {
            text = "Send logs"
        }
        content.addView(sendBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val websiteBtn = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonTonalStyle
        ).apply {
            text = "Back to website"
        }
        content.addView(websiteBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Polytoria has crashed!")
            .setView(content)
            .setCancelable(false)
            .create()

        sendBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Send logs?")
                .setMessage("This will send your app logs and Unity logs to the developer which helps to fix bugs. No personal info will be included in logs.")
                .setPositiveButton("Send") { _, _ ->
                    sendBtn.isEnabled = false
                    sendBtn.text = "Sending..."
                    SettingsActivity.sendLogsStatic(
                        this,
                        extraInfo = "box64 exit code=$exitCode",
                        onProgress = { s -> runOnUiThread { sendBtn.text = s } },
                        onDone = { _, m ->
                            runOnUiThread {
                                Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
                                sendBtn.isEnabled = true
                                sendBtn.text = "Send logs"
                            }
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        websiteBtn.setOnClickListener {
            dialog.dismiss()
            finishAndRemoveTask()
        }

        dialog.show()
    }

    private fun onThermalStatusChanged(status: Int) {
        if (status >= PowerManager.THERMAL_STATUS_CRITICAL) {
            showOverheatDialog()
        } else if (status == PowerManager.THERMAL_STATUS_SEVERE
                && lastThermalStatus < PowerManager.THERMAL_STATUS_SEVERE) {
            showThermalBanner("Device hot, performance might drop!", 5000L)
        }
        lastThermalStatus = status
    }

    private fun showThermalBanner(msg: String, durationMs: Long) {
        if (!::thermalBanner.isInitialized) return
        thermalBanner.text = msg
        thermalBanner.visibility = android.view.View.VISIBLE
        statsHandler.removeCallbacks(thermalBannerHideRunnable)
        statsHandler.postDelayed(thermalBannerHideRunnable, durationMs)
    }

    private fun showOverheatDialog() {
        if (overheatDialogShown || crashDialogShown) return
        overheatDialogShown = true
        Box64Launcher.stop()

        val pad = dp(24)
        val danger = 0xFFB00020.toInt()
        val dangerDim = 0xFF5A0010.toInt()

        val iconSize = dp(40)
        val icon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.emergency_heat_24)?.apply {
            setBounds(0, 0, iconSize, iconSize)
            setTint(danger)
        }

        val titleView = TextView(this).apply {
            text = "Device overheat!"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
            setTextColor(danger)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setCompoundDrawablesRelative(icon, null, null, null)
            compoundDrawablePadding = dp(14)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, dp(20), pad, dp(8))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(8), pad, dp(20))
        }

        val msg = TextView(this).apply {
            text = "Your device is too hot. Polytoria has exited in order to stop damage to device. Please cool down your device before playing."
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        content.addView(msg, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        val btnBgTints = android.content.res.ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(dangerDim, danger)
        )
        val btnTextTints = android.content.res.ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(0xFFDDDDDD.toInt(), 0xFFFFFFFF.toInt())
        )

        val websiteBtn = MaterialButton(this).apply {
            text = "Back to website (4)"
            isEnabled = false
            backgroundTintList = btnBgTints
            setTextColor(btnTextTints)
        }
        content.addView(websiteBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val dialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(titleView)
            .setView(content)
            .setCancelable(false)
            .create()

        websiteBtn.setOnClickListener {
            dialog.dismiss()
            finishAndRemoveTask()
        }

        statsHandler.post {
            if (!isFinishing && !isDestroyed) {
                try { dialog.show() } catch (e: Exception) {
                    Log.w("PolyDroid2", "Overheat dialog show failed: ${e.message}")
                }
            }
        }

        var seconds = 4
        val ticker = object : Runnable {
            override fun run() {
                seconds--
                if (seconds > 0) {
                    websiteBtn.text = "Back to website ($seconds)"
                    statsHandler.postDelayed(this, 1000)
                } else {
                    websiteBtn.text = "Back to website"
                    websiteBtn.isEnabled = true
                }
            }
        }
        statsHandler.postDelayed(ticker, 1000)
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
        val medianFps = nativeGetMedianFps()
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
        sb.appendLine("Unity: $unityFps FPS | Median: $medianFps FPS")
        sb.appendLine("Frames: $totalFrames")
        sb.appendLine("GPU: $gpuName | Usage: ${if (gpuUsage >= 0) "$gpuUsage%" else "N/A"}")
        if (gpuTemp > 0) sb.appendLine("  GPU Temp: ${gpuTemp}\u00B0C")
        sb.appendLine("CPU: ${if (cpuUsage >= 0) "$cpuUsage%" else "N/A"} | Temp: ${if (cpuTemp > 0) "${cpuTemp}\u00B0C" else "N/A"}")
        sb.appendLine("RAM: ${usedMb}MB / ${totalMb}MB ($ramPct%)")
        sb.appendLine("Res: ${renderWidth}x${renderHeight}")
        if (vulkanInfo.isNotEmpty()) sb.append("Vulkan: $vulkanInfo")

        statsView.text = sb.toString()
    }

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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                try { android.os.Build.SOC_MODEL } catch (_: Exception) { android.os.Build.HARDWARE }
            } else {
                android.os.Build.HARDWARE
            }
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
    private external fun nativeGetMedianFps(): Int
    private external fun nativeGetVulkanInfo(): String
    private external fun nativeSendInputEvent(type: Int, button: Int, x: Int, y: Int)
    private external fun nativeSendKeyEvent(scanCode: Int, keyCode: Int, keyDown: Boolean)
    companion object {
        init {
            System.loadLibrary("native_window_jni")
        }
    }
}
