package com.topz.karaoke.audio

import com.topz.karaoke.midi.MidiParser
import com.topz.karaoke.midi.MidiTestBuilder.cc
import com.topz.karaoke.midi.MidiTestBuilder.end
import com.topz.karaoke.midi.MidiTestBuilder.noteOff
import com.topz.karaoke.midi.MidiTestBuilder.noteOn
import com.topz.karaoke.midi.MidiTestBuilder.program
import com.topz.karaoke.midi.MidiTestBuilder.smf
import com.topz.karaoke.midi.MidiTestBuilder.track
import com.topz.karaoke.midi.MidiTestBuilder.trackName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiSequencerTest {

    /** Itinatala ang lahat ng tawag para masuri. */
    private class RecordingSink : SynthSink {
        val calls = ArrayList<String>()
        override fun noteOn(channel: Int, key: Int, velocity: Int) { calls.add("on $channel $key $velocity") }
        override fun noteOff(channel: Int, key: Int) { calls.add("off $channel $key") }
        override fun programChange(channel: Int, program: Int) { calls.add("prog $channel $program") }
        override fun controlChange(channel: Int, controller: Int, value: Int) { calls.add("cc $channel $controller $value") }
        override fun pitchBend(channel: Int, value: Int) { calls.add("bend $channel $value") }
        override fun channelNotesOff(channel: Int) { calls.add("chOff $channel") }
        override fun reset() { calls.add("reset") }
    }

    // sampleRate 1000: 1 frame = 1 ms. Division 480, default tempo: 480 ticks = 500 ms.
    private fun sequencer(vararg chunks: ByteArray): MidiSequencer =
        MidiSequencer(MidiParser.parse(smf(track(*chunks, end()))), 1000)

    @Test
    fun notesPlayAtTheRightTime() {
        val seq = sequencer(noteOn(0, 0, 60, 100), noteOff(480, 0, 60))
        val sink = RecordingSink()
        seq.advance(400, sink)
        assertEquals(listOf("on 0 60 100"), sink.calls)
        seq.advance(200, sink)
        assertEquals(listOf("on 0 60 100", "off 0 60"), sink.calls)
    }

    @Test
    fun noteOffComesBeforeNoteOnAtTheSameTime() {
        val seq = sequencer(noteOn(0, 0, 60, 100), noteOn(480, 0, 62, 100), noteOff(0, 0, 60))
        val sink = RecordingSink()
        seq.advance(600, sink)
        assertEquals(listOf("on 0 60 100", "off 0 60", "on 0 62 100"), sink.calls)
    }

    @Test
    fun transposeAppliesToNoteOnAndMatchingNoteOff() {
        val seq = sequencer(noteOn(0, 0, 60, 100), noteOff(480, 0, 60))
        val sink = RecordingSink()
        seq.transpose = 2
        seq.advance(100, sink)
        seq.transpose = -5 // nagbago habang tumutunog; dapat pa rin tumama ang note off
        seq.advance(500, sink)
        assertEquals(listOf("on 0 62 100", "off 0 62"), sink.calls)
    }

    @Test
    fun drumsAreNotTransposed() {
        val seq = sequencer(noteOn(0, 9, 36, 100))
        val sink = RecordingSink()
        seq.transpose = 7
        seq.advance(10, sink)
        assertEquals(listOf("on 9 36 100"), sink.calls)
    }

    @Test
    fun mutedChannelIsSilentAndStopsItsNotes() {
        val seq = sequencer(noteOn(0, 0, 60, 100), noteOn(0, 1, 64, 100), noteOff(480, 1, 64))
        val sink = RecordingSink()
        seq.setMuted(0, true, sink)
        seq.advance(100, sink)
        assertEquals(listOf("chOff 0", "on 1 64 100"), sink.calls)
        assertTrue(seq.isMuted(0))
    }

    @Test
    fun tempoFactorSpeedsUpTheSong() {
        val seq = sequencer(noteOn(0, 0, 60, 100), noteOff(480, 0, 60)) // note off sa 500 ms
        val sink = RecordingSink()
        seq.tempoFactor = 2.0
        seq.advance(250, sink) // 500 song-frames
        assertEquals(listOf("on 0 60 100"), sink.calls)
        seq.advance(1, sink)
        assertEquals(listOf("on 0 60 100", "off 0 60"), sink.calls)
    }

    @Test
    fun seekRestoresControllersButNotNotes() {
        val seq = sequencer(program(0, 0, 5), cc(0, 0, 7, 100), noteOn(480, 0, 60, 100))
        val sink = RecordingSink()
        seq.seek(250, sink)
        assertEquals(listOf("reset", "prog 0 5", "cc 0 7 100"), sink.calls)
        assertEquals(250L, seq.positionMs)
        seq.advance(300, sink)
        assertEquals("on 0 60 100", sink.calls.last())
    }

    @Test
    fun finishesAfterTheTail() {
        val seq = sequencer(noteOn(0, 0, 60, 100), noteOff(480, 0, 60))
        val sink = RecordingSink()
        seq.advance(2000, sink)
        assertFalse(seq.isFinished)
        seq.advance(600, sink)
        assertTrue(seq.isFinished)
    }

    @Test
    fun listsChannelsThatHaveNotes() {
        val midi = MidiParser.parse(
            smf(
                track(
                    trackName("Melody"), program(0, 0, 73), noteOn(0, 0, 60, 100),
                    noteOn(0, 9, 36, 100), end()
                )
            )
        )
        val infos = MidiSequencer(midi, 1000).channelInfos
        assertEquals(listOf(0, 9), infos.map { it.channel })
        assertEquals("1 Melody (Flute)", infos[0].label)
        assertEquals("10 Drums", infos[1].label)
    }

    // ---- Hula sa melody channel ----

    private fun notes(count: Int, ch: Int, key: Int): Array<ByteArray> =
        Array(count) { noteOn(120, ch, key, 100) }

    @Test
    fun melodyChannelFromTrackName() {
        val midi = MidiParser.parse(
            smf(
                track(trackName("Melody"), *notes(10, 0, 72), end()),
                track(trackName("Piano"), *notes(30, 1, 60), end())
            )
        )
        assertEquals(0, MidiSequencer(midi, 1000).melodyChannel)
    }

    @Test
    fun melodyChannelByShapeWhenNoNames() {
        val chords = ArrayList<ByteArray>()
        repeat(13) {
            chords.add(noteOn(120, 1, 60, 100))
            chords.add(noteOn(0, 1, 64, 100)) // sabay na note = chord
        }
        val midi = MidiParser.parse(
            smf(
                track(*notes(25, 0, 72), end()),
                track(*chords.toTypedArray(), end()),
                track(*notes(30, 2, 40), end())
            )
        )
        assertEquals(0, MidiSequencer(midi, 1000).melodyChannel)
    }

    @Test
    fun noMelodyGuessForTinyOrChordOnlySongs() {
        val midi = MidiParser.parse(smf(track(*notes(5, 0, 72), end())))
        assertEquals(null, MidiSequencer(midi, 1000).melodyChannel)
    }
}
