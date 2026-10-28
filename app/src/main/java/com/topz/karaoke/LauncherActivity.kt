package com.topz.karaoke

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Unang bubukas. Kung may naka-save na crash mula sa nakaraang takbo, ipinapakita ito
 * para makopya. Kung wala, diretso sa MainActivity.
 */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crash = CrashReporter.read(this)
        if (crash == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        showCrash(crash)
    }

    private fun showCrash(crash: String) {
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "Nag-crash ang app sa nakaraang bukas.\nKopyahin ang error at ipadala kay Claude."
            textSize = 16f
        }
        val copy = Button(this).apply {
            text = "Kopyahin ang error"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", crash))
                Toast.makeText(this@LauncherActivity, "Nakopya na", Toast.LENGTH_SHORT).show()
            }
        }
        val retry = Button(this).apply {
            text = "Subukan ulit buksan"
            setOnClickListener {
                CrashReporter.clear(this@LauncherActivity)
                startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                finish()
            }
        }
        val trace = TextView(this).apply {
            text = crash
            textSize = 11f
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 3, pad, pad)
            addView(title)
            addView(copy)
            addView(retry)
            addView(trace)
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }
}
