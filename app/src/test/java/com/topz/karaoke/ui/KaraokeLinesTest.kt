package com.topz.karaoke.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class KaraokeLinesTest {
    @Test
    fun beforeTheSongStartsShowsFirstTwoLines() {
        assertEquals(TwoLinePlan(0, 1), KaraokeLines.twoLine(-1, 5))
    }

    @Test
    fun linesSwapOneAtATime() {
        assertEquals(TwoLinePlan(0, 1), KaraokeLines.twoLine(0, 5))
        assertEquals(TwoLinePlan(2, 1), KaraokeLines.twoLine(1, 5)) // natapos ang itaas, napalitan ng linya 2
        assertEquals(TwoLinePlan(2, 3), KaraokeLines.twoLine(2, 5)) // natapos ang ibaba, napalitan ng linya 3
        assertEquals(TwoLinePlan(4, 3), KaraokeLines.twoLine(3, 5))
    }

    @Test
    fun lastLinesHaveNoNext() {
        assertEquals(TwoLinePlan(4, null), KaraokeLines.twoLine(4, 5))
        assertEquals(TwoLinePlan(0, null), KaraokeLines.twoLine(0, 1))
        assertEquals(TwoLinePlan(null, 1), KaraokeLines.twoLine(1, 2))
    }
}
