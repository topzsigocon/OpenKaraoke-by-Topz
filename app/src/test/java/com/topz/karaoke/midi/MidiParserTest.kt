package com.topz.karaoke.midi

import com.topz.karaoke.midi.MidiTestBuilder.end
import com.topz.karaoke.midi.MidiTestBuilder.smf
import com.topz.karaoke.midi.MidiTestBuilder.text
import com.topz.karaoke.midi.MidiTestBuilder.track
import com.topz.karaoke.midi.MidiTestBuilder.trackName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiParserTest {

    @Test
    fun parsesHeaderAndTracks() {
        val m = MidiParser.parse(smf(track(trackName("Words"), end()), track(end())))
        assertEquals(1, m.format)
        assertEquals(480, m.division)
        assertEquals(2, m.tracks.size)
        assertEquals("Words", m.tracks[0].name)
    }

    @Test
    fun runningStatusAndSysExAreHandled() {
        val sysex = byteArrayOf(0x00, 0xF0.toByte(), 0x03, 1, 2, 0xF7.toByte())
        val notes = byteArrayOf(
            0x00, 0x90.toByte(), 60, 100, // note on
            0x10, 62, 100,               // running status: note on
            0x10, 0x80.toByte(), 60, 0    // note off
        )
        val m = MidiParser.parse(smf(track(sysex, notes, end())))
        val events = m.tracks[0].events
        assertEquals(3, events.size)
        assertEquals(0x90, events[1].status)
        assertEquals(62, events[1].data1)
        assertEquals(16L, events[1].tick)
        assertEquals(32L, events[2].tick)
    }

    @Test
    fun readsTempoChanges() {
        val m = MidiParser.parse(
            smf(track(MidiTestBuilder.tempo(0, 500000), MidiTestBuilder.tempo(480, 1000000), end()))
        )
        assertEquals(listOf(TempoChange(0, 500000), TempoChange(480, 1000000)), m.tempos)
    }

    @Test
    fun truncatedFileKeepsWhatWasRead() {
        val data = smf(track(text(0, "/ab"), text(10, "cd"), end()))
        val m = MidiParser.parse(data.copyOf(data.size - 6))
        assertTrue(m.texts.isNotEmpty())
        assertEquals("/ab", m.texts[0].text)
    }

    @Test
    fun garbageThrows() {
        assertThrows(MidiParseException::class.java) {
            MidiParser.parse("hindi ito midi".toByteArray())
        }
    }

    @Test
    fun riffWrappedFileIsFound() {
        val wrapped = "RIFF".toByteArray() + ByteArray(4) + "RMIDdata".toByteArray() + ByteArray(4) +
            smf(track(end()))
        assertEquals(480, MidiParser.parse(wrapped).division)
    }

    @Test
    fun decodesWindows1252AndUtf8() {
        val latin = byteArrayOf(0xF1.toByte(), 'a'.code.toByte()) // "ña" sa Windows-1252
        val m = MidiParser.parse(
            smf(
                track(
                    MidiTestBuilder.textBytes(0, latin, 0x05),
                    text(10, "ñb", 0x05),
                    end()
                )
            )
        )
        assertEquals(listOf("ña", "ñb"), m.texts.map { it.text })
    }
}
