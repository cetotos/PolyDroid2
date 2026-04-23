package com.cetotos.polydroid2

import android.content.Context
import android.util.Log
import java.io.File

object PolytoriaPrefs {
    private const val TAG = "PolyDroid2"
    private const val RELATIVE_PATH = "home/user/.config/unity3d/unknown/unknown/prefs"

    private data class Entry(val name: String, val type: String, val value: String)

    fun applyTo(ctx: Context, rootfs: File) {
        val file = File(rootfs, RELATIVE_PATH)
        file.parentFile?.mkdirs()

        val entries = if (file.exists()) parse(file.readText(Charsets.UTF_8)) else linkedMapOf()
        val prefs = ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        fun putInt(name: String, value: Int) { entries[name] = Entry(name, "int", value.toString()) }
        fun putFloat(name: String, value: Float) { entries[name] = Entry(name, "float", trimFloat(value)) }
        fun has(name: String) = prefs.contains(name)

        if (has(SettingsActivity.KEY_POLY_QUALITY_LEVEL)) putInt("Graphics_QualityLevel", prefs.getInt(SettingsActivity.KEY_POLY_QUALITY_LEVEL, 3))
        if (has(SettingsActivity.KEY_POLY_TEXTURE_QUALITY)) putInt("Graphics_TextureQuality", prefs.getInt(SettingsActivity.KEY_POLY_TEXTURE_QUALITY, 0))
        if (has(SettingsActivity.KEY_POLY_SHADOW_RESOLUTION)) putInt("Graphics_ShadowResolution", prefs.getInt(SettingsActivity.KEY_POLY_SHADOW_RESOLUTION, 2))
        if (has(SettingsActivity.KEY_POLY_SHADOW_DISTANCE)) putFloat("Graphics_ShadowDistance", prefs.getFloat(SettingsActivity.KEY_POLY_SHADOW_DISTANCE, 50f))
        if (has(SettingsActivity.KEY_POLY_ANTI_ALIASING)) putInt("Graphics_AntiAliasing", prefs.getInt(SettingsActivity.KEY_POLY_ANTI_ALIASING, 2))
        if (has(SettingsActivity.KEY_POLY_ANISOTROPIC)) putInt("Graphics_AnisotropicFiltering", prefs.getInt(SettingsActivity.KEY_POLY_ANISOTROPIC, 1))
        if (has(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT)) putInt("Graphics_PixelLightCount", prefs.getInt(SettingsActivity.KEY_POLY_PIXEL_LIGHT_COUNT, 2))
        if (has(SettingsActivity.KEY_POLY_POST_PROCESSING)) putInt("Graphics_PostProcessing", if (prefs.getBoolean(SettingsActivity.KEY_POLY_POST_PROCESSING, true)) 1 else 0)
        if (has(SettingsActivity.KEY_POLY_FULLSCREEN)) putInt("Graphics_Fullscreen", if (prefs.getBoolean(SettingsActivity.KEY_POLY_FULLSCREEN, true)) 1 else 0)
        if (has(SettingsActivity.KEY_POLY_MASTER_VOLUME)) putFloat("Audio_MasterVolume", prefs.getFloat(SettingsActivity.KEY_POLY_MASTER_VOLUME, 1f))

        // force V-Sync off it tanks fps and isnt needed
        putInt("Graphics_VSync", 0)

        file.writeText(serialize(entries.values), Charsets.UTF_8)
        Log.i(TAG, "wrote ${entries.size} polytoria prefs -> ${file.absolutePath}")
    }

    private fun trimFloat(v: Float): String {
        val s = v.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    private val prefRegex = Regex(
        "<pref\\s+name=\"([^\"]+)\"\\s+type=\"([^\"]+)\"\\s*>([^<]*)</pref>",
        RegexOption.IGNORE_CASE,
    )

    private fun parse(xml: String): LinkedHashMap<String, Entry> {
        val out = linkedMapOf<String, Entry>()
        for (m in prefRegex.findAll(xml)) {
            val name = m.groupValues[1]
            out[name] = Entry(name, m.groupValues[2], m.groupValues[3])
        }
        return out
    }

    private fun serialize(entries: Collection<Entry>): String {
        val sb = StringBuilder()
        sb.append("<unity_prefs version_major=\"1\" version_minor=\"1\">\r\n")
        for (e in entries) {
            sb.append("\t<pref name=\"").append(e.name)
                .append("\" type=\"").append(e.type).append("\">")
                .append(e.value).append("</pref>\r\n")
        }
        sb.append("</unity_prefs>\r\n")
        return sb.toString()
    }
}
