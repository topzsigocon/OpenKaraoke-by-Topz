package com.topz.karaoke.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsCodecTest {
    @Test
    fun roundTripKeepsTypesAndTrickyText() {
        val map = mapOf<String, Any>(
            "a" to "may\ttab at\nbagong linya at \\ backslash",
            "b" to 42,
            "c" to true,
            "d" to 1.5f,
            "e" to 123456789012L
        )
        val decoded = PrefsCodec.decode(PrefsCodec.encode(map)).associateBy { it.key }
        assertEquals("may\ttab at\nbagong linya at \\ backslash", decoded["a"]!!.value)
        assertEquals('S', decoded["a"]!!.type)
        assertEquals("42", decoded["b"]!!.value)
        assertEquals('I', decoded["b"]!!.type)
        assertEquals('B', decoded["c"]!!.type)
        assertEquals('F', decoded["d"]!!.type)
        assertEquals('L', decoded["e"]!!.type)
        assertEquals(5, decoded.size)
    }

    @Test
    fun garbageLinesAreIgnored() {
        assertEquals(0, PrefsCodec.decode("walang tab\n\nX\tlang\n").size)
    }
}
