package com.topz.karaoke.data

/** Pag-uuri ng listahan ng kanta. Isang beses lang kinukuwenta ang lowercase ng bawat kanta (mabilis sa 47,000). */
object SongSorting {
    fun sort(list: List<Song>, order: SongSort): List<Song> = when (order) {
        SongSort.NUMBER -> list.map { it to it.title.lowercase() }
            .sortedWith(compareBy<Pair<Song, String>>({ it.first.number ?: Int.MAX_VALUE }, { it.second }))
            .map { it.first }
        SongSort.TITLE -> list.map { it to it.title.lowercase() }
            .sortedBy { it.second }
            .map { it.first }
        SongSort.ARTIST -> list.map { Triple(it, it.artist?.lowercase() ?: "\uFFFF", it.title.lowercase()) }
            .sortedWith(compareBy<Triple<Song, String, String>>({ it.second }, { it.third }))
            .map { it.first }
    }
}
