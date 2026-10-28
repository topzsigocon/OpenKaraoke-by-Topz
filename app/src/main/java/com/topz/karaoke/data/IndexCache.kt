package com.topz.karaoke.data

import java.io.File

/**
 * Sine-save ang listahan ng mga kanta sa isang file para hindi na kailangang mag-scan
 * ng 47,000 file sa bawat bukas ng app. Kapag nagbago ang [signature] (hal. ibang folder),
 * ituturing na luma ang cache.
 */
object IndexCache {
    private const val MAGIC = "OKINDEX"

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    fun save(file: File, signature: String, songs: List<Song>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.bufferedWriter().use { w ->
            w.write("$MAGIC\t${clean(signature)}\n")
            for (s in songs) {
                w.write(s.number?.toString() ?: "")
                w.write("\t")
                w.write(clean(s.title))
                w.write("\t")
                w.write(clean(s.artist ?: ""))
                w.write("\t")
                w.write(clean(s.path))
                w.write("\t")
                w.write(clean(s.entry ?: ""))
                w.write("\n")
            }
        }
        file.delete()
        tmp.renameTo(file)
    }

    /** Ibinabalik ang null kung walang cache, sira ito, o iba na ang signature. */
    fun load(file: File, signature: String): List<Song>? {
        if (!file.exists()) return null
        return try {
            file.bufferedReader().use { r ->
                val header = r.readLine()
                if (header != "$MAGIC\t${clean(signature)}") {
                    null
                } else {
                    val out = ArrayList<Song>()
                    while (true) {
                        val line = r.readLine() ?: break
                        val p = line.split('\t', limit = 5)
                        if (p.size < 5) continue
                        out.add(Song(p[0].toIntOrNull(), p[1], p[2].ifEmpty { null }, p[3], p[4].ifEmpty { null }))
                    }
                    out
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
