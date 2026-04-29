package com.cetotos.polydroid2

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object UpdateCheck {
    private const val TAG = "PolyDroid2"

    const val RELEASES_URL = "https://github.com/cetotos/PolyDroid2/releases"
    private const val LATEST_API = "https://api.github.com/repos/cetotos/PolyDroid2/releases/latest"

    data class Result(val latestTag: String, val htmlUrl: String, val outdated: Boolean)

    fun checkAsync(currentVersion: String, onResult: (Result?) -> Unit) {
        Thread {
            onResult(checkSync(currentVersion))
        }.start()
    }

    private fun checkSync(currentVersion: String): Result? {
        return try {
            val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PolyDroid2")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "update check HTTP $code")
                    return null
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", RELEASES_URL)
                if (tag.isEmpty()) return null
                Result(tag, htmlUrl, isOutdated(currentVersion, tag))
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "update check failed: ${e.message}")
            null
        }
    }

    private fun isOutdated(current: String, latest: String): Boolean {
        val a = parseVersion(current)
        val b = parseVersion(latest)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }

    private fun parseVersion(s: String): List<Int> =
        s.trimStart('v', 'V')
            .split('.', '-', '_')
            .map { tok ->
                val digits = tok.takeWhile { it.isDigit() }
                if (digits.isEmpty()) 0 else digits.toInt()
            }
}
