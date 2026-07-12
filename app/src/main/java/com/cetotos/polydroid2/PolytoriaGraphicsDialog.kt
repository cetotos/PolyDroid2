package com.cetotos.polydroid2

import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout

class PolytoriaGraphicsDialog(private val ctx: Context) {

    data class Preset(
        val quality: Int,
        val postProcessing: Boolean,
        val pixelLights: Int,
        val shadowDistance: Float,
        val shadowResolution: Int,
        val textureQuality: Int,
        val antiAliasing: Int,
        val anisotropic: Int,
    )

    private val presets = linkedMapOf(
        "High (Laggy!)" to Preset(5, true, 4, 150f, 3, 0, 2, 2),
        "Medium" to Preset(3, false, 2, 50f, 1, 1, 1, 1),
        "Low" to Preset(1, false, 2, 50f, 0, 1, 0, 0),
        "Potato" to Preset(0, false, 0, 0f, 0, 2, 0, 0),
    )

    private val qualityNames = listOf("Very Low", "Low", "Medium", "High", "Very High", "Ultra")
    private val textureNames = listOf("Full", "Half", "Quarter", "Eighth")
    private val shadowResNames = listOf("Low", "Medium", "High", "Very High")
    private val aaNames = listOf("Disabled", "FXAA", "SMAA")
    private val anisoNames = listOf("Off", "Per-texture", "Force on")

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

    private lateinit var presetDropdown: AutoCompleteTextView
    private lateinit var qualityDropdown: AutoCompleteTextView
    private lateinit var textureDropdown: AutoCompleteTextView
    private lateinit var shadowResDropdown: AutoCompleteTextView
    private lateinit var msaaDropdown: AutoCompleteTextView
    private lateinit var anisoDropdown: AutoCompleteTextView
    private lateinit var pixelLightsField: SettingsUi.SliderField
    private lateinit var shadowDistanceField: SettingsUi.SliderField
    private lateinit var postProcessingSwitch: MaterialSwitch
    private lateinit var fullscreenSwitch: MaterialSwitch
    private lateinit var masterVolumeField: SettingsUi.SliderField

    fun show() {
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        addTitle(content, "Quality preset", top = 0)
        buildPreset(content)

        addTitle(content, "Graphics")
        buildGraphics(content)

        addTitle(content, "Audio")
        buildAudio(content)

        if (!prefs.contains(SettingsActivity.KEY_POLY_PRESET)) {
            prefs.edit().putString(SettingsActivity.KEY_POLY_PRESET, SettingsActivity.DEFAULT_POLY_PRESET).apply()
            presets[SettingsActivity.DEFAULT_POLY_PRESET]?.let { applyPreset(it) }
        }
        loadCurrentValues()

        MaterialAlertDialogBuilder(ctx)
            .setTitle("1.0 client settings")
            .setView(ScrollView(ctx).apply { addView(content) })
            .setPositiveButton("Done", null)
            .show()
    }

    private fun buildPreset(parent: LinearLayout) {
        val names = presets.keys.toList()
        presetDropdown = dropdown(names) { idx ->
            val name = names[idx]
            prefs.edit().putString(SettingsActivity.KEY_POLY_PRESET, name).apply()
            applyPreset(presets.getValue(name))
        }
        parent.addView(dropdownLayout("Preset", presetDropdown), layoutParams())
    }

    private fun buildGraphics(parent: LinearLayout) {
        qualityDropdown = dropdown(qualityNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_QUALITY_LEVEL, idx).apply()
        }
        parent.addView(dropdownLayout("Quality level", qualityDropdown), layoutParams())

