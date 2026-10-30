package com.topz.karaoke.midi

/** Gumagawa ng maliliit na MIDI file sa memorya para sa mga unit test. */
object MidiTestBuilder {

    fun vlq(value: Int): ByteArray {
        val out = ArrayList<Byte>()
        var v = value
        out.add((v and 0x7F).toByte())
        v = v shr 7
        while (v > 0) {
            out.add(((v and 0x7F) or 0x80).toByte())
            v = v shr 7
        }
        out.reverse()
        return out.toByteArray()
    }

    fun int16(v: Int): ByteArray = byteArrayOf((v ushr 8).toByte(), v.toByte())

    fun int32(v: Int): ByteArray =
        byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    fun meta(delta: Int, type: Int, payload: ByteArray): ByteArray =
        vlq(delta) + byteArrayOf(0xFF.toByte(), type.toByte()) + vlq(payload.size) + payload

    fun text(delta: Int, s: String, kind: Int = 0x01): ByteArray =
        meta(delta, kind, s.toByteArray(Charsets.UTF_8))

    fun textBytes(delta: Int, bytes: ByteArray, kind: Int = 0x01): ByteArray = meta(delta, kind, bytes)

    fun trackName(name: String): ByteArray = meta(0, 0x03, name.toByteArray(Charsets.UTF_8))

    fun tempo(delta: Int, micros: Int): ByteArray =
        meta(delta, 0x51, byteArrayOf((micros shr 16).toByte(), (micros shr 8).toByte(), micros.toByte()))

    fun end(delta: Int = 0): ByteArray = meta(delta, 0x2F, ByteArray(0))

    fun track(vararg chunks: ByteArray): ByteArray {
        var body = ByteArray(0)
        for (c in chunks) body += c
        return "MTrk".toByteArray(Charsets.US_ASCII) + int32(body.size) + body
    }

    fun smf(vararg tracks: ByteArray, division: Int = 480, format: Int = 1): ByteArray {
        var out = "MThd".toByteArray(Charsets.US_ASCII) + int32(6) + int16(format) +
            int16(tracks.size) + int16(division)
        for (t in tracks) out += t
        return out
    }

    fun noteOn(delta: Int, ch: Int, key: Int, vel: Int): ByteArray =
        vlq(delta) + byteArrayOf((0x90 or ch).toByte(), key.toByte(), vel.toByte())

    fun noteOff(delta: Int, ch: Int, key: Int): ByteArray =
        vlq(delta) + byteArrayOf((0x80 or ch).toByte(), key.toByte(), 0)

    fun program(delta: Int, ch: Int, prog: Int): ByteArray =
        vlq(delta) + byteArrayOf((0xC0 or ch).toByte(), prog.toByte())

    fun cc(delta: Int, ch: Int, ctrl: Int, value: Int): ByteArray =
        vlq(delta) + byteArrayOf((0xB0 or ch).toByte(), ctrl.toByte(), value.toByte())
}
