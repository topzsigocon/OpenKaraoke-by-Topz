package com.topz.karaoke.midi

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Nagdedecode ng text sa MIDI: UTF-8 kung valid, kung hindi ay Windows-1252 (karaniwan sa KAR). */
internal object TextDecoder {
    private val cp1252: Charset =
        try {
            Charset.forName("windows-1252")
        } catch (_: Exception) {
            Charsets.ISO_8859_1
        }

    fun decode(bytes: ByteArray): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, cp1252)
        }
    }
}

private class Truncated : Exception()

private class ByteReader(private val data: ByteArray, var pos: Int, private val end: Int) {
    fun hasMore(): Boolean = pos < end

    fun peek(): Int {
        if (pos >= end) throw Truncated()
        return data[pos].toInt() and 0xFF
    }

    fun u8(): Int {
        val v = peek()
        pos++
        return v
    }

    fun vlq(): Int {
        var value = 0
        for (i in 0 until 4) {
            val b = u8()
            value = (value shl 7) or (b and 0x7F)
            if ((b and 0x80) == 0) break
        }
        return value
    }

    fun bytes(n: Int): ByteArray {
        if (n < 0 || pos + n > end) throw Truncated()
        val out = data.copyOfRange(pos, pos + n)
        pos += n
        return out
    }

    fun skip(n: Int) {
        if (n < 0 || pos + n > end) throw Truncated()
        pos += n
    }
}

/**
 * Parser ng Standard MIDI File (.mid / .kar). Maluwag ito sa mga sirang file:
 * kung putol ang isang track, itinatago ang nabasa na at nagpapatuloy.
 * Ang mali lang na hindi kayang ipagpatuloy ay kung walang "MThd" o walang nabasang track.
 */
object MidiParser {

    fun parse(data: ByteArray): MidiFile {
        val start = indexOfHeader(data)
        if (start < 0) throw MidiParseException("Walang MThd header; hindi ito MIDI file")
        var pos = start + 4
        if (pos + 10 > data.size) throw MidiParseException("Putol ang header ng MIDI")

        val headerLength = readU32(data, pos)
        val format = readU16(data, pos + 4)
        val trackCount = readU16(data, pos + 6)
        val division = readU16(data, pos + 8)
        val headerSkip = maxOf(headerLength, 6L)
        val afterHeader = pos + 4L + headerSkip
        pos = if (afterHeader > data.size) data.size else afterHeader.toInt()

        val tracks = ArrayList<MidiTrack>()
        val tempos = ArrayList<TempoChange>()
        val texts = ArrayList<MidiText>()
        var maxTick = 0L

        for (trackIndex in 0 until trackCount) {
            // Laktawan ang mga chunk na hindi MTrk
            while (pos + 8 <= data.size && !isChunk(data, pos, 'M', 'T', 'r', 'k')) {
                val next = pos + 8L + readU32(data, pos + 4)
                pos = if (next > data.size) data.size else next.toInt()
            }
            if (pos + 8 > data.size) break

            val chunkLength = readU32(data, pos + 4)
            val trackStart = pos + 8
            val trackEnd = minOf(trackStart + chunkLength, data.size.toLong()).toInt()
            pos = trackEnd

            val reader = ByteReader(data, trackStart, trackEnd)
            val events = ArrayList<ChannelEvent>()
            var name: String? = null
            var tick = 0L
            var running = 0

            try {
                while (reader.hasMore()) {
                    tick += reader.vlq()
                    var status = reader.peek()
                    if (status < 0x80) {
                        if (running == 0) throw Truncated()
                        status = running
                    } else {
                        reader.u8()
                        if (status < 0xF0) running = status
                    }

                    if (status == 0xFF) {
                        val type = reader.u8()
                        val length = reader.vlq()
                        val payload = reader.bytes(length)
                        if (tick > maxTick) maxTick = tick
                        if (type == 0x2F) break
                        when {
                            type == 0x51 && payload.size == 3 -> {
                                val micros = ((payload[0].toInt() and 0xFF) shl 16) or
                                    ((payload[1].toInt() and 0xFF) shl 8) or
                                    (payload[2].toInt() and 0xFF)
                                tempos.add(TempoChange(tick, micros))
                            }
                            type == 0x03 && name == null -> name = TextDecoder.decode(payload)
                            type == MidiFile.KIND_TEXT || type == MidiFile.KIND_LYRIC ->
                                texts.add(MidiText(tick, type, TextDecoder.decode(payload), trackIndex))
                        }
                    } else if (status == 0xF0 || status == 0xF7) {
                        reader.skip(reader.vlq())
                    } else if (status < 0xF0) {
                        val kind = status and 0xF0
                        val d1 = reader.u8()
                        val d2 = if (kind == 0xC0 || kind == 0xD0) 0 else reader.u8()
                        events.add(ChannelEvent(tick, status, d1, d2))
                        if (tick > maxTick) maxTick = tick
                    } else {
                        when (status) {
                            0xF1, 0xF3 -> reader.skip(1)
                            0xF2 -> reader.skip(2)
                        }
                    }
                }
            } catch (_: Truncated) {
                // Putol o sirang track: itago ang nabasa na.
            }

            tracks.add(MidiTrack(trackIndex, name, events))
        }

        if (tracks.isEmpty()) throw MidiParseException("Walang nabasang track sa MIDI")
        return MidiFile(format, division, tracks, tempos, texts, maxTick)
    }

    /** Hinahanap ang "MThd" sa unang 4 KB; may mga file (hal. RIFF/RMID) na may nauunang bytes. */
    private fun indexOfHeader(data: ByteArray): Int {
        val limit = minOf(data.size - 4, 4096)
        for (i in 0..limit) {
            if (isChunk(data, i, 'M', 'T', 'h', 'd')) return i
        }
        return -1
    }

    private fun isChunk(data: ByteArray, i: Int, a: Char, b: Char, c: Char, d: Char): Boolean =
        i + 4 <= data.size &&
            data[i].toInt() == a.code && data[i + 1].toInt() == b.code &&
            data[i + 2].toInt() == c.code && data[i + 3].toInt() == d.code

    private fun readU16(data: ByteArray, i: Int): Int =
        ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)

    private fun readU32(data: ByteArray, i: Int): Long =
        ((data[i].toLong() and 0xFF) shl 24) or
            ((data[i + 1].toLong() and 0xFF) shl 16) or
            ((data[i + 2].toLong() and 0xFF) shl 8) or
            (data[i + 3].toLong() and 0xFF)
}
