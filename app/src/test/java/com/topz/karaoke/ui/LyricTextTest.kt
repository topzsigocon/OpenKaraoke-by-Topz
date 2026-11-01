package com.topz.karaoke.ui

import com.topz.karaoke.data.LyricsCase
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricTextTest {
    @Test
    fun originalIsUnchanged() {
        assertEquals("HaVe You", LyricText.format("HaVe You", LyricsCase.ORIGINAL, true))
    }

    @Test
    fun uppercase() {
        assertEquals("HAVE YOU", LyricText.format("Have you", LyricsCase.UPPERCASE, true))
    }

    @Test
    fun titleCaseCapitalizesOnlyWordStarts() {
        assertEquals("Have You Ever", LyricText.format("HAVE YOU EVER", LyricsCase.TITLE, true))
    }

    @Test
    fun titleCaseKeepsSyllablesOfOneWordTogether() {
        // "MEL" + "LOW" ay iisang salita: ang ikalawang piraso ay hindi simula ng salita
        val first = LyricText.format("MEL", LyricsCase.TITLE, true)
        val second = LyricText.format("LOW ", LyricsCase.TITLE, false)
        assertEquals("Mellow ", first + second)
    }

    @Test
    fun titleCaseAfterSpaceInsideAPiece() {
        assertEquals(" World", LyricText.format(" WORLD", LyricsCase.TITLE, false))
    }
}
