package com.topz.karaoke.midi

import com.topz.karaoke.midi.MidiTestBuilder.end
import com.topz.karaoke.midi.MidiTestBuilder.smf
import com.topz.karaoke.midi.MidiTestBuilder.tempo
import com.topz.karaoke.midi.MidiTestBuilder.text
import com.topz.karaoke.midi.MidiTestBuilder.track
import com.topz.karaoke.midi.MidiTestBuilder.trackName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsExtractorTest {

    private fun extract(vararg tracks: ByteArray): Lyrics =
        LyricsExtractor.extract(MidiParser.parse(smf(*tracks)))

    private fun Lyrics.lineTexts() = lines.map { it.text }

    @Test
    fun karTextEventsWithMarkers() {
        val karTrack = track(
            trackName("Words"),
            text(0, "@KMIDI KARAOKE FILE"), text(0, "@LENGL"),
            text(0, "@TTest Song"), text(0, "@TTest Artist"),
            text(0, "/Hel"), text(480, "lo "), text(480, "wor"), text(480, "ld"),
            text(480, "\\Next "), text(480, "line"), text(480, "/Third"),
            end()
        )
        val lyrics = extract(track(tempo(0, 500000), end()), karTrack)

        assertEquals(Lyrics.Source.TEXT_EVENTS, lyrics.source)
        assertEquals(listOf("Test Song", "Test Artist"), lyrics.info)
        assertEquals(listOf("Hello world", "Next line", "Third"), lyrics.lineTexts())
        assertEquals(500L, lyrics.lines[0].words[1].timeMs)
        assertEquals(2000L, lyrics.lines[1].words[0].timeMs)
    }

    @Test
    fun lyricEventsArePreferredOverText() {
        val lyrics = extract(
            track(text(0, "Text-lang"), text(0, "Hi ", 0x05), text(480, "there", 0x05), end())
        )
        assertEquals(Lyrics.Source.LYRIC_EVENTS, lyrics.source)
        assertEquals(listOf("Hi there"), lyrics.lineTexts())
    }

    @Test
    fun newlineCharactersSplitLines() {
        val lyrics = extract(
            track(text(0, "One\r", 0x05), text(10, "Two\n", 0x05), text(20, "Three", 0x05), end())
        )
        assertEquals(listOf("One", "Two", "Three"), lyrics.lineTexts())
    }

    @Test
    fun longLinesAreWrappedBetweenWords() {
        val chunks = (0 until 40).map { text(it * 10, "word ") }.toTypedArray()
        val lyrics = extract(track(*chunks, end()))
        assertTrue(lyrics.lines.size > 1)
        assertTrue(lyrics.lineTexts().all { it.length <= LyricsExtractor.DEFAULT_MAX_LINE_CHARS })
    }

    @Test
    fun noTextMeansNoLyrics() {
        val lyrics = extract(track(tempo(0, 500000), end()))
        assertEquals(Lyrics.Source.NONE, lyrics.source)
        assertTrue(lyrics.lines.isEmpty())
    }

    @Test
    fun lineIndexAtFollowsTime() {
        val lyrics = extract(
            track(
                tempo(0, 500000),
                text(0, "A", 0x05), text(960, "\\B", 0x05), text(960, "\\C", 0x05),
                end()
            )
        )
        assertEquals(-1, lyrics.lineIndexAt(-1))
        assertEquals(0, lyrics.lineIndexAt(0))
        assertEquals(0, lyrics.lineIndexAt(999))
        assertEquals(1, lyrics.lineIndexAt(1000))
        assertEquals(2, lyrics.lineIndexAt(5000))
    }
}
