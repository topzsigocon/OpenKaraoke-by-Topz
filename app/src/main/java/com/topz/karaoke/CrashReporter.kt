package com.topz.karaoke

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Sine-save ang crash sa file para maipakita ng LauncherActivity sa susunod na bukas. */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val text = buildString {
                    append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                    append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                    append("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}\n")
                    append("Thread: ${thread.name}\n\n")
                    append(sw.toString())
                }
                File(app.filesDir, FILE_NAME).writeText(text)
            } catch (_: Throwable) {
                // Huwag hayaang mag-crash ulit ang crash handler.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? {
        val f = File(context.filesDir, FILE_NAME)
        return if (f.exists()) runCatching { f.readText() }.getOrNull() else null
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
