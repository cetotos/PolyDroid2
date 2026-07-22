package com.cetotos.polydroid2

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

class Polytoria2GraphicsDialog(private val ctx: Context) {

    private val rootfs = RootFs.rootDir(ctx)
    private val values = Polytoria2Prefs.load(rootfs)
    private var applyingPreset = false

    private lateinit var presetDropdown: AutoCompleteTextView
    private lateinit var msaaDropdown: AutoCompleteTextView
    private lateinit var shadowDropdown: AutoCompleteTextView
    private lateinit var fpsPresetDropdown: AutoCompleteTextView
    private lateinit var uiScaleDropdown: AutoCompleteTextView

    private lateinit var renderScaleField: SettingsUi.SliderField
    private lateinit var shadowDistField: SettingsUi.SliderField
    private lateinit var fpsCapField: SettingsUi.SliderField
    private lateinit var volumeField: SettingsUi.SliderField

    private lateinit var glowSwitch: MaterialSwitch
    private lateinit var ssaoSwitch: MaterialSwitch
    private lateinit var ssrSwitch: MaterialSwitch
    private lateinit var ssilSwitch: MaterialSwitch
    private lateinit var sdfgiSwitch: MaterialSwitch
    private lateinit var normalMapsSwitch: MaterialSwitch

    fun show() {
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        addTitle(content, "Quality preset", top = 0)
        presetDropdown = enumDropdown(Polytoria2Prefs.PRESET_OPTIONS) { name ->
            values[Polytoria2Prefs.PRESET] = name
            if (name != "Custom") applyPreset(name)
        }
        content.addView(dropdownLayout("Graphics preset", presetDropdown), layoutParams())

        addTitle(content, "Display")
        buildDisplay(content)

        addTitle(content, "Graphics")
        buildGraphics(content)

        addTitle(content, "Post-processing")
        buildPostProcessing(content)

        addTitle(content, "General")
        buildGeneral(content)

        loadIntoUi()

        MaterialAlertDialogBuilder(ctx)
            .setTitle("2.0 client settings")
            .setView(ScrollView(ctx).apply { addView(content) })
            .setPositiveButton("Done", null)
            .setOnDismissListener { Polytoria2Prefs.save(rootfs, values) }
            .show()
    }
    private fun buildDisplay(parent: LinearLayout) {
        fpsPresetDropdown = enumDropdown(Polytoria2Prefs.FPS_PRESET_OPTIONS) { name ->
            values[Polytoria2Prefs.FPS_PRESET] = name
            updateFpsCapEnabled()
        }
        parent.addView(dropdownLayout("FPS preset", fpsPresetDropdown), layoutParams())
        fpsCapField = SettingsUi.sliderField(ctx, "FPS cap", 0f, 360f, 6f) { v ->
            if (v.roundToInt() <= 0) "Unlimited" else "${v.roundToInt()}"
        }
        fpsCapField.slider.addOnChangeListener { _, v, fromUser ->
            if (!fromUser) return@addOnChangeListener
            values[Polytoria2Prefs.FPS_CAP] = v.roundToInt()
        }
        parent.addView(fpsCapField.view, layoutParams().apply { topMargin = dp(12) })

        uiScaleDropdown = floatDropdown(Polytoria2Prefs.UI_SCALE_OPTIONS) { value ->
            values[Polytoria2Prefs.UI_SCALE] = value
        }
        parent.addView(dropdownLayout("UI scale", uiScaleDropdown), layoutParams().apply { topMargin = dp(12) })
    }
    private fun buildGraphics(parent: LinearLayout) {
        renderScaleField = SettingsUi.sliderField(ctx, "Render scale", 0.2f, 1f, 0.05f) { v ->
            "${(v * 100).roundToInt()}%"
        }
        renderScaleField.slider.addOnChangeListener { _, v, fromUser ->
            if (applyingPreset || !fromUser) return@addOnChangeListener
            values[Polytoria2Prefs.RENDER_SCALE] = v
            markCustom()
        }
        parent.addView(renderScaleField.view, layoutParams().apply { topMargin = dp(12) })

        msaaDropdown = enumDropdown(Polytoria2Prefs.MSAA_OPTIONS) { name ->
            if (applyingPreset) return@enumDropdown
            values[Polytoria2Prefs.MSAA] = name
            markCustom()
        }
        parent.addView(dropdownLayout("MSAA", msaaDropdown), layoutParams().apply { topMargin = dp(12) })

        shadowDropdown = enumDropdown(Polytoria2Prefs.SHADOW_QUALITY_OPTIONS) { name ->
            if (applyingPreset) return@enumDropdown
            values[Polytoria2Prefs.SHADOW_QUALITY] = name
            markCustom()
        }
        parent.addView(dropdownLayout("Shadow quality", shadowDropdown), layoutParams().apply { topMargin = dp(12) })

        shadowDistField = SettingsUi.sliderField(ctx, "Shadow distance", 5f, 1250f, 5f) { v ->
            "${v.roundToInt()}"
        }
        shadowDistField.slider.addOnChangeListener { _, v, fromUser ->
            if (applyingPreset || !fromUser) return@addOnChangeListener
            values[Polytoria2Prefs.SHADOW_DISTANCE] = v
            markCustom()
        }
        parent.addView(shadowDistField.view, layoutParams().apply { topMargin = dp(12) })
    }

