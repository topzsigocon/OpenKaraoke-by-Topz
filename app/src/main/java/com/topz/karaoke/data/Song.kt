package com.topz.karaoke.data

/**
 * Isang kanta. Kung may [entry], ang kanta ay nasa loob ng zip na nasa [path];
 * kung wala, ang [path] mismo ang MIDI file.
 */
data class Song(
    val number: Int?,
    val title: String,
    val artist: String?,
    val path: String,
    val entry: String? = null
) {
    /** Natatanging susi para sa listahan. */
    val key: String get() = if (entry == null) path else "$path!$entry"

    /** Naka-lowercase na text para sa mabilis na paghahanap (kinukuwenta nang isang beses). */
    val searchKey: String = listOfNotNull(number?.toString(), title, artist).joinToString(" ").lowercase()
}
