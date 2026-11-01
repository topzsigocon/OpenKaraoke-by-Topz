package com.topz.karaoke.data

/** Ayos ng lyrics sa Karaoke Mode. DEFAULT = kasalukuyang linya + susunod na mas maliit. */
enum class KaraokeLayout { DEFAULT, TWO_LINE }

/** Saan ilalagay ang lyrics sa screen. */
enum class LyricsPosition { CENTER, BOTTOM }

/** CENTERED = parehong nasa gitna. OPPOSING = unang linya sa kaliwa, pangalawa sa kanan. */
enum class LyricsAlignment { CENTERED, OPPOSING }

/** Laki ng letra ng lyrics sa Karaoke Mode. MEDIUM = kasalukuyang laki. */
enum class LyricsSize { SMALL, MEDIUM, LARGE, XLARGE }

/** Kulay ng highlight ng nakantang salita. GOLD = kasalukuyang kulay. */
enum class HighlightColor { GOLD, CYAN, PINK, GREEN }

/** ORIGINAL = ayon sa file; UPPERCASE = MALALAKING TITIK; TITLE = Karaniwan (Title Case). */
enum class LyricsCase { ORIGINAL, UPPERCASE, TITLE }

/** Dami ng sabay-sabay na note at kung gagamit ng background video. HIGH = kasalukuyang ayos. */
enum class PerformanceMode(val voices: Int) {
    HIGH(64),
    MEDIUM(40),
    ECONOMY(24)
}

enum class BubbleSize { SMALL, MEDIUM, LARGE }

enum class BubbleSide { RIGHT, LEFT }
