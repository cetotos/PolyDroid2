package com.cetotos.polydroid2

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.google.android.material.slider.TickVisibilityMode

object SettingsUi {

    class SliderField(val view: LinearLayout, val slider: Slider) {
        fun setEnabledDimmed(enabled: Boolean) {
            slider.isEnabled = enabled
            view.alpha = if (enabled) 1f else 0.5f
        }
    }

    fun sliderField(
        ctx: Context,
        name: String,
        from: Float,
        to: Float,
        step: Float,
        ticks: Boolean = false,
        format: ((Float) -> String)? = null,
    ): SliderField {
        val label = TextView(ctx).apply {
            text = name
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        }
        val slider = Slider(ctx).apply {
            valueFrom = from
            valueTo = to
            stepSize = step
            tickVisibilityMode = if (ticks) TickVisibilityMode.TICK_VISIBILITY_AUTO_LIMIT
                else TickVisibilityMode.TICK_VISIBILITY_HIDDEN
            if (format != null) setLabelFormatter { format(it) }
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(slider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return SliderField(col, slider)
    }
}
