package com.cetotos.polydroid2

import android.util.Log
import org.json.JSONObject
import java.io.File

object Polytoria2Prefs {
    private const val TAG = "PolyDroid2"
    private const val RELATIVE_PATH = "home/user/.local/share/PolytoriaClient/settings_client.json"

    const val FPS_PRESET = "display.fps_preset"
    const val FPS_CAP = "display.fps_cap"
    const val UI_SCALE = "display.ui_scale"

    const val PRESET = "graphics.preset"
    const val RENDERING_METHOD = "graphics.rendering_method"
    const val RENDER_SCALE = "graphics.render_scale"
    const val MSAA = "graphics.msaa"
    const val SHADOW_QUALITY = "graphics.shadow_quality"
    const val SHADOW_DISTANCE = "graphics.shadow_distance"

    const val GLOW = "graphics.post_processing.glow"
    const val SSAO = "graphics.post_processing.ssao"
    const val SSR = "graphics.post_processing.ssr"
    const val SSIL = "graphics.post_processing.ssil"
    const val SDFGI = "graphics.post_processing.sdfgi"
    const val NORMAL_MAPS = "graphics.post_processing.normal_maps"

    const val MASTER_VOLUME = "general.master_volume"
    const val CTRL_LOCK = "general.ctrl_lock"

    val PRESET_OPTIONS = listOf(
        "Low" to "Low", "Medium" to "Medium", "High" to "High",
        "Ultra" to "Ultra", "Photo" to "Photo", "Custom" to "Custom",
    )
    val MSAA_OPTIONS = listOf(
        "Disabled" to "Off", "X2" to "2x", "X4" to "4x", "X8" to "8x",
    )
    val SHADOW_QUALITY_OPTIONS = listOf(
        "Off" to "Off", "Low" to "Low", "Medium" to "Medium",
        "High" to "High", "Ultra" to "Ultra",
    )
    val FPS_PRESET_OPTIONS = listOf(
        "Custom" to "Custom", "Reduced" to "Reduced (30)", "Standard" to "Standard (60)",
        "Extended" to "Extended (90)", "Smooth" to "Smooth (120)", "Slick" to "Slick (144)",
        "Fluid" to "Fluid (240)", "Limitless" to "Limitless",
    )
    val UI_SCALE_OPTIONS = listOf(
        0.5f to "0.5x", 0.75f to "0.75x", 1f to "1x", 1.25f to "1.25x",
        1.5f to "1.5x", 1.75f to "1.75x", 2f to "2x",
    )

    val DEFAULTS: Map<String, Any> = mapOf(
        FPS_PRESET to "Custom",
        FPS_CAP to 0,
        UI_SCALE to 1f,
        PRESET to "Medium",
        RENDERING_METHOD to "Performance",
        RENDER_SCALE to 1f,
        MSAA to "X2",
        SHADOW_QUALITY to "Medium",
        SHADOW_DISTANCE to 1000f,
        GLOW to true,
        SSAO to true,
        SSR to false,
        SSIL to false,
        SDFGI to false,
        NORMAL_MAPS to true,
        MASTER_VOLUME to 80f,
        CTRL_LOCK to true,
    )

    data class Preset(
        val renderScale: Float,
        val msaa: String,
        val shadowQuality: String,
        val shadowDistance: Float,
        val glow: Boolean,
        val ssao: Boolean,
        val ssr: Boolean,
        val ssil: Boolean,
        val sdfgi: Boolean,
        val normalMaps: Boolean,
    )

    val PRESETS: Map<String, Preset> = mapOf(
        "Low" to Preset(0.75f, "Disabled", "Off", 100f, false, false, false, false, false, false),
        "Medium" to Preset(1f, "X2", "Medium", 1000f, true, true, false, false, false, true),
        "High" to Preset(1f, "X4", "High", 1250f, true, true, true, false, false, true),
        "Ultra" to Preset(1f, "X8", "Ultra", 1250f, true, true, true, true, false, true),
        "Photo" to Preset(1f, "X8", "Ultra", 1250f, true, true, true, true, true, true),
    )

    private fun file(rootfs: File) = File(rootfs, RELATIVE_PATH)

    fun load(rootfs: File): MutableMap<String, Any> {
        val out = HashMap<String, Any>(DEFAULTS)
        val f = file(rootfs)
        if (!f.exists()) return out
        try {
            val json = JSONObject(f.readText(Charsets.UTF_8))
            for (key in DEFAULTS.keys) {
                if (!json.has(key) || json.isNull(key)) continue
                out[key] = when (DEFAULTS[key]) {
                    is Boolean -> json.optBoolean(key, out[key] as Boolean)
                    is Int -> json.optInt(key, out[key] as Int)
                    is Float -> json.optDouble(key, (out[key] as Float).toDouble()).toFloat()
                    else -> json.optString(key, out[key] as String)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to read settings_client.json: ${e.message}")
        }
        out[RENDERING_METHOD] = "Performance"
        return out
    }

    fun save(rootfs: File, values: Map<String, Any>) {
        val f = file(rootfs)
        f.parentFile?.mkdirs()
        val json = try {
            if (f.exists()) JSONObject(f.readText(Charsets.UTF_8)) else JSONObject()
        } catch (_: Exception) { JSONObject() }
        for ((key, value) in values) {
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Float -> json.put(key, value.toDouble())
                else -> json.put(key, value.toString())
            }
        }
        try {
            f.writeText(json.toString(2), Charsets.UTF_8)
            Log.i(TAG, "wrote ${values.size} 2.0 settings -> ${f.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "failed to write settings_client.json: ${e.message}")
        }
    }
}
