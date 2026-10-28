package com.topz.karaoke.midi

data class LyricWord(val timeMs: Long, val text: String)

data class LyricLine(val words: List<LyricWord>) {
    val startMs: Long get() = words.first().timeMs
    val text: String get() = words.joinToString("") { it.text }.trim()
}

data class Lyrics(
    val lines: List<LyricLine>,
    /** Mga @T na linya ng KAR (karaniwang pamagat at artist). */
    val info: List<String>,
    val source: Source,
    val durationMs: Long
) {
    enum class Source { LYRIC_EVENTS, TEXT_EVENTS, NONE }

    /** Index ng linyang dapat nakikita sa oras na [ms]; -1 kung wala pang nagsisimula. */
    fun lineIndexAt(ms: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var answer = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].startMs <= ms) {
                answer = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return answer
    }
}

/**
 * Kinukuha ang lyrics mula sa MIDI/KAR.
 *  - Kung may Lyric events (0x05), iyon ang gagamitin.
 *  - Kung wala, ang Text events (0x01) ng track na "Words" (o ng track na pinakamarami ang text).
 *  - "@..." na text ay header ng KAR (hindi lyrics); ang "@T" ay pamagat/artist.
 *  - "/" at "\" sa simula ng text = bagong linya; ang \r at \n sa loob ay bagong linya rin.
 *  - Ang sobrang habang linya ay hinahati (sa pagitan ng mga salita) para kasya sa screen.
 */
object LyricsExtractor {
    const val DEFAULT_MAX_LINE_CHARS = 60

    fun extract(midi: MidiFile, maxLineChars: Int = DEFAULT_MAX_LINE_CHARS): Lyrics {
        val tempoMap = TempoMap(midi.division, midi.tempos)
        val duration = tempoMap.tickToMs(midi.lengthTicks)

        val lyricEvents = midi.texts.filter { it.kind == MidiFile.KIND_LYRIC && it.text.isNotBlank() }
        val source: List<MidiText>
        val kind: Lyrics.Source
        if (lyricEvents.isNotEmpty()) {
            source = lyricEvents
            kind = Lyrics.Source.LYRIC_EVENTS
        } else {
            val byTrack = midi.texts.filter { it.kind == MidiFile.KIND_TEXT }.groupBy { it.trackIndex }
            if (byTrack.isEmpty()) return Lyrics(emptyList(), emptyList(), Lyrics.Source.NONE, duration)

            var chosen: List<MidiText>? = null
            for ((trackIndex, events) in byTrack) {
                val trackName = midi.tracks.firstOrNull { it.index == trackIndex }
                    ?.name?.trim()?.lowercase()
                if (trackName == "words" || trackName == "lyrics") {
                    chosen = events
                    break
                }
            }
            source = chosen ?: byTrack.values.maxByOrNull { it.size } ?: emptyList()
            kind = Lyrics.Source.TEXT_EVENTS
        }

        val info = ArrayList<String>()
        val rawLines = ArrayList<List<LyricWord>>()
        var current = ArrayList<LyricWord>()
        var newLine = false

        for (e in source.sortedBy { it.tick }) {
            val text = e.text
            if (text.startsWith("@")) {
                if (text.startsWith("@T")) info.add(text.substring(2).trim())
                continue
            }

            val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
            var i = 0
            while (i < normalized.length && (normalized[i] == '/' || normalized[i] == '\\')) {
                newLine = true
                i++
            }
            val parts = normalized.substring(i).split('\n')
            for ((partIndex, part) in parts.withIndex()) {
                if (partIndex > 0) newLine = true
                if (part.isEmpty()) continue
                if (newLine) {
                    if (current.isNotEmpty()) {
                        rawLines.add(current)
                        current = ArrayList()
                    }
                    newLine = false
                }
                current.add(LyricWord(tempoMap.tickToMs(e.tick), part))
            }
        }
        if (current.isNotEmpty()) rawLines.add(current)

        val lines = ArrayList<LyricLine>()
        for (line in rawLines) {
            var buffer = ArrayList<LyricWord>()
            var length = 0
            for (w in line) {
                val boundary = buffer.isEmpty() ||
                    buffer[buffer.size - 1].text.endsWith(" ") ||
                    w.text.startsWith(" ")
                if (buffer.isNotEmpty() && boundary && length + w.text.length > maxLineChars) {
                    lines.add(LyricLine(buffer))
                    buffer = ArrayList()
                    length = 0
                }
                buffer.add(w)
                length += w.text.length
            }
            if (buffer.isNotEmpty()) lines.add(LyricLine(buffer))
        }

        return Lyrics(lines.filter { it.text.isNotBlank() }, info, kind, duration)
    }
}
