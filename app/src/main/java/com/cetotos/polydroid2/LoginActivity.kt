package com.cetotos.polydroid2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PolyDroid2"
        private const val POLYTORIA_URL = "https://polytoria.com/home"
        private const val STATE_LAUNCHED = "custom_tab_launched"
    }

    private var customTabLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (handlePolytoriaIntent(intent)) return

        customTabLaunched = savedInstanceState?.getBoolean(STATE_LAUNCHED) == true
    }

    override fun onResume() {
        super.onResume()
        if (!customTabLaunched) {
            customTabLaunched = true
            launchCustomTab()
        } else {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LAUNCHED, customTabLaunched)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePolytoriaIntent(intent)
    }

    private fun launchCustomTab() {
        Log.i(TAG, "Launching Custom Tab for $POLYTORIA_URL")
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(POLYTORIA_URL))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(POLYTORIA_URL)))
            } catch (e2: Exception) {
                Log.e(TAG, "No browser available!", e2)
                finish()
            }
        }
    }

    private fun handlePolytoriaIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (uri.scheme != "polytoria") return false
        handlePolytoriaUri(uri)
        return true
    }

    private fun handlePolytoriaUri(uri: Uri) {
        // Log.i(TAG, "Received Polytoria URI: $uri")

        val segments = mutableListOf<String>()
        uri.host?.let { segments.add(it) }
        uri.pathSegments?.let { segments.addAll(it) }

        if (segments.size < 2) {
            Log.e(TAG, "Invalid URL! $uri")
            return
        }

        val type = segments[0]
        val token = segments[1]
        val map = if (segments.size > 2) segments[2] else null

        Log.i(TAG, "Type=$type, token=${token.take(8)}..., map=$map")

        val execArgs = if (type == "test") { // i havent tested creator at all, i doubt it works, but maybe it works? i dont know
            "-solo ${map ?: ""}"
        } else {
            "-network client -token $token -no-focus-pause"
        }

        val gameIntent = Intent(this, GameActivity::class.java).apply {
            putExtra("exec_args", execArgs)
        }
        startActivity(gameIntent)
        finish()
    }
}
