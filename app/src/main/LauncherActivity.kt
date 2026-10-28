package com.topz.karaoke

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

/**
 * DIAGNOSTIC SCREEN (pansamantala). Hindi na diretsong binubuksan ang MainActivity.
 * Ipinapakita muna ang device info, ang naka-save na crash, at ang logcat ng app mismo.
 * Pindutin ang "Buksan ang app" para subukan ang MainActivity. Kung mag-crash,
 * buksan ulit ang app na ito at kopyahin ang lalabas na log.
 */
class LauncherActivity : Activity() {

    private lateinit var logView: TextView
    private var fullReport: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "OpenKaraoke diagnostic\n" +
                "1) Pindutin ang \"Buksan ang app\".\n" +
                "2) Kung mag-crash, buksan ulit ito at kopyahin ang log."
            textSize = 15f
        }
        val openBtn = Button(this).apply {
            text = "Buksan ang app"
            setOnClickListener {
                CrashReporter.clear(this@LauncherActivity)
                startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                finish()
            }
        }
        val copyBtn = Button(this).apply {
            text = "Kopyahin ang buong log"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("openkaraoke-log", fullReport))
                Toast.makeText(this@LauncherActivity, "Nakopya na", Toast.LENGTH_SHORT).show()
            }
        }
        logView = TextView(this).apply {
            text = "Binabasa ang log..."
            textSize = 10f
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(openBtn)
            addView(copyBtn)
            addView(logView)
        }
        setContentView(ScrollView(this).apply { addView(column) })

        loadReport()
    }

    private fun loadReport() {
        thread {
            val report = buildString {
                append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                append("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}\n\n")

                append("=== Naka-save na crash ===\n")
                append(CrashReporter.read(this@LauncherActivity) ?: "(wala)")
                append("\n\n=== logcat (crash buffer) ===\n")
                append(runLogcat("-b", "crash", "-d", "-t", "200"))
                append("\n\n=== logcat (main, huling 400 linya) ===\n")
                append(runLogcat("-d", "-v", "time", "-t", "400"))
            }
            fullReport = report
            runOnUiThread { logView.text = report }
        }
    }

    private fun runLogcat(vararg args: String): String =
        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", *args))
            val out = process.inputStream.bufferedReader().readText()
            process.destroy()
            out.ifBlank { "(walang laman)" }
        } catch (t: Throwable) {
            "(hindi mabasa ang logcat: ${t.message})"
        }
}