        textureDropdown = dropdown(textureNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_TEXTURE_QUALITY, idx).apply()
        }
        parent.addView(dropdownLayout("Texture quality", textureDropdown), layoutParams().apply { topMargin = dp(12) })

        shadowResDropdown = dropdown(shadowResNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_SHADOW_RESOLUTION, idx).apply()
        }
        parent.addView(dropdownLayout("Shadow resolution", shadowResDropdown), layoutParams().apply { topMargin = dp(12) })

        msaaDropdown = dropdown(aaNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_ANTI_ALIASING, idx).apply()
        }
        parent.addView(dropdownLayout("Anti-aliasing", msaaDropdown), layoutParams().apply { topMargin = dp(12) })

        anisoDropdown = dropdown(anisoNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_ANISOTROPIC, idx).apply()
        }
        parent.addView(dropdownLayout("Anisotropic filtering", anisoDropdown), layoutParams().apply { topMargin = dp(12) })

        pixelLightsField = SettingsUi.sliderField(ctx, "Pixel light count", 0f, 8f, 1f, ticks = true) { "${it.toInt()}" }
        pixelLightsField.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) prefs.edit().putInt(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT, value.toInt()).apply()
        }
        parent.addView(pixelLightsField.view, layoutParams().apply { topMargin = dp(16) })

        shadowDistanceField = SettingsUi.sliderField(ctx, "Shadow distance", 0f, 150f, 5f) { "${it.toInt()}" }
        shadowDistanceField.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) prefs.edit().putFloat(SettingsActivity.KEY_POLY_SHADOW_DISTANCE, value).apply()
        }
        parent.addView(shadowDistanceField.view, layoutParams().apply { topMargin = dp(8) })

        postProcessingSwitch = MaterialSwitch(ctx).apply {
            text = "Post-processing"
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(SettingsActivity.KEY_POLY_POST_PROCESSING, checked).apply()
            }
        }
        parent.addView(postProcessingSwitch, layoutParams().apply { topMargin = dp(16) })

        fullscreenSwitch = MaterialSwitch(ctx).apply {
            text = "Fullscreen"
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(SettingsActivity.KEY_POLY_FULLSCREEN, checked).apply()
            }
        }
        parent.addView(fullscreenSwitch, layoutParams().apply { topMargin = dp(8) })
    }

    private fun buildAudio(parent: LinearLayout) {
        masterVolumeField = SettingsUi.sliderField(ctx, "Master volume", 0f, 1f, 0.05f) { "${(it * 100).toInt()}%" }
        masterVolumeField.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) prefs.edit().putFloat(SettingsActivity.KEY_POLY_MASTER_VOLUME, value).apply()
        }
        parent.addView(masterVolumeField.view, layoutParams())
    }

    private fun dropdown(options: List<String>, onSelected: (Int) -> Unit): AutoCompleteTextView {
        val view = AutoCompleteTextView(ctx).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, options))
        }
        view.setOnItemClickListener { _, _, position, _ -> onSelected(position) }
        return view
    }

    private fun dropdownLayout(name: String, inner: AutoCompleteTextView): View {
        val label = TextView(ctx).apply {
            text = name
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        }
        val til = TextInputLayout(
            ctx, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            isHintEnabled = false
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(inner)
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(label, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(til, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) })
        }
    }

    private fun addTitle(parent: LinearLayout, text: String, top: Int = dp(20)) {
        val tv = TextView(ctx).apply {
            this.text = text
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        }
        parent.addView(tv, layoutParams().apply { topMargin = top; bottomMargin = dp(6) })
    }

    private fun loadCurrentValues() {
        val presetName = prefs.getString(SettingsActivity.KEY_POLY_PRESET, null)
        if (presetName != null && presets.containsKey(presetName)) {
            presetDropdown.setText(presetName, false)
        }

        val q = prefs.getInt(SettingsActivity.KEY_POLY_QUALITY_LEVEL, 3).coerceIn(0, qualityNames.size - 1)
        qualityDropdown.setText(qualityNames[q], false)

        val t = prefs.getInt(SettingsActivity.KEY_POLY_TEXTURE_QUALITY, 0).coerceIn(0, textureNames.size - 1)
        textureDropdown.setText(textureNames[t], false)

        val sr = prefs.getInt(SettingsActivity.KEY_POLY_SHADOW_RESOLUTION, 2).coerceIn(0, shadowResNames.size - 1)
        shadowResDropdown.setText(shadowResNames[sr], false)

        val aa = prefs.getInt(SettingsActivity.KEY_POLY_ANTI_ALIASING, 1).coerceIn(0, aaNames.size - 1)
        msaaDropdown.setText(aaNames[aa], false)

        val aniso = prefs.getInt(SettingsActivity.KEY_POLY_ANISOTROPIC, 1).coerceIn(0, anisoNames.size - 1)
        anisoDropdown.setText(anisoNames[aniso], false)

        pixelLightsField.slider.value = prefs.getInt(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT, 2).toFloat().coerceIn(0f, 8f)
        shadowDistanceField.slider.value = prefs.getFloat(SettingsActivity.KEY_POLY_SHADOW_DISTANCE, 50f).coerceIn(0f, 150f)
        postProcessingSwitch.isChecked = prefs.getBoolean(SettingsActivity.KEY_POLY_POST_PROCESSING, true)
        fullscreenSwitch.isChecked = prefs.getBoolean(SettingsActivity.KEY_POLY_FULLSCREEN, true)
        masterVolumeField.slider.value = prefs.getFloat(SettingsActivity.KEY_POLY_MASTER_VOLUME, 1f).coerceIn(0f, 1f)
    }

    private fun applyPreset(p: Preset) {
        prefs.edit()
            .putInt(SettingsActivity.KEY_POLY_QUALITY_LEVEL, p.quality)
            .putBoolean(SettingsActivity.KEY_POLY_POST_PROCESSING, p.postProcessing)
            .putInt(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT, p.pixelLights)
            .putFloat(SettingsActivity.KEY_POLY_SHADOW_DISTANCE, p.shadowDistance)
            .putInt(SettingsActivity.KEY_POLY_SHADOW_RESOLUTION, p.shadowResolution)
            .putInt(SettingsActivity.KEY_POLY_TEXTURE_QUALITY, p.textureQuality)
            .putInt(SettingsActivity.KEY_POLY_ANTI_ALIASING, p.antiAliasing)
            .putInt(SettingsActivity.KEY_POLY_ANISOTROPIC, p.anisotropic)
            .apply()
        loadCurrentValues()
    }

    private fun dp(value: Int): Int = (value * ctx.resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