    private fun buildPostProcessing(parent: LinearLayout) {
        glowSwitch = managedSwitch("Glow", Polytoria2Prefs.GLOW)
        ssaoSwitch = managedSwitch("SSAO", Polytoria2Prefs.SSAO)
        ssrSwitch = managedSwitch("SSR", Polytoria2Prefs.SSR)
        ssilSwitch = managedSwitch("SSIL", Polytoria2Prefs.SSIL)
        sdfgiSwitch = managedSwitch("SDFGI", Polytoria2Prefs.SDFGI)
        normalMapsSwitch = managedSwitch("Normal maps", Polytoria2Prefs.NORMAL_MAPS)
        for ((i, sw) in listOf(glowSwitch, ssaoSwitch, ssrSwitch, ssilSwitch, sdfgiSwitch, normalMapsSwitch).withIndex()) {
            parent.addView(sw, layoutParams().apply { if (i > 0) topMargin = dp(8) })
        }
    }

    private fun buildGeneral(parent: LinearLayout) {
        volumeField = SettingsUi.sliderField(ctx, "Master volume", 0f, 100f, 1f) { v ->
            "${v.roundToInt()}%"
        }
        volumeField.slider.addOnChangeListener { _, v, fromUser ->
            if (!fromUser) return@addOnChangeListener
            values[Polytoria2Prefs.MASTER_VOLUME] = v
        }
        parent.addView(volumeField.view, layoutParams())

        parent.addView(boolSwitch("Ctrl lock in third person", Polytoria2Prefs.CTRL_LOCK), layoutParams().apply { topMargin = dp(12) })
    }

    private fun markCustom() {
        values[Polytoria2Prefs.PRESET] = "Custom"
        presetDropdown.setText("Custom", false)
    }

    private fun applyPreset(name: String) {
        val p = Polytoria2Prefs.PRESETS[name] ?: return
        applyingPreset = true
        values[Polytoria2Prefs.RENDER_SCALE] = p.renderScale
        values[Polytoria2Prefs.MSAA] = p.msaa
        values[Polytoria2Prefs.SHADOW_QUALITY] = p.shadowQuality
        values[Polytoria2Prefs.SHADOW_DISTANCE] = p.shadowDistance
        values[Polytoria2Prefs.GLOW] = p.glow
        values[Polytoria2Prefs.SSAO] = p.ssao
        values[Polytoria2Prefs.SSR] = p.ssr
        values[Polytoria2Prefs.SSIL] = p.ssil
        values[Polytoria2Prefs.SDFGI] = p.sdfgi
        values[Polytoria2Prefs.NORMAL_MAPS] = p.normalMaps
        setSlider(renderScaleField.slider, p.renderScale)
        msaaDropdown.setText(labelFor(Polytoria2Prefs.MSAA_OPTIONS, p.msaa), false)
        shadowDropdown.setText(labelFor(Polytoria2Prefs.SHADOW_QUALITY_OPTIONS, p.shadowQuality), false)
        setSlider(shadowDistField.slider, p.shadowDistance)
        glowSwitch.isChecked = p.glow
        ssaoSwitch.isChecked = p.ssao
        ssrSwitch.isChecked = p.ssr
        ssilSwitch.isChecked = p.ssil
        sdfgiSwitch.isChecked = p.sdfgi
        normalMapsSwitch.isChecked = p.normalMaps
        applyingPreset = false
    }

