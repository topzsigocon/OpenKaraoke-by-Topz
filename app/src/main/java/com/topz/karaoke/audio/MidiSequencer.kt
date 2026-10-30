package com.topz.karaoke.audio

import com.topz.karaoke.midi.MidiFile
import com.topz.karaoke.midi.TempoMap

data class ChannelInfo(val channel: Int, val label: String)

/**
 * Nagpapatugtog ng MidiFile sa isang [SynthSink]. Pure Kotlin (walang Android), kaya masusubukan.
 *
 * Ang oras ay sa "song frames" (sample rate ng output). Ang tempo factor ay nagpapabilis o
 * nagpapabagal sa pag-usad ng oras ng kanta, at ang lyrics (na nasa song time) ay nananatiling
 * magkasabay. Hindi thread-safe; ang AudioEngine ang nagbibigay ng lock.
 */
class MidiSequencer(midi: MidiFile, val sampleRate: Int) {

    private class Ev(val frame: Long, val status: Int, val d1: Int, val d2: Int, val priority: Int)

    private class Candidate(
        val channel: Int,
        val notes: Int,
        val avgPitch: Double,
        val chordRatio: Double,
        val hinted: Boolean
    )

    private val events: Array<Ev>
    val totalFrames: Long
    val channelInfos: List<ChannelInfo>

    /** Hula kung aling channel ang melody (para sa karaoke). Null kung hindi matiyak. */
    val melodyChannel: Int?

    /** Semitones (-12..12). Naaapektuhan ang mga bagong note; hindi ang drums (channel 10). */
    var transpose: Int = 0
        set(value) { field = value.coerceIn(-12, 12) }

    /** 1.0 = normal. 0.5 hanggang 2.0. */
    var tempoFactor: Double = 1.0
        set(value) { field = value.coerceIn(0.5, 2.0) }

    private val muted = BooleanArray(16)
    private val sounding = IntArray(16 * 128) { -1 } // orihinal na key -> key na pinatunog
    private var index = 0

    var positionFrames: Double = 0.0
        private set

    val positionMs: Long get() = (positionFrames * 1000.0 / sampleRate).toLong()
    val durationMs: Long get() = totalFrames * 1000L / sampleRate

    /** Tapos na kapag naproseso na ang lahat ng event at lumipas ang 2 segundong buntot. */
    val isFinished: Boolean
        get() = index >= events.size && positionFrames >= totalFrames + 2L * sampleRate

    init {
        val tempoMap = TempoMap(midi.division, midi.tempos)
        fun frameOf(tick: Long): Long = (tempoMap.tickToUs(tick) * sampleRate / 1_000_000.0).toLong()

        val list = ArrayList<Ev>()
        for (track in midi.tracks) {
            for (e in track.events) {
                val kind = e.status and 0xF0
                val priority = when {
                    kind == 0x80 || (kind == 0x90 && e.data2 == 0) -> 0 // note off muna
                    kind == 0x90 -> 2                                   // note on huli
                    else -> 1                                           // program/control sa gitna
                }
                list.add(Ev(frameOf(e.tick), e.status, e.data1, e.data2, priority))
            }
        }
        list.sortWith(compareBy<Ev>({ it.frame }, { it.priority }))
        events = list.toTypedArray()
        totalFrames = maxOf(if (events.isEmpty()) 0L else events[events.size - 1].frame, frameOf(midi.lengthTicks))

        val infos = ArrayList<ChannelInfo>()
        val candidates = ArrayList<Candidate>()
        for (ch in 0 until 16) {
            var hasNotes = false
            var noteCount = 0
            var pitchSum = 0L
            var chordCount = 0
            var program = -1
            var trackName: String? = null
            for (track in midi.tracks) {
                var inTrack = false
                var lastOnTick = -1L
                for (e in track.events) {
                    if ((e.status and 0x0F) != ch) continue
                    val kind = e.status and 0xF0
                    if (kind == 0x90 && e.data2 > 0) {
                        hasNotes = true
                        inTrack = true
                        noteCount++
                        pitchSum += e.data1
                        if (e.tick == lastOnTick) chordCount++
                        lastOnTick = e.tick
                    } else if (kind == 0xC0 && program < 0) {
                        program = e.data1
                    }
                }
                if (inTrack && trackName == null) trackName = track.name?.trim()?.takeIf { it.isNotEmpty() }
            }
            if (!hasNotes) continue
            val instrument = if (ch == 9) "Drums" else GmNames.name(maxOf(program, 0))
            val label = if (ch == 9 || trackName == null || trackName.equals(instrument, ignoreCase = true)) {
                instrument
            } else {
                "$trackName ($instrument)"
            }
            infos.add(ChannelInfo(ch, "${ch + 1} $label"))
            if (ch != 9) {
                val hinted = trackName != null && MELODY_HINT.containsMatchIn(trackName)
                candidates.add(
                    Candidate(ch, noteCount, pitchSum.toDouble() / noteCount, chordCount.toDouble() / noteCount, hinted)
                )
            }
        }
        channelInfos = infos
        melodyChannel = pickMelody(candidates)
    }

