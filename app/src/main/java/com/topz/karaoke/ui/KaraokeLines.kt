package com.topz.karaoke.ui

/** Aling mga linya ang nasa itaas at ibaba sa two-line mode (null kung wala nang susunod). */
data class TwoLinePlan(val topIndex: Int?, val bottomIndex: Int?)

object KaraokeLines {
    /**
     * Two-line: ang kasalukuyang linya (c) at ang susunod (c+1) ay nasa magkabilang puwesto.
     * Ang even na linya ay laging sa itaas at ang odd sa ibaba. Kapag nagsimula ang susunod na linya,
     * ang natapos na puwesto ay agad napapalitan ng linya pagkatapos nito, kaya nagpapalitan ang dalawa.
     * Bago magsimula ang kanta (current = -1), ang unang dalawang linya ang ipinapakita.
     */
    fun twoLine(current: Int, lineCount: Int): TwoLinePlan {
        val c = maxOf(current, 0)
        val top = if (c % 2 == 0) c else c + 1
        val bottom = if (c % 2 == 1) c else c + 1
        return TwoLinePlan(
            topIndex = top.takeIf { it < lineCount },
            bottomIndex = bottom.takeIf { it < lineCount }
        )
    }
}
