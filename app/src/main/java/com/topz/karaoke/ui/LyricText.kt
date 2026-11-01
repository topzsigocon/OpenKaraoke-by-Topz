package com.topz.karaoke.ui

import com.topz.karaoke.data.LyricsCase

/** Pagpapalit ng malaki/maliit na titik ng mga piraso (syllable) ng lyrics. Pure Kotlin. */
object LyricText {
    /**
     * [atWordStart] = true kung ang piraso ay simula ng bagong salita (ang nauna ay nagtapos sa espasyo),
     * para hindi mapalitan nang mali ang gitna ng salitang hinati sa syllable (hal. "Mel" + "low").
     */
    fun format(text: String, mode: LyricsCase, atWordStart: Boolean): String = when (mode) {
        LyricsCase.ORIGINAL -> text
        LyricsCase.UPPERCASE -> text.uppercase()
        LyricsCase.TITLE -> title(text, atWordStart)
    }

    private fun title(text: String, atWordStart: Boolean): String {
        val out = StringBuilder(text.length)
        var boundary = atWordStart
        for (c in text) {
            when {
                c.isLetter() -> {
                    out.append(if (boundary) c.uppercaseChar() else c.lowercaseChar())
                    boundary = false
                }
                c.isDigit() -> {
                    out.append(c)
                    boundary = false
                }
                else -> {
                    out.append(c)
                    if (c.isWhitespace() || c == '(' || c == '"') boundary = true
                }
            }
        }
        return out.toString()
    }
}
