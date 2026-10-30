package com.topz.karaoke.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongPrefsTest {
    @Test
    fun roundTrip() {
        val prefs = SongPrefs(transpose = -3, tempoPercent = 90, muted = setOf(0, 4))
        assertEquals(prefs, SongPrefs.decode(prefs.encode()))
    }

    @Test
    fun defaultsRoundTrip() {
        assertEquals(SongPrefs(), SongPrefs.decode(SongPrefs().encode()))
    }

    @Test
    fun garbageIsIgnored() {
        assertNull(SongPrefs.decode(null))
        assertNull(SongPrefs.decode("hindi ito tama"))
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val p = SongPrefs.decode("99;500;3,99,x")!!
        assertEquals(12, p.transpose)
        assertEquals(150, p.tempoPercent)
        assertEquals(setOf(3), p.muted)
    }
}
