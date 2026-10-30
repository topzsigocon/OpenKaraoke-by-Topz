package com.topz.karaoke.data

/** Mga setting na ise-save kada kanta: key, tempo, at mga naka-mute na channel. */
data class SongPrefs(
    val transpose: Int = 0,
    val tempoPercent: Int = 100,
    val muted: Set<Int> = emptySet()
) {
    fun encode(): String = "$transpose;$tempoPercent;${muted.sorted().joinToString(",")}"

    companion object {
        fun decode(text: String?): SongPrefs? {
            if (text == null) return null
            val parts = text.split(';')
            if (parts.size < 2) return null
            val transpose = parts[0].toIntOrNull() ?: return null
            val tempo = parts[1].toIntOrNull() ?: return null
            val muted = parts.getOrNull(2)
                ?.split(',')
                ?.mapNotNull { it.toIntOrNull() }
                ?.filter { it in 0..15 }
                ?.toSet()
                ?: emptySet()
            return SongPrefs(transpose.coerceIn(-12, 12), tempo.coerceIn(50, 150), muted)
        }
    }
}

/** Taguan ng mga setting kada kanta (ang susi ay Song.key). */
interface SongPrefsStore {
    fun load(songKey: String): SongPrefs?
    fun save(songKey: String, prefs: SongPrefs)
}
