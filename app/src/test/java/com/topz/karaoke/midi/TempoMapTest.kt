package com.topz.karaoke.midi

import org.junit.Assert.assertEquals
import org.junit.Test

class TempoMapTest {

    @Test
    fun defaultTempoIs120Bpm() {
        val tm = TempoMap(480, emptyList())
        assertEquals(0L, tm.tickToMs(0))
        assertEquals(500L, tm.tickToMs(480))
    }

    @Test
    fun tempoChangeMidSong() {
        val tm = TempoMap(480, listOf(TempoChange(480, 1000000)))
        assertEquals(500L, tm.tickToMs(480))
        assertEquals(1500L, tm.tickToMs(960))
    }

    @Test
    fun tempoAtStartThenChange() {
        val tm = TempoMap(480, listOf(TempoChange(0, 250000), TempoChange(480, 500000)))
        assertEquals(250L, tm.tickToMs(480))
        assertEquals(750L, tm.tickToMs(960))
    }

    @Test
    fun firstTempoAfterStartUsesDefaultBefore() {
        val tm = TempoMap(480, listOf(TempoChange(960, 250000)))
        assertEquals(1000L, tm.tickToMs(960))
        assertEquals(1250L, tm.tickToMs(1440))
    }

    @Test
    fun smpteDivision() {
        // 0xE7 = -25 fps, 40 ticks bawat frame = 1000 ticks bawat segundo
        val tm = TempoMap(0xE728, emptyList())
        assertEquals(1000L, tm.tickToMs(1000))
        assertEquals(40L, tm.tickToMs(40))
    }
}
