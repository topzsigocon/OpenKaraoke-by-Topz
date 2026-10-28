package com.topz.karaoke.midi

/** Nagko-convert ng MIDI ticks papuntang milliseconds, kasama ang pagbabago ng tempo. */
class TempoMap(division: Int, tempos: List<TempoChange>) {

    private val smpte: Boolean = (division and 0x8000) != 0
    private val msPerTickSmpte: Double
    private val starts: LongArray
    private val cumulativeUs: DoubleArray
    private val usPerTick: DoubleArray

    init {
        if (smpte) {
            // SMPTE: ang mataas na byte ay negatibong frames-per-second.
            val fpsRaw = 256 - (division shr 8)
            val fps = if (fpsRaw == 29) 29.97 else fpsRaw.toDouble()
            val ticksPerFrame = maxOf(division and 0xFF, 1)
            msPerTickSmpte = 1000.0 / (fps * ticksPerFrame)
            starts = longArrayOf(0L)
            cumulativeUs = doubleArrayOf(0.0)
            usPerTick = doubleArrayOf(0.0)
        } else {
            msPerTickSmpte = 0.0
            val div = if (division <= 0) 480 else division
            val st = ArrayList<Long>()
            val cu = ArrayList<Double>()
            val upt = ArrayList<Double>()

            var curTick = 0L
            var curUs = 0.0
            var curUpt = 500000.0 / div // default na 120 BPM
            st.add(0L)
            cu.add(0.0)
            upt.add(curUpt)

            for (t in tempos.sortedBy { it.tick }) {
                curUs += (t.tick - curTick) * curUpt
                curTick = t.tick
                curUpt = t.microsPerQuarter.toDouble() / div
                if (st[st.size - 1] == t.tick) {
                    upt[upt.size - 1] = curUpt
                } else {
                    st.add(t.tick)
                    cu.add(curUs)
                    upt.add(curUpt)
                }
            }
            starts = st.toLongArray()
            cumulativeUs = cu.toDoubleArray()
            usPerTick = upt.toDoubleArray()
        }
    }

    fun tickToMs(tick: Long): Long {
        if (tick <= 0L) return 0L
        if (smpte) return Math.round(tick * msPerTickSmpte)
        val found = java.util.Arrays.binarySearch(starts, tick)
        val i = if (found >= 0) found else -found - 2
        return Math.round((cumulativeUs[i] + (tick - starts[i]) * usPerTick[i]) / 1000.0)
    }
}
