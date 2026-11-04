package com.topz.karaoke.ui

import com.topz.karaoke.data.LyricsOutline
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.topz.karaoke.data.BubbleSide
import com.topz.karaoke.data.BubbleSize
import com.topz.karaoke.data.HighlightColor
import com.topz.karaoke.data.LyricsCase
import com.topz.karaoke.data.LyricsSize

/** Itsura ng lyrics sa Karaoke Mode. Ang mga default ay ang kasalukuyang itsura. */
data class KaraokeStyle(
    val sizeScale: Float = 1f,
    val highlight: Color = KColors.Gold,
    val caseMode: LyricsCase = LyricsCase.ORIGINAL,
    val outline: LyricsOutline = LyricsOutline.MEDIUM
)

fun LyricsSize.scale(): Float = when (this) {
    LyricsSize.SMALL -> 0.8f
    LyricsSize.MEDIUM -> 1f
    LyricsSize.LARGE -> 1.25f
    LyricsSize.XLARGE -> 1.5f
}

fun HighlightColor.toColor(): Color = when (this) {
    HighlightColor.GOLD -> KColors.Gold
    HighlightColor.CYAN -> Color(0xFF4DD8FF)
    HighlightColor.PINK -> Color(0xFFFF6EB4)
    HighlightColor.GREEN -> Color(0xFF7CFF6B)
}

fun HighlightColor.label(): String = when (this) {
    HighlightColor.GOLD -> "Ginto"
    HighlightColor.CYAN -> "Cyan"
    HighlightColor.PINK -> "Pink"
    HighlightColor.GREEN -> "Berde"
}

fun BubbleSize.dpSize(): Dp = when (this) {
    BubbleSize.SMALL -> 48.dp
    BubbleSize.MEDIUM -> 60.dp
    BubbleSize.LARGE -> 76.dp
}

fun BubbleSide.alignment(): Alignment = when (this) {
    BubbleSide.RIGHT -> Alignment.BottomEnd
    BubbleSide.LEFT -> Alignment.BottomStart
}

/** Anino/outline ng lyrics. MEDIUM = kasalukuyang itsura. */
fun LyricsOutline.shadow(): Shadow? = when (this) {
    LyricsOutline.NONE -> null
    LyricsOutline.THIN -> Shadow(Color.Black, Offset(1f, 1f), 3f)
    LyricsOutline.MEDIUM -> Shadow(Color.Black, Offset(2f, 2f), 6f)
    LyricsOutline.THICK -> Shadow(Color.Black, Offset(3f, 3f), 10f)
}
