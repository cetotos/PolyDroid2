package com.cetotos.polydroid2

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView

class StatsOverlayView(context: Context) : LinearLayout(context) {

    enum class StatItem(val key: String, val label: String) {
        FPS("fps", "FPS"),
        FRAMES("frames", "Frame count"),
        GPU("gpu", "GPU usage"),
        GPU_TEMP("gpu_temp", "GPU temperature"),
        CPU("cpu", "CPU usage"),
        CPU_TEMP("cpu_temp", "CPU temperature"),
        RAM("ram", "RAM usage"),
        RES("res", "Resolution"),
        BATTERY("battery", "Battery"),
        UPTIME("uptime", "Session time"),
        VULKAN("vulkan", "GPU / Vulkan info"),
    }

    data class Metrics(
        val unityFps: Int,
        val totalFrames: Int,
        val gpuName: String,
        val gpuUsage: Int,
        val gpuTemp: Int,
        val cpuUsage: Int,
        val cpuTemp: Int,
        val usedMb: Long,
        val totalMb: Long,
        val ramPct: Int,
        val ramInGb: Boolean,
        val renderWidth: Int,
        val renderHeight: Int,
        val battery: Int,
        val uptimeMs: Long,
        val vulkanInfo: String,
    )

    private var items: List<StatItem> = StatItem.entries.toList()
    private var sizeScale = 1f
    private var opacityPct = 60
    private val rows = LinkedHashMap<StatItem, TextView>()

    init {
        orientation = VERTICAL
        setPadding(dp(11), dp(7), dp(11), dp(8))
        isClickable = false
        isFocusable = false
        rebuildRows()
        applyBackground()
    }

    fun configure(items: List<StatItem>, sizeScale: Float, opacityPct: Int) {
        this.items = items
        this.sizeScale = sizeScale
        this.opacityPct = opacityPct
        rebuildRows()
        applyBackground()
    }

    private fun rebuildRows() {
        removeAllViews()
        rows.clear()
        for (item in items) {
            val tv = TextView(context).apply {
                includeFontPadding = false
                if (item == StatItem.FPS) {
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setShadowLayer(3f, 0f, 1f, 0xFF000000.toInt())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * sizeScale)
                } else {
                    typeface = Typeface.MONOSPACE
                    setTextColor(0xFFDDDDDD.toInt())
                    setShadowLayer(2f, 1f, 1f, 0xFF000000.toInt())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f * sizeScale)
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            if (rows.isNotEmpty()) lp.topMargin = dp(2)
            addView(tv, lp)
            rows[item] = tv
        }
    }

    private fun applyBackground() {
        val alpha = (opacityPct * 255 / 100).coerceIn(0, 255)
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.argb(alpha, 8, 8, 10))
        }
    }

    fun update(m: Metrics) {
        for ((item, tv) in rows) {
            val text = lineFor(item, m)
            if (text == null) {
                tv.visibility = GONE
            } else {
                tv.visibility = VISIBLE
                tv.text = text
                if (item == StatItem.FPS) tv.setTextColor(fpsColor(m.unityFps))
            }
        }
    }

    private fun lineFor(item: StatItem, m: Metrics): String? = when (item) {
        StatItem.FPS -> "${m.unityFps} FPS"
        StatItem.FRAMES -> "frames ${m.totalFrames}"
        StatItem.GPU -> if (m.gpuUsage >= 0) "GPU  ${m.gpuUsage}%" else "GPU  N/A"
        StatItem.GPU_TEMP -> if (m.gpuTemp > 0) "GPU temp  ${m.gpuTemp}°C" else null
        StatItem.CPU -> if (m.cpuUsage >= 0) "CPU  ${m.cpuUsage}%" else "CPU  N/A"
        StatItem.CPU_TEMP -> if (m.cpuTemp > 0) "CPU temp  ${m.cpuTemp}°C" else null
        StatItem.RAM -> if (m.ramInGb)
            "RAM  ${gb(m.usedMb)}/${gb(m.totalMb)} GB (${m.ramPct}%)"
        else
            "RAM  ${m.usedMb}/${m.totalMb} MB (${m.ramPct}%)"
        StatItem.RES -> "Res  ${m.renderWidth}x${m.renderHeight}"
        StatItem.BATTERY -> if (m.battery >= 0) "Battery  ${m.battery}%" else null
        StatItem.UPTIME -> "Uptime  ${formatUptime(m.uptimeMs)}"
        StatItem.VULKAN -> {
            val parts = ArrayList<String>()
            if (m.gpuName.isNotEmpty()) parts.add(m.gpuName)
            if (m.vulkanInfo.isNotEmpty()) parts.add(m.vulkanInfo)
            if (parts.isEmpty()) null else parts.joinToString("\n")
        }
    }

    private fun fpsColor(fps: Int): Int = when {
        fps >= 50 -> 0xFF74E37A.toInt()
        fps >= 30 -> 0xFFE8C65A.toInt()
        else -> 0xFFE87070.toInt()
    }

    private fun gb(mb: Long): String = String.format("%.1f", mb / 1024f)

    private fun formatUptime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val POS_TL = "tl"
        const val POS_TR = "tr"
        const val POS_BL = "bl"
        const val POS_BR = "br"
    }
}