    private fun pickMelody(list: List<Candidate>): Int? {
        // 1) Kung may track na pinangalanang "Melody", "Vocal", atbp.
        val hinted = list.filter { it.hinted && it.notes >= 8 }.maxByOrNull { it.notes }
        if (hinted != null) return hinted.channel
        // 2) Kung wala: isang linya lang ang tumutugtog (halos walang chord), mataas ang tono, at maraming note
        return list
            .filter { it.notes >= 20 && it.chordRatio <= 0.15 && it.avgPitch >= 58.0 }
            .maxByOrNull { it.notes }
            ?.channel
    }

    fun isMuted(channel: Int): Boolean = muted.getOrElse(channel) { false }

    fun setMuted(channel: Int, value: Boolean, sink: SynthSink) {
        if (channel !in 0..15) return
        muted[channel] = value
        if (value) {
            sink.channelNotesOff(channel)
            java.util.Arrays.fill(sounding, channel * 128, channel * 128 + 128, -1)
        }
    }

    /** Pinauusad ang kanta ng [blockFrames] (na may tempo factor) at pinapatugtog ang mga event na dapat. */
    fun advance(blockFrames: Int, sink: SynthSink) {
        val newPosition = positionFrames + blockFrames * tempoFactor
        while (index < events.size && events[index].frame < newPosition) {
            apply(events[index], sink, notes = true)
            index++
        }
        positionFrames = newPosition
    }

    /**
     * Lumilipat sa [ms]. Ni-re-reset ang synth at ibinabalik ang program, controller at pitch bend
     * hanggang sa puntong iyon (walang tutunog na note).
     */
    fun seek(ms: Long, sink: SynthSink) {
        val target = maxOf(ms, 0L) * sampleRate / 1000L
        sink.reset()
        java.util.Arrays.fill(sounding, -1)
        var i = 0
        while (i < events.size && events[i].frame < target) {
            apply(events[i], sink, notes = false)
            i++
        }
        index = i
        positionFrames = target.toDouble()
    }

    private fun apply(e: Ev, sink: SynthSink, notes: Boolean) {
        val ch = e.status and 0x0F
        when (e.status and 0xF0) {
            0x90 -> if (notes) { if (e.d2 == 0) noteOff(ch, e.d1, sink) else noteOn(ch, e.d1, e.d2, sink) }
            0x80 -> if (notes) noteOff(ch, e.d1, sink)
            0xB0 -> sink.controlChange(ch, e.d1, e.d2)
            0xC0 -> sink.programChange(ch, e.d1)
            0xE0 -> sink.pitchBend(ch, e.d1 or (e.d2 shl 7))
        }
    }

    private fun noteOn(ch: Int, key: Int, velocity: Int, sink: SynthSink) {
        if (muted[ch]) return
        val shifted = if (ch == 9) key else (key + transpose).coerceIn(0, 127)
        val slot = ch * 128 + key
        val previous = sounding[slot]
        if (previous >= 0) sink.noteOff(ch, previous)
        sounding[slot] = shifted
        sink.noteOn(ch, shifted, velocity)
    }

    private fun noteOff(ch: Int, key: Int, sink: SynthSink) {
        val slot = ch * 128 + key
        val shifted = sounding[slot]
        if (shifted >= 0) {
            sink.noteOff(ch, shifted)
            sounding[slot] = -1
        }
    }

    private companion object {
        val MELODY_HINT = Regex("(?i)(melod|vocal|voice|vox|singer|lead|karaoke|lyric)")
    }
}
