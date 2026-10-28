package com.topz.karaoke.data

import java.io.File
import java.util.zip.ZipFile

data class ZipSongEntry(val entryName: String, val fileName: String)

/** Binabasa ang listahan ng mga MIDI/KAR sa loob ng zip nang hindi ini-extract. */
object ZipSongScanner {
    private val extensions = setOf("mid", "midi", "kar")

    fun scan(zip: File): List<ZipSongEntry> =
        try {
            ZipFile(zip).use { zf ->
                val out = ArrayList<ZipSongEntry>()
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    val name = entry.name
                    if (name.startsWith("__MACOSX/")) continue
                    val fileName = name.substringAfterLast('/')
                    if (fileName.startsWith(".")) continue
                    if (fileName.substringAfterLast('.', "").lowercase() !in extensions) continue
                    out.add(ZipSongEntry(name, fileName))
                }
                out
            }
        } catch (_: Exception) {
            emptyList() // sira o hindi mabasang zip
        }
}
