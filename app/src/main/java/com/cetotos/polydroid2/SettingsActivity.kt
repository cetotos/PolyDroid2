package com.cetotos.polydroid2

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "polydroid_settings"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_CUSTOM_ENABLED = "custom_resolution_enabled"
        const val KEY_CUSTOM_WIDTH = "custom_width"
        const val KEY_CUSTOM_HEIGHT = "custom_height"
        const val KEY_CAMERA_SENSITIVITY = "camera_sensitivity"
        const val KEY_SHOW_STATS = "show_stats"
        const val DEFAULT_RESOLUTION = 720
        const val DEFAULT_SENSITIVITY = 3f

        private val PRESETS = listOf(
            240 to "240p",
            360 to "360p",
            480 to "480p",
            720 to "720p (Default)",
            900 to "900p",
            1080 to "1080p",
            1440 to "1440p",
        )

        fun getCameraSensitivity(ctx: Context): Float {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_SENSITIVITY)
        }

        fun getShowStats(ctx: Context): Boolean {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(KEY_SHOW_STATS, true)
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
    }

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val toolbar = MaterialToolbar(this).apply {
            title = "Settings"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }

        // resolution dropdown
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
        content.addView(dropdownLayout, layoutParams())

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
            setPadding(0, dp(16), 0, 0)
        }
        content.addView(customSwitch, layoutParams())

        val customContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
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

        content.addView(customContainer, layoutParams())

        // load custom state
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

        val sensitivityLabel = TextView(this).apply {
            setPadding(0, dp(24), 0, 0)
            text = "Camera sensitivity: ${"%.1f".format(prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_SENSITIVITY))}x"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        content.addView(sensitivityLabel, layoutParams())

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
        content.addView(sensitivitySlider, layoutParams())

        val statsSwitch = MaterialSwitch(this).apply {
            text = "Performance stats overlay"
            setPadding(0, dp(16), 0, 0)
            isChecked = prefs.getBoolean(KEY_SHOW_STATS, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KEY_SHOW_STATS, checked).apply()
            }
        }
        content.addView(statsSwitch, layoutParams())

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            addView(content)
        }

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
