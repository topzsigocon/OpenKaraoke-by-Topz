package com.topz.karaoke.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SongSortingTest {
    private val a = Song(30, "banana", "Zed", "/a.mid")
    private val b = Song(5, "Apple", null, "/b.mid")
    private val c = Song(null, "cherry", "Abe", "/c.mid")
    private val d = Song(5, "Aardvark", "Abe", "/d.mid")

    private fun titles(order: SongSort) = SongSorting.sort(listOf(a, b, c, d), order).map { it.title }

    @Test
    fun byNumberThenTitleWithoutNumberLast() {
        assertEquals(listOf("Aardvark", "Apple", "banana", "cherry"), titles(SongSort.NUMBER))
    }

    @Test
    fun byTitleIgnoresCase() {
        assertEquals(listOf("Aardvark", "Apple", "banana", "cherry"), titles(SongSort.TITLE))
    }

    @Test
    fun byArtistThenTitleWithoutArtistLast() {
        assertEquals(listOf("Aardvark", "cherry", "banana", "Apple"), titles(SongSort.ARTIST))
    }
}
