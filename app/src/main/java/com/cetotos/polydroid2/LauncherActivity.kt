package com.cetotos.polydroid2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.concurrent.thread

class LauncherActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PolyDroid2"
        private const val POLYTORIA_URL = "https://polytoria.com/home"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        val root = findViewById<android.view.View>(R.id.launcher_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                bars.left + 32.dpToPx(),
                bars.top + 32.dpToPx(),
                bars.right + 32.dpToPx(),
                bars.bottom + 32.dpToPx()
            )
            insets
        }

        findViewById<android.view.View>(R.id.btn_website).setOnClickListener {
            polyWebsite()
        }

        findViewById<android.view.View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (RootFs.needsExtraction(this)) {
            val detailBar = ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
            ).apply {
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
                addView(detailBar)
            }
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Extracting files...")
                .setView(dialogView)
                .setCancelable(false)
                .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            thread {
                RootFs.extractAll(this) { detailPct, _, _, stageLabel ->
                    runOnUiThread {
                        dialog.setTitle(stageLabel)
                        detailBar.progress = detailPct
                    }
                }
                runOnUiThread { dialog.dismiss() }
            }
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    private fun polyWebsite() {
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
            }
        }
    }
}
