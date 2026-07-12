package com.cetotos.polydroid2

import java.io.File

class SystemStats {

    private val ws = Regex("\\s+")
    private val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    private val clkTck = try {
        android.system.Os.sysconf(android.system.OsConstants._SC_CLK_TCK).coerceAtLeast(1)
    } catch (_: Exception) { 100L }

    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L

    private var gamePid = -1
    private var lastProcTicks = 0L
    private var lastProcNanos = 0L

    fun setGamePid(pid: Int) {
        if (pid != gamePid) { gamePid = pid; lastProcTicks = 0L; lastProcNanos = 0L }
    }

    fun cpuUsage(): Int = procStatCpu() ?: processCpu() ?: -1

    private fun procStatCpu(): Int? {
        return try {
            val line = File("/proc/stat").bufferedReader().use { it.readLine() } ?: return null
            if (!line.startsWith("cpu ")) return null
            val p = line.trim().split(ws).drop(1).mapNotNull { it.toLongOrNull() }
            if (p.size < 4) return null
            val idle = p[3] + p.getOrElse(4) { 0L }
            val total = p.sum()
            val di = idle - lastCpuIdle
            val dt = total - lastCpuTotal
            lastCpuIdle = idle
            lastCpuTotal = total
            if (dt <= 0L) null else (((dt - di) * 100L) / dt).toInt().coerceIn(0, 100)
        } catch (_: Exception) { null }
    }

    private fun processCpu(): Int? {
        if (gamePid <= 0) return null
        return try {
            val stat = File("/proc/$gamePid/stat").readText()
            val fields = stat.substring(stat.lastIndexOf(") ") + 2).trim().split(ws)
            val ticks = fields[11].toLong() + fields[12].toLong()
            val now = System.nanoTime()
            val dTicks = ticks - lastProcTicks
            val dNanos = now - lastProcNanos
            val first = lastProcNanos == 0L
            lastProcTicks = ticks
            lastProcNanos = now
            if (first || dNanos <= 0L) return null
            val secs = dNanos / 1_000_000_000.0
            (((dTicks / clkTck.toDouble()) / (secs * cores)) * 100.0).toInt().coerceIn(0, 100)
        } catch (_: Exception) { null }
    }

    private val gpuCandidates = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage" to GPU_PERCENT,
        "/sys/class/kgsl/kgsl-3d0/gpubusy" to GPU_BUSY_TOTAL,
        "/sys/kernel/ged/hal/gpu_utilization" to GPU_FIRST_INT,
        "/sys/kernel/gpu/gpu_busy" to GPU_PERCENT,
        "/sys/devices/platform/gpusysfs/gpu_busy" to GPU_PERCENT,
        "/proc/mali/utilization" to GPU_FIRST_INT,
        "/sys/class/misc/mali0/device/utilization" to GPU_FIRST_INT,
        "/sys/devices/platform/mali/utilization" to GPU_FIRST_INT,
    )
    private var gpuPath: Pair<String, Int>? = null

    fun gpuUsage(): Int {
        gpuPath?.let { readGpu(it.first, it.second)?.let { v -> return v } }
        for (cand in gpuCandidates) {
            val v = readGpu(cand.first, cand.second)
            if (v != null) { gpuPath = cand; return v }
        }
        return -1
    }

    private fun readGpu(path: String, mode: Int): Int? {
        return try {
            val t = File(path).readText().trim()
            when (mode) {
                GPU_BUSY_TOTAL -> {
                    val pp = t.split(ws)
                    if (pp.size < 2) return null
                    val busy = pp[0].toLong()
                    val total = pp[1].toLong()
                    if (total <= 0L) 0 else ((busy * 100L) / total).toInt().coerceIn(0, 100)
                }
                GPU_PERCENT -> t.replace("%", "").trim().split(ws)[0].toDouble().toInt().coerceIn(0, 100)
                else -> t.split(ws)[0].takeWhile { it.isDigit() }.toIntOrNull()?.coerceIn(0, 100)
            }
        } catch (_: Exception) { null }
    }

    private var scanned = false
    private var cpuZone: File? = null
    private var gpuZone: File? = null
    private var fallbackZones: List<File> = emptyList()

    private fun scanZones() {
        if (scanned) return
        scanned = true
        val cpuKeys = listOf("cpu", "kryo", "apc", "silver", "gold", "cluster", "little", "big", "mtktscpu", "tsens", "soc", "ap_", "core")
        val gpuKeys = listOf("gpu", "mali", "kgsl", "adreno", "powervr", "g3d")
        val skipKeys = listOf("batt", "charg", "usb", "skin", "case", "quiet", "pa_", "pa-", "modem", "wifi", "wlan", "cam", "display", "lcd", "board", "conn", "5g", "sub")
        val fallback = ArrayList<File>()
        for (i in 0..49) {
            val dir = File("/sys/class/thermal/thermal_zone$i")
            if (!dir.isDirectory) continue
            val tempF = File(dir, "temp")
            val type = try { File(dir, "type").readText().trim().lowercase() } catch (_: Exception) { continue }
            if (readTemp(tempF) == null) continue
            if (gpuZone == null && gpuKeys.any { type.contains(it) }) gpuZone = tempF
            if (cpuZone == null && cpuKeys.any { type.contains(it) }) cpuZone = tempF
            if (skipKeys.none { type.contains(it) }) fallback.add(tempF)
        }
        fallbackZones = fallback
    }

    fun cpuTemp(): Int {
        scanZones()
        cpuZone?.let { readTemp(it)?.let { c -> return c } }
        return hottest() ?: -1
    }

    fun gpuTemp(): Int {
        scanZones()
        gpuZone?.let { readTemp(it)?.let { c -> return c } }
        return hottest() ?: -1
    }

    private fun hottest(): Int? {
        var max: Int? = null
        for (f in fallbackZones) {
            val t = readTemp(f) ?: continue
            if (max == null || t > max) max = t
        }
        return max
    }

    private fun readTemp(f: File): Int? {
        return try {
            val raw = f.readText().trim().toLong()
            val c = when {
                raw > 1000 -> (raw / 1000).toInt()
                raw in 200..999 -> (raw / 10).toInt()
                else -> raw.toInt()
            }
            if (c in 1..150) c else null
        } catch (_: Exception) { null }
    }

    companion object {
        private const val GPU_PERCENT = 0
        private const val GPU_BUSY_TOTAL = 1
        private const val GPU_FIRST_INT = 2
    }
}
