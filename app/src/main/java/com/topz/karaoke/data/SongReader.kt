package com.topz.karaoke.data

import java.io.File
import java.io.FileNotFoundException
import java.util.zip.ZipFile

/**
 * Kinukuha ang laman (bytes) ng isang kanta, mula sa file o sa loob ng zip.
 * Nire-reuse ang mga nakabukas na zip, dahil mabagal buksan ang zip na may libo-libong entry.
 */
object SongReader {
    private val openZips = HashMap<String, ZipFile>()

    fun read(song: Song): ByteArray {
        val entry = song.entry ?: return File(song.path).readBytes()
        val zip = zipFor(song.path)
        val ze = zip.getEntry(entry) ?: throw FileNotFoundException("$entry sa ${song.path}")
        return zip.getInputStream(ze).use { it.readBytes() }
    }

    @Synchronized
    private fun zipFor(path: String): ZipFile =
        openZips.getOrPut(path) { ZipFile(File(path)) }

    @Synchronized
    fun closeAll() {
        for (z in openZips.values) {
            try { z.close() } catch (_: Exception) { }
        }
        openZips.clear()
    }
}
