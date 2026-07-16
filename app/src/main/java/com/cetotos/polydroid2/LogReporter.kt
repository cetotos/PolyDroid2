package com.cetotos.polydroid2

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object LogReporter {
    private const val TAG = "PolyDroid2"
    private const val KEY_LAST_SEND = "last_log_send_time"
    private const val COOLDOWN_MS = 180 * 1000L
    private const val GAME_LOG_TAIL = 5000
    private const val LOGCAT_TAIL = 1500

    private const val REPORT_KEY = "very-secure-key-ok-dont-spam-it-bots-thanks"
    private const val REPORT_BLOB = "HhEGCV5JSkwRGxZOBBcdAwwEQEsOHh0CBBUDBUIGH15NXkBKG0ZeWFhfQEBXS04fQFNaTF1IHxIdH3cJKU5QWysmeBwRUV5rKzNnCCVAPWkAAysUDh4lVEUuQBpSMAIlBhhZHxZyLV5+CFovA1lHHgwCXwc3YDsaPw=="

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    enum class Client(val label: String, val channel: ClientDownloader.Channel, val logName: String) {
        GODOT("Polytoria 2.0", ClientDownloader.Channel.BETA, "godot.log"),
        UNITY("Polytoria 1.0", ClientDownloader.Channel.STABLE, "player.log");
    }

    fun defaultClient(ctx: Context): Client =
        if (RootFs.isPolytoria2(ctx)) Client.GODOT else Client.UNITY

    fun promptAndSend(
        ctx: Context,
        note: String = "",
        onProgress: (String) -> Unit,
        onDone: (success: Boolean, msg: String) -> Unit,
    ) {
        val clients = Client.entries.toList()
        var selected = clients.indexOf(defaultClient(ctx)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Send logs")
            .setSingleChoiceItems(clients.map { it.label }.toTypedArray(), selected) { _, which -> selected = which }
            .setPositiveButton("Send") { _, _ -> send(ctx, clients[selected], note, onProgress, onDone) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun send(
        ctx: Context,
        client: Client,
        note: String,
        onProgress: (String) -> Unit,
        onDone: (success: Boolean, msg: String) -> Unit,
    ) {
        val prefs = ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val since = now - prefs.getLong(KEY_LAST_SEND, 0)
        if (since < COOLDOWN_MS) {
            onDone(false, "Please wait ${((COOLDOWN_MS - since) / 1000)}s before sending again")
            return
        }

        Thread {
            try {
                onProgress("Reading logs…")
                Thread.sleep(500)
                val plain = "text/plain".toMediaTypeOrNull()
                val report = buildReport(ctx, client, note).toByteArray(Charsets.UTF_8)
                val gameLog = readGameLog(ctx, client).toByteArray(Charsets.UTF_8)
                val logcat = readLogcat().toByteArray(Charsets.UTF_8)
                val sessionLog = readSessionLog(ctx).toByteArray(Charsets.UTF_8)

                onProgress("Sending…")
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("content", summary(ctx, client, note))
                    .addFormDataPart("files[0]", "report.txt", report.toRequestBody(plain))
                    .addFormDataPart("files[1]", client.logName, gameLog.toRequestBody(plain))
                    .addFormDataPart("files[2]", "logcat.log", logcat.toRequestBody(plain))
                    .addFormDataPart("files[3]", "session.log", sessionLog.toRequestBody(plain))
                    .build()
                val req = Request.Builder().url(endpoint()).post(body).build()
                http.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        prefs.edit().putLong(KEY_LAST_SEND, System.currentTimeMillis()).apply()
                        onDone(true, "Logs sent!")
                    } else {
                        onDone(false, "Failed to send! HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "failed to send logs: ${e.message}", e)
                onDone(false, "Failed with: ${e.message}")
            }
        }.start()
    }

    private fun buildReport(ctx: Context, client: Client, note: String): String {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val vCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
        val clientVer = ClientDownloader.installedVersion(ctx, client.channel) ?: "not installed"
        val soc = if (Build.VERSION.SDK_INT >= 31) "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else "unknown"
        val mem = ActivityManager.MemoryInfo().also {
            (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        return buildString {
            appendLine("App version: ${pi.versionName} (code $vCode)")
            appendLine("Client: ${client.label} ($clientVer)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("SOC: $soc")
            appendLine("Hardware: ${Build.BOARD} / ${Build.HARDWARE}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("RAM: ${mem.totalMem / (1024 * 1024)} MB")
            appendLine("Vulkan driver: ${SettingsActivity.getVulkanDriver(ctx)}")
            appendLine("Safe mode: ${SettingsActivity.isSafeMode(ctx)}")
            if (note.isNotBlank()) appendLine("Note: $note")
        }
    }

    private fun summary(ctx: Context, client: Client, note: String): String {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val base = "${client.label} | ${pi.versionName} | ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE}"
        val full = if (note.isNotBlank()) "$base | $note" else base
        return if (full.length > 1900) full.take(1900) else full
    }

    private fun readGameLog(ctx: Context, client: Client): String {
        val root = RootFs.rootDir(ctx)
        val file = when (client) {
            Client.UNITY -> File(root, "home/user/.config/unity3d/Polytoria/Polytoria Client/Player.log")
            Client.GODOT -> File(root, "home/user/.local/share/PolytoriaClient/logs")
                .listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
        }
        if (file == null || !file.exists()) return "${client.logName} not found (was ${client.label} run this session?)"

        val lines = file.readLines()
        val start = (lines.size - GAME_LOG_TAIL).coerceAtLeast(0)
        return buildString {
            for (i in start until lines.size) {
                val line = lines[i]
                if (client == Client.UNITY) {
                    if (line.contains("sigaction handler for sig ")) continue
                    if (line.contains("Signal ") && line.contains("si_addr=")) continue
                    if (line.contains("Warning, calling Signal ") && line.contains("SIG_IGN")) continue
                }
                appendLine(line)
            }
        }
    }

    private fun readSessionLog(ctx: Context): String {
        val files = Box64Launcher.sessionLogFiles(ctx)
        if (files.isEmpty()) return "no session log"
        val lines = files.flatMap { f ->
            try { f.readLines() } catch (e: Exception) { listOf("failed to read ${f.name}: ${e.message}") }
        }
        val start = (lines.size - GAME_LOG_TAIL).coerceAtLeast(0)
        return lines.subList(start, lines.size).joinToString("\n")
    }

    private fun readLogcat(): String {
        val main = runLogcat(
            "logcat", "-d", "-v", "time",
            "PolyDroid2:*", "PolyDroid2-Vulkan:*", "PolyDroid2-window:*",
            "Box64:*", "BOX64:*",
            "*:S"
        ) ?: "No matching logcat entries found"
        val crash = runLogcat("logcat", "-d", "-b", "crash", "-v", "time")
        return if (crash == null) main else "$main\n\n----- crash buffer -----\n$crash"
    }

    private fun runLogcat(vararg cmd: String): String? {
        return try {
            val proc = Runtime.getRuntime().exec(cmd)
            val lines = proc.inputStream.bufferedReader().readLines()
            proc.waitFor()
            if (lines.isEmpty()) return null
            val start = (lines.size - LOGCAT_TAIL).coerceAtLeast(0)
            lines.subList(start, lines.size).joinToString("\n")
        } catch (e: Exception) {
            "Failed to read logcat: ${e.message}"
        }
    }

    private fun endpoint(): String {
        val raw = android.util.Base64.decode(REPORT_BLOB, android.util.Base64.DEFAULT)
        val k = REPORT_KEY.toByteArray()
        val out = ByteArray(raw.size)
        for (i in raw.indices) out[i] = (raw[i].toInt() xor k[i % k.size].toInt()).toByte()
        return String(out, Charsets.UTF_8)
    }
}
