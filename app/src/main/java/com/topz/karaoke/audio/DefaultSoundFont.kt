package com.topz.karaoke.audio

import android.content.Context
import java.io.File

/**
 * Ang default SoundFont ay nasa assets/default.sf2 (dina-download ng Gradle sa build).
 * Kinokopya ito sa internal storage nang isang beses, dahil file path ang kailangan ng native synth.
 */
object DefaultSoundFont {
    private const val ASSET = "default.sf2"

    /** Tawagin sa background thread (kinokopya ang ~30 MB sa unang pagkakataon). */
    fun path(context: Context): String? {
        val out = File(context.filesDir, ASSET)
        if (out.isFile && out.length() > 100_000L) return out.absolutePath

        val hasAsset = try {
            context.assets.list("")?.contains(ASSET) == true
        } catch (_: Exception) {
            false
        }
        if (!hasAsset) return null

        return try {
            val tmp = File(context.filesDir, "$ASSET.part")
            context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            out.delete()
            if (tmp.renameTo(out)) out.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }
}