    private fun loadIntoUi() {
        presetDropdown.setText(labelFor(Polytoria2Prefs.PRESET_OPTIONS, str(Polytoria2Prefs.PRESET)), false)
        msaaDropdown.setText(labelFor(Polytoria2Prefs.MSAA_OPTIONS, str(Polytoria2Prefs.MSAA)), false)
        shadowDropdown.setText(labelFor(Polytoria2Prefs.SHADOW_QUALITY_OPTIONS, str(Polytoria2Prefs.SHADOW_QUALITY)), false)
        fpsPresetDropdown.setText(labelFor(Polytoria2Prefs.FPS_PRESET_OPTIONS, str(Polytoria2Prefs.FPS_PRESET)), false)
        uiScaleDropdown.setText(labelForFloat(Polytoria2Prefs.UI_SCALE_OPTIONS, flt(Polytoria2Prefs.UI_SCALE)), false)

        setSlider(renderScaleField.slider, flt(Polytoria2Prefs.RENDER_SCALE))
        setSlider(shadowDistField.slider, flt(Polytoria2Prefs.SHADOW_DISTANCE))
        setSlider(fpsCapField.slider, (values[Polytoria2Prefs.FPS_CAP] as Int).toFloat())
        setSlider(volumeField.slider, flt(Polytoria2Prefs.MASTER_VOLUME))

        glowSwitch.isChecked = bool(Polytoria2Prefs.GLOW)
        ssaoSwitch.isChecked = bool(Polytoria2Prefs.SSAO)
        ssrSwitch.isChecked = bool(Polytoria2Prefs.SSR)
        ssilSwitch.isChecked = bool(Polytoria2Prefs.SSIL)
        sdfgiSwitch.isChecked = bool(Polytoria2Prefs.SDFGI)
        normalMapsSwitch.isChecked = bool(Polytoria2Prefs.NORMAL_MAPS)

        updateFpsCapEnabled()
    }

    private fun updateFpsCapEnabled() {
        fpsCapField.setEnabledDimmed(str(Polytoria2Prefs.FPS_PRESET) == "Custom")
    }

    private fun str(key: String) = values[key] as String
    private fun flt(key: String) = (values[key] as Number).toFloat()
    private fun bool(key: String) = values[key] as Boolean


    private fun labelFor(options: List<Pair<String, String>>, value: String): String =
        options.firstOrNull { it.first == value }?.second ?: value

    private fun labelForFloat(options: List<Pair<Float, String>>, value: Float): String =
        options.minByOrNull { kotlin.math.abs(it.first - value) }?.second ?: value.toString()

    private fun setSlider(slider: Slider, value: Float) {
        val steps = ((value - slider.valueFrom) / slider.stepSize).roundToInt()
        val snapped = (slider.valueFrom + steps * slider.stepSize).coerceIn(slider.valueFrom, slider.valueTo)
        slider.value = snapped
    }

    private fun boolSwitch(text: String, key: String): MaterialSwitch =
        MaterialSwitch(ctx).apply {
            this.text = text
            setOnCheckedChangeListener { _, checked -> values[key] = checked }
        }

    private fun managedSwitch(text: String, key: String): MaterialSwitch =
        MaterialSwitch(ctx).apply {
            this.text = text
            setOnCheckedChangeListener { _, checked ->
                if (applyingPreset) return@setOnCheckedChangeListener
                values[key] = checked
                markCustom()
            }
        }

    private fun enumDropdown(options: List<Pair<String, String>>, onSelected: (String) -> Unit): AutoCompleteTextView {
        val view = AutoCompleteTextView(ctx).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, options.map { it.second }))
        }
        view.setOnItemClickListener { _, _, position, _ -> onSelected(options[position].first) }
        return view
    }

    private fun floatDropdown(options: List<Pair<Float, String>>, onSelected: (Float) -> Unit): AutoCompleteTextView {
        val view = AutoCompleteTextView(ctx).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, options.map { it.second }))
        }
        view.setOnItemClickListener { _, _, position, _ -> onSelected(options[position].first) }
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

    private fun dp(value: Int): Int = (value * ctx.resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
