package com.topz.karaoke.storage

import java.io.File

/** Mga uri ng file na hinahanap ng app, at kung saang subfolder sila nakalagay (default). */
enum class FileKind(val folder: String, val extensions: Set<String>) {
    SONG("songs", setOf("mid", "midi", "kar", "zip")), // puwedeng zip na may lamang MIDI
    SOUNDFONT("soundfonts", setOf("sf2")),
    VIDEO("videos", setOf("mp4", "mkv", "webm", "3gp"))
}

/** sizeBytes ay 0 na lang (hindi na kinukuha) para bumilis ang pag-scan ng libo-libong file. */
data class StoredFile(val path: String, val name: String, val sizeBytes: Long)

/**
 * Abstraksyon sa storage. Ngayon ay direktang file path (All Files Access);
 * kung kailangan ng Play Store version, gagawa lang ng SAF implementation nito.
 */
interface StorageProvider {
    fun hasAccess(): Boolean

    /** Root ng bawat volume (internal at SD card), hal. /storage/1234-ABCD */
    fun volumeRoots(): List<File>

    /** Ang mga OpenKaraoke/ folder sa bawat volume. */
    fun appRoots(): List<File>

    /** Gumagawa ng OpenKaraoke/{songs,soundfonts,videos} kung wala pa. */
    fun ensureFolders()

    /** Hinahanap ang uri ng file sa default na mga folder (OpenKaraoke/<kind> sa bawat volume). */
    suspend fun list(kind: FileKind, onProgress: (Int) -> Unit = {}): List<StoredFile>

    /** Hinahanap ang uri ng file sa isang folder na pinili ng user (kasama ang mga subfolder). */
    suspend fun listIn(dir: File, kind: FileKind, onProgress: (Int) -> Unit = {}): List<StoredFile>
}
