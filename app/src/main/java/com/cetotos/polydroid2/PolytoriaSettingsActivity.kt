package com.cetotos.polydroid2

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout

class PolytoriaSettingsActivity : AppCompatActivity() {

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
        // high is very, very laggy
        "High (Laggy!)"   to Preset(5, true,  4, 150f, 3, 0, 2, 2),
        "Medium" to Preset(3, false, 2, 50f,  1, 1, 1, 1),
        "Low"    to Preset(1, false, 2, 50f,  0, 1, 0, 0),
        "Potato" to Preset(0, false, 0, 0f,   0, 2, 0, 0),
    )

    private val qualityNames = listOf("Very Low", "Low", "Medium", "High", "Very High", "Ultra")
    private val textureNames = listOf("Full", "Half", "Quarter", "Eighth")
    private val shadowResNames = listOf("Low", "Medium", "High", "Very High")
    private val aaNames = listOf("Disabled", "FXAA", "SMAA")
    private val anisoNames = listOf("Off", "Per-texture", "Force on")

    private lateinit var prefs: SharedPreferences

    private lateinit var presetDropdown: AutoCompleteTextView
    private lateinit var qualityDropdown: AutoCompleteTextView
    private lateinit var textureDropdown: AutoCompleteTextView
    private lateinit var shadowResDropdown: AutoCompleteTextView
    private lateinit var msaaDropdown: AutoCompleteTextView
    private lateinit var anisoDropdown: AutoCompleteTextView
    private lateinit var pixelLightsSlider: Slider
    private lateinit var shadowDistanceSlider: Slider
    private lateinit var postProcessingSwitch: MaterialSwitch
    private lateinit var fullscreenSwitch: MaterialSwitch
    private lateinit var masterVolumeSlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Polytoria graphics"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        content.addView(buildPresetCard(), cardParams(first = true))
        content.addView(buildGraphicsCard(), cardParams())
        content.addView(buildAudioCard(), cardParams())

        root.addView(toolbar)
        root.addView(
            ScrollView(this).apply { addView(content) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        if (!prefs.contains(SettingsActivity.KEY_POLY_PRESET)) {
            prefs.edit().putString(SettingsActivity.KEY_POLY_PRESET, SettingsActivity.DEFAULT_POLY_PRESET).apply()
            presets[SettingsActivity.DEFAULT_POLY_PRESET]?.let { applyPreset(it) }
        }
        loadCurrentValues()
    }

    private fun buildPresetCard(): View {
        val (card, inner) = section("Quality preset")
        val names = presets.keys.toList()
        presetDropdown = dropdown("Preset", names) { idx ->
            val name = names[idx]
            prefs.edit().putString(SettingsActivity.KEY_POLY_PRESET, name).apply()
            applyPreset(presets.getValue(name))
        }
        inner.addView(dropdownLayout("Preset", presetDropdown), layoutParams())
        return card
    }

    private fun buildGraphicsCard(): View {
        val (card, inner) = section("Graphics")

        qualityDropdown = dropdown("Quality level", qualityNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_QUALITY_LEVEL, idx).apply()
        }
        inner.addView(dropdownLayout("Quality level", qualityDropdown), layoutParams())

        textureDropdown = dropdown("Texture quality", textureNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_TEXTURE_QUALITY, idx).apply()
        }
        inner.addView(dropdownLayout("Texture quality", textureDropdown), layoutParams().apply { topMargin = dp(12) })

        shadowResDropdown = dropdown("Shadow resolution", shadowResNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_SHADOW_RESOLUTION, idx).apply()
        }
        inner.addView(dropdownLayout("Shadow resolution", shadowResDropdown), layoutParams().apply { topMargin = dp(12) })

        msaaDropdown = dropdown("Anti-aliasing", aaNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_ANTI_ALIASING, idx).apply()
        }
        inner.addView(dropdownLayout("Anti-aliasing", msaaDropdown), layoutParams().apply { topMargin = dp(12) })

        anisoDropdown = dropdown("Anisotropic filtering", anisoNames) { idx ->
            prefs.edit().putInt(SettingsActivity.KEY_POLY_ANISOTROPIC, idx).apply()
        }
        inner.addView(dropdownLayout("Anisotropic filtering", anisoDropdown), layoutParams().apply { topMargin = dp(12) })

        inner.addView(labelView("Pixel light count"), layoutParams().apply { topMargin = dp(16) })
        pixelLightsSlider = Slider(this).apply {
            valueFrom = 0f
            valueTo = 8f
            stepSize = 1f
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) prefs.edit().putInt(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT, value.toInt()).apply()
            }
        }
        inner.addView(pixelLightsSlider, layoutParams())

        inner.addView(labelView("Shadow distance"), layoutParams().apply { topMargin = dp(8) })
        shadowDistanceSlider = Slider(this).apply {
            valueFrom = 0f
            valueTo = 150f
            stepSize = 5f
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) prefs.edit().putFloat(SettingsActivity.KEY_POLY_SHADOW_DISTANCE, value).apply()
            }
        }
        inner.addView(shadowDistanceSlider, layoutParams())

        postProcessingSwitch = MaterialSwitch(this).apply {
            text = "Post-processing"
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(SettingsActivity.KEY_POLY_POST_PROCESSING, checked).apply()
            }
        }
        inner.addView(postProcessingSwitch, layoutParams().apply { topMargin = dp(16) })

        fullscreenSwitch = MaterialSwitch(this).apply {
            text = "Fullscreen"
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(SettingsActivity.KEY_POLY_FULLSCREEN, checked).apply()
            }
        }
        inner.addView(fullscreenSwitch, layoutParams().apply { topMargin = dp(8) })

        return card
    }

    private fun buildAudioCard(): View {
        val (card, inner) = section("Audio")

        inner.addView(labelView("Master volume"), layoutParams())
        masterVolumeSlider = Slider(this).apply {
            valueFrom = 0f
            valueTo = 1f
            stepSize = 0.05f
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) prefs.edit().putFloat(SettingsActivity.KEY_POLY_MASTER_VOLUME, value).apply()
            }
        }
        inner.addView(masterVolumeSlider, layoutParams())
        return card
    }

    private fun dropdown(hint: String, options: List<String>, onSelected: (Int) -> Unit): AutoCompleteTextView {
        val view = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(this@PolytoriaSettingsActivity, android.R.layout.simple_dropdown_item_1line, options))
        }
        view.setOnItemClickListener { _, _, position, _ -> onSelected(position) }
        return view
    }

    private fun dropdownLayout(hint: String, inner: AutoCompleteTextView): View {
        return TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            this.hint = hint
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(inner)
        }
    }

    private fun labelView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
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

        pixelLightsSlider.value = prefs.getInt(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT, 2).toFloat().coerceIn(0f, 8f)
        shadowDistanceSlider.value = prefs.getFloat(SettingsActivity.KEY_POLY_SHADOW_DISTANCE, 50f).coerceIn(0f, 150f)
        postProcessingSwitch.isChecked = prefs.getBoolean(SettingsActivity.KEY_POLY_POST_PROCESSING, true)
        fullscreenSwitch.isChecked = prefs.getBoolean(SettingsActivity.KEY_POLY_FULLSCREEN, true)
        masterVolumeSlider.value = prefs.getFloat(SettingsActivity.KEY_POLY_MASTER_VOLUME, 1f).coerceIn(0f, 1f)
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

    private fun section(title: String): Pair<MaterialCardView, LinearLayout> {
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
            this.text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        }
        inner.addView(titleView, layoutParams().apply { bottomMargin = dp(14) })
        card.addView(inner)
        return card to inner
    }

    private fun cardParams(first: Boolean = false): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { if (!first) topMargin = dp(12) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun layoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
