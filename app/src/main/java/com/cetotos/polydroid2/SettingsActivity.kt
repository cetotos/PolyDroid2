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

    data class OverlayButton(val xFrac: Float, val yFrac: Float, val scale: Float)

    data class CustomKey(
        val id: String,
        val label: String,
        val scanCode: Int,
        val xFrac: Float,
        val yFrac: Float,
        val scale: Float,
        val toggle: Boolean,
    )

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
        const val KEY_MAX_FPS = "max_fps"

        const val KEY_POLY_QUALITY_LEVEL = "poly_quality_level"
        const val KEY_POLY_POST_PROCESSING = "poly_post_processing"
        const val KEY_POLY_PIXEL_LIGHT_COUNT = "poly_pixel_light_count"
        const val KEY_POLY_SHADOW_DISTANCE = "poly_shadow_distance"
        const val KEY_POLY_SHADOW_RESOLUTION = "poly_shadow_resolution"
        const val KEY_POLY_TEXTURE_QUALITY = "poly_texture_quality"
        const val KEY_POLY_ANTI_ALIASING = "poly_anti_aliasing"
        const val KEY_POLY_ANISOTROPIC = "poly_anisotropic"
        const val KEY_POLY_FULLSCREEN = "poly_fullscreen"
        const val KEY_POLY_MASTER_VOLUME = "poly_master_volume"
        const val KEY_POLY_PRESET = "poly_preset"
        const val DEFAULT_POLY_PRESET = "Low"
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
        const val KEY_SPRINT_X = "overlay_sprint_x"
        const val KEY_SPRINT_Y = "overlay_sprint_y"
        const val KEY_SPRINT_SCALE = "overlay_sprint_scale"
        const val KEY_SPRINT_TOGGLE = "overlay_sprint_toggle"
        const val KEY_CUSTOM_KEYS = "overlay_custom_keys"

        const val DEFAULT_JOYSTICK_X = 0.13f
        const val DEFAULT_JOYSTICK_Y = 0.72f
        const val DEFAULT_JUMP_X = 0.91f
        const val DEFAULT_JUMP_Y = 0.80f
        const val DEFAULT_IME_X = 0.36f
        const val DEFAULT_IME_Y = 0.94f
        const val DEFAULT_ITEMBAR_X = 0.5f
        const val DEFAULT_ITEMBAR_Y = 0.08f
        const val DEFAULT_SPRINT_X = 0.82f
        const val DEFAULT_SPRINT_Y = 0.65f

        val KEY_OPTIONS: List<Pair<String, Int>> = listOf(
            "A" to 30, "B" to 48, "C" to 46, "D" to 32, "E" to 18, "F" to 33,
            "G" to 34, "H" to 35, "I" to 23, "J" to 36, "K" to 37, "L" to 38,
            "M" to 50, "N" to 49, "O" to 24, "P" to 25, "Q" to 16, "R" to 19,
            "S" to 31, "T" to 20, "U" to 22, "V" to 47, "W" to 17, "X" to 45,
            "Y" to 21, "Z" to 44,
            "1" to 2, "2" to 3, "3" to 4, "4" to 5, "5" to 6,
            "6" to 7, "7" to 8, "8" to 9, "9" to 10, "0" to 11,
            "Tab" to 15, "Enter" to 28, "Esc" to 1,
            "Shift" to 42, "Ctrl" to 29, "Alt" to 56,
            "F1" to 59, "F2" to 60, "F3" to 61, "F4" to 62,
        )

        // if you decoded this, please dont spam it. Thanks
        private const val REPORT_KEY = "very-secure-key-ok-dont-spam-it-bots-thanks"
        private const val REPORT_BLOB = "HhEGCV5JSkwRGxZOBBcdAwwEQEsOHh0CBBUDBUIGH15NXkBKG0ZeWFhfQEBXS04fQFNaTF1IHxIdH3cJKU5QWysmeBwRUV5rKzNnCCVAPWkAAysUDh4lVEUuQBpSMAIlBhhZHxZyLV5+CFovA1lHHgwCXwc3YDsaPw=="

        private fun reportEndpoint(): String {
            val raw = android.util.Base64.decode(REPORT_BLOB, android.util.Base64.DEFAULT)
            val k = REPORT_KEY.toByteArray()
            val out = ByteArray(raw.size)
            for (i in raw.indices) out[i] = (raw[i].toInt() xor k[i % k.size].toInt()).toByte()
            return String(out, Charsets.UTF_8)
        }

        private val PRESETS = listOf(
            480 to "480p", // 480p is basically the minimum until thing start breaking
            720 to "720p (Default)",
            900 to "900p",
            1080 to "1080p",
            1440 to "1440p",
        )

        fun getSprintToggle(ctx: Context): Boolean {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return p.getBoolean(KEY_SPRINT_TOGGLE, false)
        }

        fun setSprintToggle(ctx: Context, v: Boolean) {
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_SPRINT_TOGGLE, v).apply()
        }

        fun getCustomKeys(ctx: Context): List<CustomKey> {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val s = p.getString(KEY_CUSTOM_KEYS, null) ?: return emptyList()
            return try {
                val arr = org.json.JSONArray(s)
                val out = ArrayList<CustomKey>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(CustomKey(
                        id = o.getString("id"),
                        label = o.getString("label"),
                        scanCode = o.getInt("scanCode"),
                        xFrac = o.getDouble("x").toFloat(),
                        yFrac = o.getDouble("y").toFloat(),
                        scale = o.getDouble("scale").toFloat(),
                        toggle = o.getBoolean("toggle"),
                    ))
                }
                out
            } catch (_: Exception) { emptyList() }
        }

        fun saveCustomKeys(ctx: Context, list: List<CustomKey>) {
            val arr = org.json.JSONArray()
            for (k in list) {
                val o = org.json.JSONObject()
                o.put("id", k.id)
                o.put("label", k.label)
                o.put("scanCode", k.scanCode)
                o.put("x", k.xFrac.toDouble())
                o.put("y", k.yFrac.toDouble())
                o.put("scale", k.scale.toDouble())
                o.put("toggle", k.toggle)
                arr.put(o)
            }
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_CUSTOM_KEYS, arr.toString()).apply()
        }

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

        fun getMaxFps(ctx: Context): Int {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getInt(KEY_MAX_FPS, 0)
        }

        fun sendLogsStatic(
            ctx: Context,
            extraInfo: String = "",
            onProgress: (String) -> Unit,
            onDone: (success: Boolean, msg: String) -> Unit,
        ) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val lastSend = prefs.getLong(KEY_LAST_LOG_SEND, 0)
            val now = System.currentTimeMillis()
            if (now - lastSend < LOG_SEND_COOLDOWN) {
                val remaining = ((LOG_SEND_COOLDOWN - (now - lastSend)) / 1000).toInt()
                onDone(false, "Please wait ${remaining}s before sending again")
                return
            }

            Thread {
                try {
                    onProgress("Reading logs...")
                    val playerSb = StringBuilder()
                    playerSb.appendLine("------ Device info -----")
                    playerSb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    playerSb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    playerSb.appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
                    playerSb.appendLine("Board: ${Build.BOARD}")
                    playerSb.appendLine("Hardware: ${Build.HARDWARE}")
                    playerSb.appendLine("Vulkan driver: ${getVulkanDriver(ctx)}")
                    playerSb.appendLine("Resolution: ${getResolution(ctx)}")
                    if (extraInfo.isNotEmpty()) {
                        playerSb.appendLine()
                        playerSb.appendLine(extraInfo)
                    }
                    playerSb.appendLine()

                    val playerLog = File(
                        ctx.filesDir,
                        "rootfs/home/user/.config/unity3d/Polytoria/Polytoria Client/Player.log"
                    )
                    if (playerLog.exists()) {
                        val lines = playerLog.readLines()
                        val start = (lines.size - 5000).coerceAtLeast(0)
                        for (i in start until lines.size) {
                            val line = lines[i]
                            if (line.contains("sigaction handler for sig ")) continue
                            if (line.contains("Signal ") && line.contains("si_addr=")) continue
                            if (line.contains("Warning, calling Signal ") && line.contains("SIG_IGN")) continue
                            playerSb.appendLine(line)
                        }
                    } else {
                        playerSb.appendLine("Player.log not found")
                    }
                    val playerBytes = playerSb.toString().toByteArray(Charsets.UTF_8)

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
                        for (i in start until allLines.size) logcatSb.appendLine(allLines[i])
                        if (allLines.isEmpty()) logcatSb.appendLine("No matching logcat entries found")
                    } catch (e: Exception) {
                        logcatSb.appendLine("Failed to read logcat: ${e.message}")
                    }
                    val logcatBytes = logcatSb.toString().toByteArray(Charsets.UTF_8)

                    onProgress("Sending...")

                    val boundary = "----PolyDroid${System.currentTimeMillis()}"
                    val message = "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | ${Build.HARDWARE}" +
                        if (extraInfo.isNotEmpty()) " | $extraInfo" else ""

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

                    val url = URL(reportEndpoint())
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

                    if (responseCode in 200..299) {
                        prefs.edit().putLong(KEY_LAST_LOG_SEND, System.currentTimeMillis()).apply()
                        onDone(true, "Logs sent!")
                    } else {
                        onDone(false, "Failed to send! HTTP $responseCode")
                    }
                } catch (e: Exception) {
                    Log.e("PolyDroid2", "failed to send logs: ${e.message}", e)
                    onDone(false, "Failed with: ${e.message}")
                }
            }.start()
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

        fun getOverlaySprint(ctx: Context): OverlayButton {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return OverlayButton(
                p.getFloat(KEY_SPRINT_X, DEFAULT_SPRINT_X),
                p.getFloat(KEY_SPRINT_Y, DEFAULT_SPRINT_Y),
                p.getFloat(KEY_SPRINT_SCALE, 1f),
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

        val polytoriaButton = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Polytoria graphics settings"
            setOnClickListener {
                startActivity(android.content.Intent(this@SettingsActivity, PolytoriaSettingsActivity::class.java))
            }
        }
        graphics.addView(polytoriaButton, layoutParams().apply { topMargin = dp(16) })

        content.addView(graphicsCard, cardParams())

        val (perfCard, perf) = section("Performance")

        val fpsOptions = listOf(
            0 to "Unlimited",
            30 to "30 FPS",
            45 to "45 FPS",
            60 to "60 FPS",
            90 to "90 FPS",
            120 to "120 FPS",
            144 to "144 FPS",
        )
        val fpsDropdown = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_dropdown_item_1line,
                fpsOptions.map { it.second }
            ))
        }
        val fpsLayout = TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Max FPS"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(fpsDropdown)
        }

        fpsLayout.isEnabled = false // disabled for now because it doesn't work
        fpsDropdown.isEnabled = false // ^^^^

        perf.addView(fpsLayout, layoutParams())


        val currentFps = prefs.getInt(KEY_MAX_FPS, 0)
        val fpsIdx = fpsOptions.indexOfFirst { it.first == currentFps }.let { if (it < 0) 0 else it }
        fpsDropdown.setText(fpsOptions[fpsIdx].second, false)
        fpsDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putInt(KEY_MAX_FPS, fpsOptions[position].first).apply()
        }

        val perfHint = TextView(this).apply {
            text = "Currently broken"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
        }
        perf.addView(perfHint, layoutParams().apply { topMargin = dp(12) })

        content.addView(perfCard, cardParams())

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

        val toggleSwitch = MaterialSwitch(this).apply {
            text = "Toggle mode"
            visibility = View.GONE
        }
        overlay.addView(toggleSwitch, layoutParams().apply { topMargin = dp(8) })

        val customLabel = TextView(this).apply {
            text = "Custom key"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
            visibility = View.GONE
        }
        overlay.addView(customLabel, layoutParams().apply { topMargin = dp(8) })

        val customRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        val keyDropdown = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_dropdown_item_1line,
                KEY_OPTIONS.map { it.first }
            ))
        }
        val keyDropdownLayout = TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Key"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(keyDropdown)
        }
        customRow.addView(keyDropdownLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val removeBtn = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Remove"
        }
        customRow.addView(removeBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8) })
        overlay.addView(customRow, layoutParams().apply { topMargin = dp(4) })

        val addBtn = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Add custom key"
        }
        overlay.addView(addBtn, layoutParams().apply { topMargin = dp(8) })

        fun renderScaleLabel(which: OverlayEditorView.Which, scale: Float) {
            val name = when (which) {
                OverlayEditorView.Which.JOYSTICK -> "Joystick"
                OverlayEditorView.Which.JUMP -> "Jump"
                OverlayEditorView.Which.IME -> "IME"
                OverlayEditorView.Which.ITEMBAR -> "Item bar"
                OverlayEditorView.Which.SPRINT -> "Sprint"
                is OverlayEditorView.Which.CUSTOM -> "Custom key"
            }
            scaleLabel.text = "$name size: ${"%.2f".format(scale)}x"
        }

        fun refreshSelectionControls() {
            val t = editor.selectedToggle()
            if (t != null) {
                toggleSwitch.visibility = View.VISIBLE
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = t
                toggleSwitch.setOnCheckedChangeListener { _, checked ->
                    editor.setSelectedToggle(checked)
                }
            } else {
                toggleSwitch.visibility = View.GONE
            }
            val cid = editor.selectedCustomId()
            if (cid != null) {
                customLabel.visibility = View.VISIBLE
                customRow.visibility = View.VISIBLE
                val ck = editor.customKeys().firstOrNull { it.id == cid }
                if (ck != null) {
                    keyDropdown.setText(ck.label, false)
                }
            } else {
                customLabel.visibility = View.GONE
                customRow.visibility = View.GONE
            }
        }

        renderScaleLabel(editor.selected, editor.selectedScale())
        scaleSlider.value = editor.selectedScale()
        refreshSelectionControls()

        editor.onSelectionChanged = { which, scale ->
            renderScaleLabel(which, scale)
            scaleSlider.value = scale.coerceIn(scaleSlider.valueFrom, scaleSlider.valueTo)
            refreshSelectionControls()
        }

        scaleSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                editor.setSelectedScale(value)
                renderScaleLabel(editor.selected, value)
            }
        }

        keyDropdown.setOnItemClickListener { _, _, position, _ ->
            val cid = editor.selectedCustomId() ?: return@setOnItemClickListener
            val (label, scan) = KEY_OPTIONS[position]
            val existing = editor.customKeys().firstOrNull { it.id == cid } ?: return@setOnItemClickListener
            editor.updateCustomKey(cid, label, scan, existing.toggle)
        }

        removeBtn.setOnClickListener {
            val cid = editor.selectedCustomId() ?: return@setOnClickListener
            editor.removeCustomKey(cid)
            refreshSelectionControls()
            scaleSlider.value = editor.selectedScale()
            renderScaleLabel(editor.selected, editor.selectedScale())
        }

        addBtn.setOnClickListener {
            val (label, scan) = KEY_OPTIONS[0]
            val id = "ck_${System.currentTimeMillis()}"
            editor.addCustomKey(SettingsActivity.CustomKey(
                id = id, label = label, scanCode = scan,
                xFrac = 0.5f, yFrac = 0.5f, scale = 1f, toggle = false,
            ))
            scaleSlider.value = editor.selectedScale()
            renderScaleLabel(editor.selected, editor.selectedScale())
            refreshSelectionControls()
        }

        val resetButton = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Reset overlay layout"
            setOnClickListener {
                editor.resetAll()
                scaleSlider.value = editor.selectedScale()
                renderScaleLabel(editor.selected, editor.selectedScale())
                refreshSelectionControls()
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
        button.isEnabled = false
        button.text = "Sending..."
        sendLogsStatic(
            this,
            onProgress = { msg -> runOnUiThread { button.text = msg } },
            onDone = { _, msg ->
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    button.isEnabled = true
                    button.text = "Send app logs"
                }
            }
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
