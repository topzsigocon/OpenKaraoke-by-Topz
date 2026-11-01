package com.topz.karaoke.data

/** Mga simpleng operasyon sa listahan ng kanta para sa History at Favorites. */
object SongLists {
    /** Ilagay sa unahan (pinakabago); tanggalin ang kopya nito kung nandoon na; limitahan ang haba. */
    fun pushFront(list: List<Song>, song: Song, max: Int): List<Song> =
        (listOf(song) + list.filter { it.key != song.key }).take(max)

    /** Kung nasa listahan na, alisin; kung wala pa, ilagay sa unahan. */
    fun toggle(list: List<Song>, song: Song): List<Song> =
        if (list.any { it.key == song.key }) list.filter { it.key != song.key } else listOf(song) + list
}
