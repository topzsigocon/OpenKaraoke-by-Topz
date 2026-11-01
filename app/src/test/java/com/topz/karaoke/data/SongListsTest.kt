package com.topz.karaoke.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SongListsTest {
    private fun song(n: Int) = Song(n, "Kanta $n", null, "/musika/$n.mid")

    @Test
    fun pushFrontPutsNewestFirstAndRemovesDuplicates() {
        val list = listOf(song(1), song(2), song(3))
        assertEquals(listOf(2, 1, 3), SongLists.pushFront(list, song(2), 10).map { it.number })
        assertEquals(listOf(9, 1, 2, 3), SongLists.pushFront(list, song(9), 10).map { it.number })
    }

    @Test
    fun pushFrontLimitsLength() {
        val list = listOf(song(1), song(2), song(3))
        assertEquals(listOf(9, 1), SongLists.pushFront(list, song(9), 2).map { it.number })
    }

    @Test
    fun toggleAddsThenRemoves() {
        val added = SongLists.toggle(listOf(song(1)), song(2))
        assertEquals(listOf(2, 1), added.map { it.number })
        assertEquals(listOf(1), SongLists.toggle(added, song(2)).map { it.number })
    }
}
