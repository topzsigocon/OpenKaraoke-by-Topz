package com.topz.karaoke.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Paano ipinapakita ang numero ng kanta: 00006 (may zero sa unahan) o 6. Compose state, kaya nag-a-update ang UI. */
object SongNumberFormat {
    var padded by mutableStateOf(true)

    fun format(number: Int): String = if (padded) number.toString().padStart(5, '0') else number.toString()

    fun formatOrEmpty(number: Int?): String = if (number == null) "" else format(number)
}
