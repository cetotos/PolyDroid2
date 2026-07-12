package com.cetotos.polydroid2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PolyDroid2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!handlePolytoriaIntent(intent)) {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePolytoriaIntent(intent)
    }

    private fun handlePolytoriaIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (uri.scheme != "polytoria") return false
        handlePolytoriaUri(uri)
        return true
    }

    private fun handlePolytoriaUri(uri: Uri) {
        val segments = mutableListOf<String>()
        uri.host?.let { segments.add(it) }
        uri.pathSegments?.let { segments.addAll(it) }

        if (segments.size < 2) {
            Log.e(TAG, "Invalid URL! $uri")
            finish()
            return
        }

        val type = segments[0]
        val token = segments[1]
        val map = if (segments.size > 2) segments[2] else null

        Log.i(TAG, "Type=$type, token=${token.take(8)}..., map=$map")

        val execArgs = if (type == "test") {
            "-solo ${map ?: ""}"
        } else {
            "-network client -token $token -no-focus-pause"
        }

        val channel = ClientDownloader.Channel.fromDeepLinkType(type)
        prepareClientThenLaunch(token, channel, execArgs)
    }

    private fun prepareClientThenLaunch(token: String, channel: ClientDownloader.Channel, execArgs: String) {
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 48, 64, 48)
            addView(bar)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Preparing ${channel.label} client…")
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        thread {
            try {
                ClientDownloader.prepare(this, token, channel) { pct, label ->
                    runOnUiThread {
                        dialog.setTitle(label)
                        bar.progress = pct
                    }
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    dialog.dismiss()
                    startActivity(Intent(this, GameActivity::class.java).apply {
                        putExtra("exec_args", execArgs)
                    })
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "client prepare failed: ${e.message}", e)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    dialog.dismiss()
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Couldn't get the ${channel.label} client")
                        .setMessage("${e.message}\n\nPress Play again to retry.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setOnDismissListener { finish() }
                        .show()
                }
            }
        }
    }
}
