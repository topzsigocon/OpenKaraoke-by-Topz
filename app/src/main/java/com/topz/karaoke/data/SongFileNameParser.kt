package com.topz.karaoke.data

data class ParsedSongName(
    val number: Int?,
    val title: String,
    val artist: String?
)

/**
 * Kinukuha ang song number, title at artist sa pangalan ng file.
 *
 * Mga tinatanggap:
 *   "00123 - Pamagat - Artist.mid"
 *   "00123_Pamagat.kar"
 *   "123 Pamagat.mid"
 *   "123.mid"            (numero lang)
 *   "Pamagat.mid"        (walang numero)
 *
 * Kapag " - " ang pagitan, ang una ay title at ang natitira ay artist.
 * Kapag walang " - ", ang buong natitira ay title (ang "_" ay nagiging espasyo).
 * Tandaan: ang pangalang nagsisimula sa numero (hal. "7 Rings.mid") ay
 * ituturing na may song number na 7.
 */
object SongFileNameParser {
    private val numberOnly = Regex("""^\s*(\d{1,9})\s*$""")
    private val numberThenRest = Regex("""^\s*(\d{1,9})(?:\s*[-_.)]+\s*|\s+)(.+)$""")

    fun parse(fileName: String): ParsedSongName {
        val base = fileName.substringBeforeLast('.', fileName).trim()

        numberOnly.matchEntire(base)?.let {
            return ParsedSongName(it.groupValues[1].toInt(), base, null)
        }

        val match = numberThenRest.matchEntire(base)
        val number = match?.groupValues?.get(1)?.toInt()
        val rest = match?.groupValues?.get(2) ?: base

        val parts = rest.split(" - ", limit = 2).map { it.trim() }
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            ParsedSongName(number, cleanup(parts[0]), cleanup(parts[1]))
        } else {
            ParsedSongName(number, cleanup(rest), null)
        }
    }

    private fun cleanup(s: String) = s.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
}
