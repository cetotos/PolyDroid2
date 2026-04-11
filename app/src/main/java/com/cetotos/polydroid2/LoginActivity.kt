package com.cetotos.polydroid2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

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
