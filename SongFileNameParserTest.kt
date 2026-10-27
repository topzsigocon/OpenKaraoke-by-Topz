package com.topz.karaoke.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongFileNameParserTest {

    @Test fun numberTitleArtist() {
        val r = SongFileNameParser.parse("00123 - Bakit Ba - Some Artist.mid")
        assertEquals(123, r.number)
        assertEquals("Bakit Ba", r.title)
        assertEquals("Some Artist", r.artist)
    }

    @Test fun underscoreSeparator() {
        val r = SongFileNameParser.parse("00045_Magandang_Umaga.kar")
        assertEquals(45, r.number)
        assertEquals("Magandang Umaga", r.title)
        assertNull(r.artist)
    }

    @Test fun numberWithSpace() {
        val r = SongFileNameParser.parse("123 Pamagat.mid")
        assertEquals(123, r.number)
        assertEquals("Pamagat", r.title)
    }

    @Test fun numberOnly() {
        val r = SongFileNameParser.parse("123.kar")
        assertEquals(123, r.number)
        assertEquals("123", r.title)
        assertNull(r.artist)
    }

    @Test fun noNumber() {
        val r = SongFileNameParser.parse("Walang Numero.mid")
        assertNull(r.number)
        assertEquals("Walang Numero", r.title)
    }

    @Test fun artistWithDash() {
        val r = SongFileNameParser.parse("5 - A - B - C.mid")
        assertEquals(5, r.number)
        assertEquals("A", r.title)
        assertEquals("B - C", r.artist)
    }
}
