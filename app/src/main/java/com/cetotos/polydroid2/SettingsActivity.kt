package com.cetotos.polydroid2

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "polydroid_settings"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_CUSTOM_ENABLED = "custom_resolution_enabled"
        const val KEY_CUSTOM_WIDTH = "custom_width"
        const val KEY_CUSTOM_HEIGHT = "custom_height"
        const val KEY_CAMERA_SENSITIVITY = "camera_sensitivity"
        const val KEY_SHOW_STATS = "show_stats"
        const val KEY_FULLSCREEN = "fullscreen"
        const val KEY_VULKAN_DRIVER = "vulkan_driver"
        const val VULKAN_DRIVER_AUTO = "auto"
        const val VULKAN_DRIVER_SYSTEM = "system"
        const val VULKAN_DRIVER_TURNIP = "turnip"
        const val KEY_LAST_LOG_SEND = "last_log_send_time"
        const val LOG_SEND_COOLDOWN = 180 * 1000L
        const val DEFAULT_RESOLUTION = 720
        const val DEFAULT_SENSITIVITY = 3f

        const val KEY_JOYSTICK_X = "overlay_joystick_x"
        const val KEY_JOYSTICK_Y = "overlay_joystick_y"
        const val KEY_JOYSTICK_SCALE = "overlay_joystick_scale"
        const val KEY_JUMP_X = "overlay_jump_x"
        const val KEY_JUMP_Y = "overlay_jump_y"
        const val KEY_JUMP_SCALE = "overlay_jump_scale"
        const val KEY_IME_X = "overlay_ime_x"
        const val KEY_IME_Y = "overlay_ime_y"
        const val KEY_IME_SCALE = "overlay_ime_scale"
        const val KEY_ITEMBAR_X = "overlay_itembar_x"
        const val KEY_ITEMBAR_Y = "overlay_itembar_y"
        const val KEY_ITEMBAR_SCALE = "overlay_itembar_scale"

        const val DEFAULT_JOYSTICK_X = 0.13f
        const val DEFAULT_JOYSTICK_Y = 0.72f
        const val DEFAULT_JUMP_X = 0.91f
        const val DEFAULT_JUMP_Y = 0.80f
        const val DEFAULT_IME_X = 0.36f
        const val DEFAULT_IME_Y = 0.94f
        const val DEFAULT_ITEMBAR_X = 0.5f
        const val DEFAULT_ITEMBAR_Y = 0.08f

        // whoever is reading this, please, dont spam the webhook. Thanks
        private const val WEBHOOK_URL = "https://discord.com/api/webhooks/1492893841915904120/1eGfbBbhAK76Q7yzJs8ilxxjE00nDVY8-cBNY4yPJxROOoMqaMySXQUI9VavagVfwpx5"

        private val PRESETS = listOf(
            480 to "480p", // 480p is basically the minimum until thing start breaking
            720 to "720p (Default)",
            900 to "900p",
            1080 to "1080p",
            1440 to "1440p",
        )

        data class OverlayButton(val xFrac: Float, val yFrac: Float, val scale: Float)

        fun getCameraSensitivity(ctx: Context): Float {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_SENSITIVITY)
        }

        fun getShowStats(ctx: Context): Boolean {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(KEY_SHOW_STATS, true)
        }

        fun getFullscreen(ctx: Context): Boolean {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(KEY_FULLSCREEN, true)
        }

        fun getVulkanDriver(ctx: Context): String {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_VULKAN_DRIVER, VULKAN_DRIVER_SYSTEM) ?: VULKAN_DRIVER_SYSTEM
        }

        fun getResolution(ctx: Context): Triple<Boolean, Int, Int> {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val customEnabled = prefs.getBoolean(KEY_CUSTOM_ENABLED, false)
            return if (customEnabled) {
                val w = prefs.getInt(KEY_CUSTOM_WIDTH, 1280)
                val h = prefs.getInt(KEY_CUSTOM_HEIGHT, 720)
                Triple(true, w, h)
            } else {
                val shortEdge = prefs.getInt(KEY_RESOLUTION, DEFAULT_RESOLUTION)
                Triple(false, shortEdge, 0)
            }
        }

        fun getOverlayJoystick(ctx: Context): OverlayButton {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return OverlayButton(
                p.getFloat(KEY_JOYSTICK_X, DEFAULT_JOYSTICK_X),
                p.getFloat(KEY_JOYSTICK_Y, DEFAULT_JOYSTICK_Y),
                p.getFloat(KEY_JOYSTICK_SCALE, 1f),
            )
        }

        fun getOverlayJump(ctx: Context): OverlayButton {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return OverlayButton(
                p.getFloat(KEY_JUMP_X, DEFAULT_JUMP_X),
                p.getFloat(KEY_JUMP_Y, DEFAULT_JUMP_Y),
                p.getFloat(KEY_JUMP_SCALE, 1f),
            )
        }

        fun getOverlayIme(ctx: Context): OverlayButton {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return OverlayButton(
                p.getFloat(KEY_IME_X, DEFAULT_IME_X),
                p.getFloat(KEY_IME_Y, DEFAULT_IME_Y),
                p.getFloat(KEY_IME_SCALE, 1f),
            )
        }

        fun getOverlayItemBar(ctx: Context): OverlayButton {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return OverlayButton(
                p.getFloat(KEY_ITEMBAR_X, DEFAULT_ITEMBAR_X),
                p.getFloat(KEY_ITEMBAR_Y, DEFAULT_ITEMBAR_Y),
                p.getFloat(KEY_ITEMBAR_SCALE, 1f),
            )
        }
    }

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val colorPrimary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)
        val colorOnSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)

        val toolbar = MaterialToolbar(this).apply {
            title = "Settings"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            setBackgroundColor(colorSurface)
        }

        val tabLayout = TabLayout(this).apply {
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL
            setSelectedTabIndicatorColor(colorPrimary)
            setSelectedTabIndicatorHeight(dp(3))
            setTabTextColors(colorOnSurfaceVariant, colorPrimary)
            tabRippleColor = android.content.res.ColorStateList.valueOf(
                MaterialColors.compositeARGBWithAlpha(colorPrimary, 40)
            )
            setBackgroundColor(colorSurface)
            addTab(newTab().setText("Client"))
            addTab(newTab().setText("Controls"))
            addTab(newTab().setText("Other"))
        }

        val tabDivider = MaterialDivider(this)

        val container = FrameLayout(this)

        val pages = listOf(
            buildGraphicsTab(),
            buildControlsTab(),
            buildOtherTab(),
        )
        for ((idx, page) in pages.withIndex()) {
            container.addView(
                page,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
            page.visibility = if (idx == 0) View.VISIBLE else View.GONE
        }

        fun show(i: Int) {
            for (j in pages.indices) {
                pages[j].visibility = if (j == i) View.VISIBLE else View.GONE
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { show(tab.position) }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            addView(tabLayout)
            addView(tabDivider)
            addView(
                container,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }
    }

    private fun section(title: String): Pair<View, LinearLayout> {
        val card = MaterialCardView(
            this, null, com.google.android.material.R.attr.materialCardViewFilledStyle
        ).apply {
            radius = dp(20).toFloat()
            strokeWidth = 0
            cardElevation = 0f
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        val titleView = TextView(this).apply {
            text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        }
        inner.addView(titleView, layoutParams().apply { bottomMargin = dp(14) })
        card.addView(inner)
        return card to inner
    }

    private fun cardParams(first: Boolean = false): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { if (!first) topMargin = dp(12) }
    }

    private fun buildGraphicsTab(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        val (displayCard, display) = section("Display")

        val dropdown = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_dropdown_item_1line,
                PRESETS.map { it.second }
            ))
        }
        val dropdownLayout = TextInputLayout(
            this,
            null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Resolution"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(dropdown)
        }
        display.addView(dropdownLayout, layoutParams())

        val currentPreset = prefs.getInt(KEY_RESOLUTION, DEFAULT_RESOLUTION)
        val presetIndex = PRESETS.indexOfFirst { it.first == currentPreset }
        if (presetIndex >= 0) {
            dropdown.setText(PRESETS[presetIndex].second, false)
        }

        dropdown.setOnItemClickListener { _, _, position, _ ->
            val shortEdge = PRESETS[position].first
            prefs.edit().putInt(KEY_RESOLUTION, shortEdge).apply()
        }

        val customSwitch = MaterialSwitch(this).apply {
            text = "Custom resolution"
        }
        display.addView(customSwitch, layoutParams().apply { topMargin = dp(16) })

        val customContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val widthEdit = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        val widthLayout = TextInputLayout(this).apply {
            hint = "Width"
            addView(widthEdit)
        }
        customContainer.addView(widthLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })

        val heightEdit = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        val heightLayout = TextInputLayout(this).apply {
            hint = "Height"
            addView(heightEdit)
        }
        customContainer.addView(heightLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        display.addView(customContainer, layoutParams().apply { topMargin = dp(8) })

        val customEnabled = prefs.getBoolean(KEY_CUSTOM_ENABLED, false)
        customSwitch.isChecked = customEnabled
        dropdownLayout.isEnabled = !customEnabled
        customContainer.visibility = if (customEnabled) View.VISIBLE else View.GONE

        widthEdit.setText(prefs.getInt(KEY_CUSTOM_WIDTH, 1280).toString())
        heightEdit.setText(prefs.getInt(KEY_CUSTOM_HEIGHT, 720).toString())

        customSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_CUSTOM_ENABLED, checked).apply()
            dropdownLayout.isEnabled = !checked
            customContainer.visibility = if (checked) View.VISIBLE else View.GONE
        }

        val saveCustom = {
            val w = widthEdit.text.toString().toIntOrNull() ?: 1280
            val h = heightEdit.text.toString().toIntOrNull() ?: 720
            prefs.edit()
                .putInt(KEY_CUSTOM_WIDTH, w)
                .putInt(KEY_CUSTOM_HEIGHT, h)
                .apply()
        }

        widthEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveCustom() }
        heightEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveCustom() }
        heightEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) saveCustom()
            false
        }

        val fullscreenSwitch = MaterialSwitch(this).apply {
            text = "Fullscreen mode"
            isChecked = prefs.getBoolean(KEY_FULLSCREEN, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KEY_FULLSCREEN, checked).apply()
            }
        }
        display.addView(fullscreenSwitch, layoutParams().apply { topMargin = dp(16) })

        content.addView(displayCard, cardParams(first = true))

        val (graphicsCard, graphics) = section("Graphics")

        val statsSwitch = MaterialSwitch(this).apply {
            text = "Performance stats overlay"
            isChecked = prefs.getBoolean(KEY_SHOW_STATS, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KEY_SHOW_STATS, checked).apply()
            }
        }
        graphics.addView(statsSwitch, layoutParams())

        val driverOptions = listOf(
            VULKAN_DRIVER_AUTO to "Auto",
            VULKAN_DRIVER_SYSTEM to "System driver (Adreno/Mali)", // system driver has better performance, well on my phone atleast, but its kinda broken
            VULKAN_DRIVER_TURNIP to "Turnip (Adreno only)",
        )
        val driverDropdown = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_dropdown_item_1line,
                driverOptions.map { it.second }
            ))
        }
        val driverLayout = TextInputLayout(
            this,
            null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Vulkan driver"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(driverDropdown)
        }
        graphics.addView(driverLayout, layoutParams().apply { topMargin = dp(16) })

        val currentDriver = prefs.getString(KEY_VULKAN_DRIVER, VULKAN_DRIVER_SYSTEM) ?: VULKAN_DRIVER_SYSTEM
        val driverIndex = driverOptions.indexOfFirst { it.first == currentDriver }
        if (driverIndex >= 0) {
            driverDropdown.setText(driverOptions[driverIndex].second, false)
        }
        driverDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putString(KEY_VULKAN_DRIVER, driverOptions[position].first).apply()
        }

        content.addView(graphicsCard, cardParams())

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun buildControlsTab(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        val (cameraCard, camera) = section("Camera")

        val sensitivityLabel = TextView(this).apply {
            text = "Camera sensitivity: ${"%.1f".format(prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_SENSITIVITY))}x"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        camera.addView(sensitivityLabel, layoutParams())

        val sensitivitySlider = Slider(this).apply {
            valueFrom = 0.5f
            valueTo = 10f
            stepSize = 0.5f
            value = prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_SENSITIVITY)
            addOnChangeListener { _, newVal, _ ->
                prefs.edit().putFloat(KEY_CAMERA_SENSITIVITY, newVal).apply()
                sensitivityLabel.text = "Camera sensitivity: ${"%.1f".format(newVal)}x"
            }
        }
        camera.addView(sensitivitySlider, layoutParams())

        content.addView(cameraCard, cardParams(first = true))

        val (overlayCard, overlay) = section("Overlay editor")

        val editorHint = TextView(this).apply {
            text = "Tap a button/joystick, then drag to move or rescale."
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
        }
        overlay.addView(editorHint, layoutParams().apply { bottomMargin = dp(12) })

        val editor = OverlayEditorView(this)
        overlay.addView(editor, layoutParams())

        val scaleLabel = TextView(this).apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        overlay.addView(scaleLabel, layoutParams().apply { topMargin = dp(16) })

        val scaleSlider = Slider(this).apply {
            valueFrom = 0.5f
            valueTo = 2f
            stepSize = 0.05f
        }
        overlay.addView(scaleSlider, layoutParams())

        fun renderScaleLabel(which: OverlayEditorView.Which, scale: Float) {
            val name = when (which) {
                OverlayEditorView.Which.JOYSTICK -> "Joystick"
                OverlayEditorView.Which.JUMP -> "Jump"
                OverlayEditorView.Which.IME -> "IME"
                OverlayEditorView.Which.ITEMBAR -> "Item bar"
            }
            scaleLabel.text = "$name size: ${"%.2f".format(scale)}x"
        }

        renderScaleLabel(editor.selected, editor.selectedScale())
        scaleSlider.value = editor.selectedScale()

        editor.onSelectionChanged = { which, scale ->
            renderScaleLabel(which, scale)
            scaleSlider.value = scale.coerceIn(scaleSlider.valueFrom, scaleSlider.valueTo)
        }

        scaleSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                editor.setSelectedScale(value)
                renderScaleLabel(editor.selected, value)
            }
        }

        val resetButton = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Reset overlay layout"
            setOnClickListener {
                editor.resetAll()
                scaleSlider.value = editor.selectedScale()
                renderScaleLabel(editor.selected, editor.selectedScale())
            }
        }
        overlay.addView(resetButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        content.addView(overlayCard, cardParams())

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun buildOtherTab(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        val (diagCard, diag) = section("Diagnostics")

        val diagHint = TextView(this).apply {
            text = "Send your recent app and Unity logs to the developer to help fix bugs. No personal info is included."
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
        }
        diag.addView(diagHint, layoutParams().apply { bottomMargin = dp(12) })

        val sendLogsButton = MaterialButton(this).apply {
            text = "Send app logs"
            setOnClickListener {
                android.app.AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Send logs?")
                    .setMessage("This will send your app logs and Unity logs to the developer which helps to fix bugs. No personal info will be included in logs.")
                    .setPositiveButton("Send") { _, _ -> sendLogs(this) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        diag.addView(sendLogsButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        content.addView(diagCard, cardParams(first = true))

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun sendLogs(button: MaterialButton) {
        val lastSend = prefs.getLong(KEY_LAST_LOG_SEND, 0)
        val now = System.currentTimeMillis()
        if (now - lastSend < LOG_SEND_COOLDOWN) {
            val remaining = ((LOG_SEND_COOLDOWN - (now - lastSend)) / 1000).toInt()
            Toast.makeText(this, "Please wait ${remaining}s before sending again", Toast.LENGTH_SHORT).show()
            return
        }

        button.isEnabled = false
        button.text = "Sending..."

        Thread {
            try {
                runOnUiThread { button.text = "Reading logs..." }
                // collect Player.log which is where stderr (so the vulkan shim etc.) go to
                val playerSb = StringBuilder()
                playerSb.appendLine("------ Device info -----")
                playerSb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                playerSb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                playerSb.appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
                playerSb.appendLine("Board: ${Build.BOARD}")
                playerSb.appendLine("Hardware: ${Build.HARDWARE}")
                playerSb.appendLine("Vulkan driver: ${getVulkanDriver(this)}")
                playerSb.appendLine("Resolution: ${getResolution(this)}")
                playerSb.appendLine()

                val playerLog = File(
                    filesDir,
                    "rootfs/home/user/.config/unity3d/Polytoria/Polytoria Client/Player.log"
                )
                if (playerLog.exists()) {
                    val lines = playerLog.readLines()
                    val start = (lines.size - 5000).coerceAtLeast(0)
                    for (i in start until lines.size) {
                        val line = lines[i]
                        // filter Box64's signal spam
                        if (line.contains("sigaction handler for sig ")) continue
                        if (line.contains("Signal ") && line.contains("si_addr=")) continue
                        if (line.contains("Warning, calling Signal ") && line.contains("SIG_IGN")) continue
                        playerSb.appendLine(line)
                    }
                } else {
                    playerSb.appendLine("Player.log not found")
                }
                val playerBytes = playerSb.toString().toByteArray(Charsets.UTF_8)

                // collect logcat
                val logcatSb = StringBuilder()
                try {
                    val proc = Runtime.getRuntime().exec(arrayOf(
                        "logcat", "-d", "-v", "time",
                        "PolyDroid2:*", "PolyDroid2-Vulkan:*", "PolyDroid2-window:*",
                        "Box64:*", "BOX64:*",
                        "*:S"
                    ))
                    val allLines = proc.inputStream.bufferedReader().readLines()
                    proc.waitFor()

                    val start = (allLines.size - 1000).coerceAtLeast(0)
                    for (i in start until allLines.size) {
                        logcatSb.appendLine(allLines[i])
                    }

                    if (allLines.isEmpty()) {
                        logcatSb.appendLine("No matching logcat entries found")
                    }
                } catch (e: Exception) {
                    logcatSb.appendLine("Failed to read logcat: ${e.message}")
                }
                val logcatBytes = logcatSb.toString().toByteArray(Charsets.UTF_8)

                runOnUiThread { button.text = "Sending..." }

                val boundary = "----PolyDroid${System.currentTimeMillis()}"
                val message = "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | ${Build.HARDWARE}"

                val body = java.io.ByteArrayOutputStream()
                fun writePart(s: String) { body.write(s.toByteArray(Charsets.UTF_8)) }

                writePart("--$boundary\r\n")
                writePart("Content-Disposition: form-data; name=\"content\"\r\n\r\n")
                writePart("$message\r\n")

                writePart("--$boundary\r\n")
                writePart("Content-Disposition: form-data; name=\"files[0]\"; filename=\"player.log\"\r\n")
                writePart("Content-Type: text/plain\r\n\r\n")
                body.write(playerBytes)
                writePart("\r\n")

                writePart("--$boundary\r\n")
                writePart("Content-Disposition: form-data; name=\"files[1]\"; filename=\"logcat.log\"\r\n")
                writePart("Content-Type: text/plain\r\n\r\n")
                body.write(logcatBytes)
                writePart("\r\n")

                writePart("--$boundary--\r\n")

                val bodyBytes = body.toByteArray()

                val url = URL(WEBHOOK_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.setRequestProperty("Content-Length", bodyBytes.size.toString())
                conn.setFixedLengthStreamingMode(bodyBytes.size)
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                conn.outputStream.use { it.write(bodyBytes) }
                val responseCode = conn.responseCode
                conn.disconnect()

                runOnUiThread {
                    if (responseCode in 200..299) {
                        prefs.edit().putLong(KEY_LAST_LOG_SEND, System.currentTimeMillis()).apply()
                        Toast.makeText(this, "Logs sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to send! got HTTP error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                    button.isEnabled = true
                    button.text = "Send app logs"
                }
            } catch (e: Exception) {
                Log.e("PolyDroid2", "failed to send logs: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Failed with: ${e.message}", Toast.LENGTH_SHORT).show()
                    button.isEnabled = true
                    button.text = "Send app logs"
                }
            }
        }.start()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
