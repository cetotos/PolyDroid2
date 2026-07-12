package com.cetotos.polydroid2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.color.MaterialColors
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
        val color: Int = DEFAULT_KEY_COLOR,
        val shape: String = SHAPE_CIRCLE,
        val opacity: Float = 1f,
        val textScale: Float = 1f,
    )

    companion object {
        const val PREFS_NAME = "polydroid_settings"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_CUSTOM_ENABLED = "custom_resolution_enabled"
        const val KEY_CUSTOM_WIDTH = "custom_width"
        const val KEY_CUSTOM_HEIGHT = "custom_height"
        const val KEY_CAMERA_SENSITIVITY = "camera_sensitivity"
        const val KEY_SHOW_STATS = "show_stats"
        const val KEY_STATS_POSITION = "stats_position"
        const val KEY_STATS_SIZE = "stats_size"
        const val KEY_STATS_OPACITY = "stats_opacity"
        const val KEY_STATS_ITEMS = "stats_items"
        const val KEY_STATS_ORDER = "stats_order"
        const val KEY_STATS_RAM_UNIT = "stats_ram_unit"
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
        const val KEY_SAFE_MODE = "safe_mode"
        const val KEY_PIP = "pip_enabled"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_OVERHEAT_PROTECTION = "overheat_protection"

        fun isSafeMode(ctx: Context): Boolean =
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_SAFE_MODE, false)

        fun isPipEnabled(ctx: Context): Boolean =
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_PIP, true)

        fun isHapticEnabled(ctx: Context): Boolean =
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_HAPTIC, true)

        fun isOverheatProtectionEnabled(ctx: Context): Boolean =
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_OVERHEAT_PROTECTION, true)
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
        const val KEY_CUSTOM_KEYS_V2 = "overlay_custom_keys_v2"

        const val SHAPE_CIRCLE = "circle"
        const val SHAPE_SQUARE = "square"
        const val DEFAULT_KEY_COLOR = 0xFFFFFFFF.toInt()

        val KEY_COLOR_OPTIONS: List<Pair<String, Int>> = listOf(
            "White" to 0xFFFFFFFF.toInt(),
            "Red" to 0xFFF44336.toInt(),
            "Orange" to 0xFFFF9800.toInt(),
            "Yellow" to 0xFFFFEB3B.toInt(),
            "Green" to 0xFF4CAF50.toInt(),
            "Cyan" to 0xFF00BCD4.toInt(),
            "Blue" to 0xFF2196F3.toInt(),
            "Purple" to 0xFF9C27B0.toInt(),
            "Pink" to 0xFFE91E63.toInt(),
        )
        val KEY_SHAPE_OPTIONS: List<Pair<String, String>> = listOf(
            SHAPE_CIRCLE to "Circle",
            SHAPE_SQUARE to "Square",
        )

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

        fun getCustomKeys(ctx: Context, v2: Boolean = false): List<CustomKey> {
            val p = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val s = p.getString(if (v2) KEY_CUSTOM_KEYS_V2 else KEY_CUSTOM_KEYS, null) ?: return emptyList()
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
                        color = if (o.has("color")) o.getInt("color") else DEFAULT_KEY_COLOR,
                        shape = o.optString("shape", SHAPE_CIRCLE),
                        opacity = o.optDouble("opacity", 1.0).toFloat(),
                        textScale = o.optDouble("textScale", 1.0).toFloat(),
                    ))
                }
                out
            } catch (_: Exception) { emptyList() }
        }

        fun saveCustomKeys(ctx: Context, list: List<CustomKey>, v2: Boolean = false) {
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
                o.put("color", k.color)
                o.put("shape", k.shape)
                o.put("opacity", k.opacity.toDouble())
                o.put("textScale", k.textScale.toDouble())
                arr.put(o)
            }
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(if (v2) KEY_CUSTOM_KEYS_V2 else KEY_CUSTOM_KEYS, arr.toString()).apply()
        }

        fun getCameraSensitivity(ctx: Context): Float {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_SENSITIVITY)
        }

        fun getShowStats(ctx: Context): Boolean {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(KEY_SHOW_STATS, true)
        }

        fun getStatsPosition(ctx: Context): String {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_STATS_POSITION, StatsOverlayView.POS_TL) ?: StatsOverlayView.POS_TL
        }

        fun getStatsSizeScale(ctx: Context): Float {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return when (prefs.getString(KEY_STATS_SIZE, "medium")) {
                "small" -> 0.85f
                "large" -> 1.3f
                else -> 1f
            }
        }

        fun getStatsOpacity(ctx: Context): Int {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getInt(KEY_STATS_OPACITY, 60)
        }

        fun getStatsItems(ctx: Context): Set<StatsOverlayView.StatItem> {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val stored = prefs.getString(KEY_STATS_ITEMS, null)
                ?: return StatsOverlayView.StatItem.entries.toSet()
            val keys = stored.split(",").filter { it.isNotEmpty() }.toSet()
            return StatsOverlayView.StatItem.entries.filter { it.key in keys }.toSet()
        }

        fun setStatsItem(ctx: Context, item: StatsOverlayView.StatItem, enabled: Boolean) {
            val current = getStatsItems(ctx).toMutableSet()
            if (enabled) current.add(item) else current.remove(item)
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_STATS_ITEMS, current.joinToString(",") { it.key }).apply()
        }

        fun getStatsOrder(ctx: Context): List<StatsOverlayView.StatItem> {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val stored = prefs.getString(KEY_STATS_ORDER, null)
                ?: return StatsOverlayView.StatItem.entries.toList()
            val byKey = StatsOverlayView.StatItem.entries.associateBy { it.key }
            val ordered = stored.split(",").mapNotNull { byKey[it] }
            val rest = StatsOverlayView.StatItem.entries.filter { it !in ordered }
            return ordered + rest
        }

        fun saveStatsOrder(ctx: Context, list: List<StatsOverlayView.StatItem>) {
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_STATS_ORDER, list.joinToString(",") { it.key }).apply()
        }

        fun getStatsEnabledOrdered(ctx: Context): List<StatsOverlayView.StatItem> {
            val enabled = getStatsItems(ctx)
            return getStatsOrder(ctx).filter { it in enabled }
        }

        fun getStatsRamInGb(ctx: Context): Boolean {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_STATS_RAM_UNIT, "mb") == "gb"
        }

        fun setStatsRamInGb(ctx: Context, gb: Boolean) {
            ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_STATS_RAM_UNIT, if (gb) "gb" else "mb").apply()
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
            if (prefs.contains(KEY_MAX_FPS)) return prefs.getInt(KEY_MAX_FPS, 0)
            return defaultMaxFps(ctx)
        }
        // set default max fps to screen refresh rate instead of Unlimited
        private fun defaultMaxFps(ctx: Context): Int {
            val rate = try {
                if (android.os.Build.VERSION.SDK_INT >= 30) ctx.display?.refreshRate ?: 60f
                else {
                    @Suppress("DEPRECATION")
                    (ctx.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                        .defaultDisplay.refreshRate
                }
            } catch (_: Exception) { 60f }
            val candidates = intArrayOf(30, 45, 60, 90, 120, 144)
            return candidates.minBy { kotlin.math.abs(it - rate) }
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

    private fun tabSidePadding(): Int {
        val widthDp = resources.configuration.screenWidthDp
        val maxContentDp = 640
        return if (widthDp > maxContentDp) dp((widthDp - maxContentDp) / 2) else dp(16)
    }

    private fun buildGraphicsTab(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(tabSidePadding(), dp(16), tabSidePadding(), dp(24))
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
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Resolution"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(dropdown)
        }
        display.addView(dropdownLayout, layoutParams())

        val currentPreset = prefs.getInt(KEY_RESOLUTION, DEFAULT_RESOLUTION)
        val presetIndex = PRESETS.indexOfFirst { it.first == currentPreset }
        if (presetIndex >= 0) dropdown.setText(PRESETS[presetIndex].second, false)
        dropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putInt(KEY_RESOLUTION, PRESETS[position].first).apply()
        }

        val customSwitch = MaterialSwitch(this).apply { text = "Custom resolution" }
        display.addView(customSwitch, layoutParams().apply { topMargin = dp(16) })

        val customContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

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
            prefs.edit().putInt(KEY_CUSTOM_WIDTH, w).putInt(KEY_CUSTOM_HEIGHT, h).apply()
        }
        widthEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveCustom() }
        heightEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveCustom() }
        heightEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) saveCustom()
            false
        }

        val (fullscreenRow, _) = switchRow("Fullscreen mode", null, prefs.getBoolean(KEY_FULLSCREEN, true)) { checked ->
            prefs.edit().putBoolean(KEY_FULLSCREEN, checked).apply()
        }
        display.addView(fullscreenRow, layoutParams().apply { topMargin = dp(16) })

        val (pipRow, _) = switchRow(
            "Picture-in-Picture",
            "Runs the game in a separate window so you can use other apps while the client stays running.",
            prefs.getBoolean(KEY_PIP, true)
        ) { checked -> prefs.edit().putBoolean(KEY_PIP, checked).apply() }
        display.addView(pipRow, layoutParams().apply { topMargin = dp(16) })

        addStatsOverlayControls(display)

        content.addView(displayCard, cardParams(first = true))

        val (graphicsCard, graphics) = section("Graphics")

        val driverOptions = listOf(
            VULKAN_DRIVER_AUTO to "Auto",
            VULKAN_DRIVER_SYSTEM to "System driver (Universal, faster)",
            VULKAN_DRIVER_TURNIP to "Turnip (Adreno only, slower)",
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
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Vulkan driver"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(driverDropdown)
        }
        graphics.addView(driverLayout, layoutParams().apply { topMargin = dp(16) })

        val currentDriver = prefs.getString(KEY_VULKAN_DRIVER, VULKAN_DRIVER_SYSTEM) ?: VULKAN_DRIVER_SYSTEM
        val driverIndex = driverOptions.indexOfFirst { it.first == currentDriver }
        if (driverIndex >= 0) driverDropdown.setText(driverOptions[driverIndex].second, false)
        driverDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putString(KEY_VULKAN_DRIVER, driverOptions[position].first).apply()
        }

        content.addView(graphicsCard, cardParams())

        val (perfCard, perf) = section("Performance")

        val fpsOptions = listOf(
            0 to "Unlimited", 30 to "30 FPS", 45 to "45 FPS", 60 to "60 FPS",
            90 to "90 FPS", 120 to "120 FPS", 144 to "144 FPS",
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
        perf.addView(fpsLayout, layoutParams())

        val currentFps = getMaxFps(this)
        val fpsIdx = fpsOptions.indexOfFirst { it.first == currentFps }.let { if (it < 0) 0 else it }
        fpsDropdown.setText(fpsOptions[fpsIdx].second, false)
        fpsDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putInt(KEY_MAX_FPS, fpsOptions[position].first).apply()
        }

        val perfHint = TextView(this).apply {
            text = "It is recommened to limit your FPS, as it makes the game more stable and reduces heat."
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
        }
        perf.addView(perfHint, layoutParams().apply { topMargin = dp(12) })

        val (overheatRow, _) = switchRow(
            "Overheat protection",
            "Exits the client when your device gets too hot. Leave this on unless it is causing issues.",
            prefs.getBoolean(KEY_OVERHEAT_PROTECTION, true)
        ) { checked -> prefs.edit().putBoolean(KEY_OVERHEAT_PROTECTION, checked).apply() }
        perf.addView(overheatRow, layoutParams().apply { topMargin = dp(20) })

        content.addView(perfCard, cardParams())

        val (storageCard, storage) = section("Clients")
        val clientOrder = listOf(ClientDownloader.Channel.BETA, ClientDownloader.Channel.STABLE)
        for ((idx, channel) in clientOrder.withIndex()) {
            if (idx > 0) {
                storage.addView(MaterialDivider(this), layoutParams().apply {
                    topMargin = dp(6); bottomMargin = dp(6)
                })
            }
            storage.addView(buildClientRow(channel))
        }
        content.addView(storageCard, cardParams())

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun buildClientRow(channel: ClientDownloader.Channel): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Polytoria ${channel.label}"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        }
        val status = TextView(this).apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
        }
        textCol.addView(title)
        textCol.addView(status)
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
        })

        val settingsBtn = iconButton(R.drawable.ic_settings, MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0))
        settingsBtn.contentDescription = "${channel.label} client settings"
        settingsBtn.setOnClickListener {
            if (channel == ClientDownloader.Channel.BETA) Polytoria2GraphicsDialog(this).show()
            else PolytoriaGraphicsDialog(this).show()
        }
        row.addView(settingsBtn)

        val deleteBtn = iconButton(R.drawable.ic_delete, MaterialColors.getColor(this, androidx.appcompat.R.attr.colorError, 0))
        deleteBtn.contentDescription = "Delete ${channel.label} client"
        row.addView(deleteBtn)

        fun refresh() {
            val v = ClientDownloader.installedVersion(this, channel)
            status.text = if (v != null) "Installed, $v" else "Not installed"
            deleteBtn.isEnabled = v != null
        }
        refresh()

        deleteBtn.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete ${channel.label} client?")
                .setMessage("Deletes the ${channel.label} client. It will download the next time you join a ${channel.label} game.")
                .setPositiveButton("Delete") { _, _ ->
                    deleteBtn.isEnabled = false
                    status.text = "Deleting…"
                    Thread {
                        ClientDownloader.delete(this, channel)
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            Toast.makeText(this, "${channel.label} client deleted", Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }.start()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        return row
    }

    private fun iconButton(iconRes: Int, tint: Int): MaterialButton {
        val disabled = MaterialColors.compositeARGBWithAlpha(
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0), 97
        )
        val csl = android.content.res.ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(disabled, tint)
        )
        return MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            icon = androidx.core.content.ContextCompat.getDrawable(this@SettingsActivity, iconRes)
            iconTint = csl
        }
    }

    private class ActionRow(val view: LinearLayout, val subtitle: TextView, val icon: MaterialButton)

    private fun textColumn(title: String, subtitle: String? = null): Pair<LinearLayout, TextView?> {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply {
            text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        })
        var sub: TextView? = null
        if (!subtitle.isNullOrEmpty()) {
            sub = TextView(this).apply {
                text = subtitle
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
            }
            col.addView(sub, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) })
        }
        return col to sub
    }

    private fun switchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit): Pair<LinearLayout, MaterialSwitch> {
        val (col, _) = textColumn(title, subtitle)
        val sw = MaterialSwitch(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, c -> onChange(c) }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            })
            addView(sw, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(12) })
        }
        return row to sw
    }

    private fun actionRow(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit): ActionRow {
        val (col, sub) = textColumn(title, subtitle)
        val icon = iconButton(iconRes, MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)).apply {
            contentDescription = title
            setOnClickListener { onClick() }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            })
            addView(icon)
        }
        return ActionRow(row, sub!!, icon)
    }

    private fun addStatsOverlayControls(display: LinearLayout) {
        val enabled = prefs.getBoolean(KEY_SHOW_STATS, true)

        val gear = iconButton(R.drawable.ic_settings, MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)).apply {
            contentDescription = "Customize overlay"
            isEnabled = enabled
            setOnClickListener { showStatsOverlayDialog() }
        }
        val sw = MaterialSwitch(this).apply { isChecked = enabled }
        val (col, _) = textColumn("Performance stats overlay")

        sw.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_SHOW_STATS, checked).apply()
            gear.isEnabled = checked
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            })
            addView(gear)
            addView(sw, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(4) })
        }
        display.addView(row, layoutParams().apply { topMargin = dp(16) })
    }

    private fun buildControlsTab(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(tabSidePadding(), dp(16), tabSidePadding(), dp(24))
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

        val (hapticRow, _) = switchRow("Haptic feedback", null, prefs.getBoolean(KEY_HAPTIC, true)) { checked ->
            prefs.edit().putBoolean(KEY_HAPTIC, checked).apply()
        }
        camera.addView(hapticRow, layoutParams().apply { topMargin = dp(16) })

        content.addView(cameraCard, cardParams(first = true))

        val (overlayCard, overlay) = section("Overlay")
        val overlayEditorBtn = MaterialButton(this).apply {
            text = "Open overlay editor"
            setOnClickListener {
                startActivity(android.content.Intent(this@SettingsActivity, OverlayEditorActivity::class.java))
            }
        }
        overlay.addView(overlayEditorBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })
        content.addView(overlayCard, cardParams())

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun buildOtherTab(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(tabSidePadding(), dp(16), tabSidePadding(), dp(24))
        }

        val (updCard, upd) = section("Updates")
        lateinit var updateRow: ActionRow
        updateRow = actionRow(
            "Check for updates",
            "Current version: ${currentVersionName()}",
            R.drawable.ic_refresh
        ) { checkForUpdates(updateRow) }
        upd.addView(updateRow.view)
        content.addView(updCard, cardParams(first = true))

        val (diagCard, diag) = section("Debug")
        val (safeRow, _) = switchRow(
            "Safe mode",
            "Disables some box64 optimizations and may be slower. Use only if you hit frequent crashes.",
            prefs.getBoolean(KEY_SAFE_MODE, false)
        ) { checked -> prefs.edit().putBoolean(KEY_SAFE_MODE, checked).apply() }
        diag.addView(safeRow)

        lateinit var logsRow: ActionRow
        logsRow = actionRow(
            "Send app logs",
            "Sends your recent app and client logs to the developer to help fix bugs. No personal info is included.",
            R.drawable.ic_send
        ) { sendLogs(logsRow) }
        diag.addView(logsRow.view, layoutParams().apply { topMargin = dp(8) })
        content.addView(diagCard, cardParams())

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun checkForUpdates(row: ActionRow) {
        row.icon.isClickable = false
        val spin = android.animation.ObjectAnimator.ofFloat(row.icon, android.view.View.ROTATION, 0f, 360f).apply {
            duration = 750
            repeatCount = android.animation.ValueAnimator.INFINITE
            interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
            start()
        }
        val startedAt = android.os.SystemClock.uptimeMillis()
        val current = currentVersionName()
        UpdateCheck.checkAsync(current) { result ->
            val remaining = 1500 - (android.os.SystemClock.uptimeMillis() - startedAt)
            row.icon.postDelayed({
                spin.cancel()
                row.icon.rotation = 0f
                row.icon.isClickable = true
                if (isFinishing || isDestroyed) return@postDelayed
                when {
                    result == null -> Toast.makeText(this, "Update check failed", Toast.LENGTH_SHORT).show()
                    result.outdated -> showUpdateDialog(current, result.latestTag, result.htmlUrl)
                    else -> Toast.makeText(this, "You're up to date! ($current)", Toast.LENGTH_SHORT).show()
                }
            }, remaining.coerceAtLeast(0))
        }
    }

    private fun sendLogs(row: ActionRow) {
        val original = row.subtitle.text
        var launched = false
        LogReporter.promptAndSend(
            this,
            onProgress = { msg ->
                runOnUiThread {
                    row.icon.isEnabled = false
                    row.subtitle.text = msg
                    if (!launched) {
                        launched = true
                        planeFlyOut(row.icon)
                    }
                }
            },
            onDone = { _, msg ->
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    row.icon.isEnabled = true
                    row.subtitle.text = original
                    if (launched) planeFlyIn(row.icon)
                }
            }
        )
    }

    private fun planeFlyOut(icon: View) {
        val loc = IntArray(2)
        icon.getLocationOnScreen(loc)
        val flyDist = (resources.displayMetrics.widthPixels - loc[0] + icon.width).toFloat()
        icon.animate().cancel()
        icon.animate()
            .translationX(dp(-14).toFloat())
            .translationY(dp(3).toFloat())
            .rotation(-8f)
            .scaleX(0.82f)
            .setDuration(260)
            .setInterpolator(android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f))
            .withEndAction {
                icon.animate()
                    .translationX(flyDist)
                    .translationY(dp(-12).toFloat())
                    .rotation(0f)
                    .scaleX(1.45f)
                    .scaleY(0.9f)
                    .setDuration(450)
                    .setInterpolator(android.view.animation.PathInterpolator(0.8f, 0f, 1f, 0.6f))
                    .withEndAction {
                        icon.alpha = 0f
                        icon.translationX = 0f
                        icon.translationY = 0f
                        icon.rotation = 0f
                        icon.scaleX = 1f
                        icon.scaleY = 1f
                    }
                    .start()
            }
            .start()
    }

    private fun planeFlyIn(icon: View) {
        icon.animate().cancel()
        icon.alpha = 0f
        icon.translationX = dp(-28).toFloat()
        icon.translationY = 0f
        icon.rotation = 0f
        icon.scaleX = 1f
        icon.scaleY = 1f
        icon.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(340)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
            .start()
    }


    private fun showStatsOverlayDialog() {
        val hintColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        val posOptions = listOf(
            StatsOverlayView.POS_TL to "Top left",
            StatsOverlayView.POS_TR to "Top right",
            StatsOverlayView.POS_BL to "Bottom left",
            StatsOverlayView.POS_BR to "Bottom right",
        )
        val posDropdown = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_dropdown_item_1line,
                posOptions.map { it.second }
            ))
        }
        val posLayout = TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Position"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(posDropdown)
        }
        val curPos = prefs.getString(KEY_STATS_POSITION, StatsOverlayView.POS_TL)
        posDropdown.setText(posOptions.firstOrNull { it.first == curPos }?.second ?: posOptions[0].second, false)
        posDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putString(KEY_STATS_POSITION, posOptions[position].first).apply()
        }
        content.addView(posLayout, layoutParams().apply { topMargin = dp(8) })

        val sizeOptions = listOf(
            "small" to "Small",
            "medium" to "Medium",
            "large" to "Large",
        )
        val sizeDropdown = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_dropdown_item_1line,
                sizeOptions.map { it.second }
            ))
        }
        val sizeLayout = TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = "Size"
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(sizeDropdown)
        }
        val curSize = prefs.getString(KEY_STATS_SIZE, "medium")
        sizeDropdown.setText(sizeOptions.firstOrNull { it.first == curSize }?.second ?: sizeOptions[1].second, false)
        sizeDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putString(KEY_STATS_SIZE, sizeOptions[position].first).apply()
        }
        content.addView(sizeLayout, layoutParams().apply { topMargin = dp(12) })

        val opacityLabel = TextView(this).apply {
            text = "Background opacity: ${prefs.getInt(KEY_STATS_OPACITY, 60)}%"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        content.addView(opacityLabel, layoutParams().apply { topMargin = dp(16) })

        val opacitySlider = Slider(this).apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 5f
            value = prefs.getInt(KEY_STATS_OPACITY, 60).toFloat().coerceIn(0f, 100f)
            addOnChangeListener { _, v, _ ->
                prefs.edit().putInt(KEY_STATS_OPACITY, v.toInt()).apply()
                opacityLabel.text = "Background opacity: ${v.toInt()}%"
            }
        }
        content.addView(opacitySlider, layoutParams())

        val itemsLabel = TextView(this).apply {
            text = "Stats"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        content.addView(itemsLabel, layoutParams().apply { topMargin = dp(12) })
        val adapter = StatsReorderAdapter(this)
        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            this.adapter = adapter
            isNestedScrollingEnabled = false
        }
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean =
                adapter.onMove(vh.bindingAdapterPosition, target.bindingAdapterPosition)
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        })
        adapter.touchHelper = touchHelper
        touchHelper.attachToRecyclerView(recycler)
        content.addView(recycler, layoutParams())

        val scroll = androidx.core.widget.NestedScrollView(this).apply { addView(content) }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Overlay settings")
            .setView(scroll)
            .setPositiveButton("Done", null)
            .setNeutralButton("Reset to defaults") { _, _ ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Reset overlay stats?")
                    .setMessage("This turns every stat back on and restores the default order, position, size and opacity.")
                    .setPositiveButton("Reset") { _, _ ->
                        prefs.edit()
                            .remove(KEY_STATS_ITEMS)
                            .remove(KEY_STATS_ORDER)
                            .remove(KEY_STATS_RAM_UNIT)
                            .remove(KEY_STATS_POSITION)
                            .remove(KEY_STATS_SIZE)
                            .remove(KEY_STATS_OPACITY)
                            .apply()
                        showStatsOverlayDialog()
                    }
                    .setNegativeButton("Cancel") { _, _ -> showStatsOverlayDialog() }
                    .show()
            }
            .show()
    }

    private fun showUpdateDialog(current: String, latestTag: String, url: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Update available")
            .setMessage("A newer version ($latestTag) is available.\nCurrently on $current.")
            .setPositiveButton("Update") { _, _ ->
                try {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                } catch (e: Exception) {
                    Log.e("PolyDroid2", "no browser to open update URL", e)
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun currentVersionName(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
    } catch (_: Exception) {
        "0"
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}

private class StatsReorderAdapter(
    private val ctx: Context,
) : RecyclerView.Adapter<StatsReorderAdapter.VH>() {

    private val order = SettingsActivity.getStatsOrder(ctx).toMutableList()
    private val enabled = SettingsActivity.getStatsItems(ctx).toMutableSet()
    lateinit var touchHelper: ItemTouchHelper

    class VH(
        val row: LinearLayout,
        val checkbox: MaterialCheckBox,
        val handle: ImageView,
        val unitLayout: TextInputLayout,
        val unitDropdown: AutoCompleteTextView,
    ) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val checkbox = MaterialCheckBox(ctx)
        val unitDropdown = AutoCompleteTextView(ctx).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("MB", "GB")))
        }
        val unitLayout = TextInputLayout(
            ctx, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            visibility = View.GONE
            addView(unitDropdown)
        }
        val handle = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_drag_handle)
            setColorFilter(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
            scaleType = ImageView.ScaleType.CENTER
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(checkbox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(unitLayout, LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(4)
            })
            addView(handle, LinearLayout.LayoutParams(dp(44), dp(44)))
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
        return VH(row, checkbox, handle, unitLayout, unitDropdown)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = order[position]
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.text = item.label
        holder.checkbox.isChecked = item in enabled
        holder.checkbox.setOnCheckedChangeListener { _, checked ->
            if (checked) enabled.add(item) else enabled.remove(item)
            SettingsActivity.setStatsItem(ctx, item, checked)
        }
        if (item == StatsOverlayView.StatItem.RAM) {
            holder.unitLayout.visibility = View.VISIBLE
            holder.unitDropdown.setText(if (SettingsActivity.getStatsRamInGb(ctx)) "GB" else "MB", false)
            holder.unitDropdown.setOnItemClickListener { _, _, pos, _ ->
                SettingsActivity.setStatsRamInGb(ctx, pos == 1)
            }
        } else {
            holder.unitLayout.visibility = View.GONE
            holder.unitDropdown.setOnItemClickListener(null)
        }
        holder.handle.setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder)
            false
        }
    }

    override fun getItemCount() = order.size

    fun onMove(from: Int, to: Int): Boolean {
        if (from < 0 || to < 0 || from >= order.size || to >= order.size) return false
        order.add(to, order.removeAt(from))
        notifyItemMoved(from, to)
        SettingsActivity.saveStatsOrder(ctx, order)
        return true
    }
}
