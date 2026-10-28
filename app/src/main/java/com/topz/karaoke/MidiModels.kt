package com.topz.karaoke.midi

class MidiParseException(message: String) : Exception(message)

/** Channel event (note on/off, program change, atbp.). Itinatabi para sa player sa susunod na yugto. */
data class ChannelEvent(val tick: Long, val status: Int, val data1: Int, val data2: Int)

data class MidiTrack(val index: Int, val name: String?, val events: List<ChannelEvent>)

data class TempoChange(val tick: Long, val microsPerQuarter: Int)

/** Text (0x01) o Lyric (0x05) na meta event. */
data class MidiText(val tick: Long, val kind: Int, val text: String, val trackIndex: Int)

class MidiFile(
    val format: Int,
    val division: Int,
    val tracks: List<MidiTrack>,
    val tempos: List<TempoChange>,
    val texts: List<MidiText>,
    val lengthTicks: Long
) {
    companion object {
        const val KIND_TEXT = 0x01
        const val KIND_LYRIC = 0x05
    }
}
