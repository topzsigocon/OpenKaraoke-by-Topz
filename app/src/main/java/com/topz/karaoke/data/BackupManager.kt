package com.topz.karaoke.data

import android.content.Context
import android.content.SharedPreferences
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Pag-encode ng SharedPreferences sa teksto (isang entry bawat linya) at pabalik. Pure Kotlin. */
object PrefsCodec {
    data class Entry(val type: Char, val key: String, val value: String)

    fun encode(all: Map<String, *>): String {
        val sb = StringBuilder()
        for ((key, value) in all) {
            val type = when (value) {
                is String -> 'S'
                is Int -> 'I'
                is Boolean -> 'B'
                is Float -> 'F'
                is Long -> 'L'
                else -> continue
            }
            sb.append(type).append('\t').append(escape(key)).append('\t').append(escape(value.toString())).append('\n')
        }
        return sb.toString()
    }

    fun decode(text: String): List<Entry> {
        val out = ArrayList<Entry>()
        for (line in text.split('\n')) {
            if (line.isEmpty()) continue
            val parts = line.split('\t')
            if (parts.size != 3 || parts[0].length != 1) continue
            out.add(Entry(parts[0][0], unescape(parts[1]), unescape(parts[2])))
        }
        return out
    }

    private fun escape(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\t' -> sb.append("\\t")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    't' -> sb.append('\t')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}

/** Backup at restore ng mga setting, paborito, history, at queue sa isang zip. */
object BackupManager {
    private const val PREFS_ENTRY = "prefs.tsv"
    private val FILE_ENTRIES = listOf("queue.tsv", "history.tsv", "favorites.tsv")

    /** Ibinabalik ang null kung tagumpay, o mensahe ng error. */
    fun export(context: Context, target: File): String? =
        try {
            target.parentFile?.mkdirs()
            val prefs = context.getSharedPreferences("openkaraoke", Context.MODE_PRIVATE)
            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(PREFS_ENTRY))
                zip.write(PrefsCodec.encode(prefs.all).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                for (name in FILE_ENTRIES) {
                    val f = File(context.filesDir, name)
                    if (f.isFile) {
                        zip.putNextEntry(ZipEntry(name))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }

    /** Ibinabalik ang null kung tagumpay. Kailangang buksan ulit ang app pagkatapos. */
    fun import(context: Context, source: File): String? =
        try {
            var prefsText: String? = null
            val contents = HashMap<String, ByteArray>()
            ZipInputStream(source.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    // Mga kilalang pangalan lang ang tinatanggap (iwas sa mapanganib na path)
                    if (entry.name == PREFS_ENTRY) {
                        val buf = ByteArrayOutputStream()
                        zip.copyTo(buf)
                        prefsText = buf.toString("UTF-8")
                    } else if (entry.name in FILE_ENTRIES) {
                        val buf = ByteArrayOutputStream()
                        zip.copyTo(buf)
                        contents[entry.name] = buf.toByteArray()
                    }
                }
            }
            val text = prefsText ?: throw IllegalStateException("Hindi valid na backup (walang prefs).")
            applyPrefs(context.getSharedPreferences("openkaraoke", Context.MODE_PRIVATE), PrefsCodec.decode(text))
            for ((name, bytes) in contents) File(context.filesDir, name).writeBytes(bytes)
            null
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }

    private fun applyPrefs(prefs: SharedPreferences, entries: List<PrefsCodec.Entry>) {
        val editor = prefs.edit().clear()
        for (e in entries) {
            when (e.type) {
                'S' -> editor.putString(e.key, e.value)
                'I' -> e.value.toIntOrNull()?.let { editor.putInt(e.key, it) }
                'B' -> editor.putBoolean(e.key, e.value == "true")
                'F' -> e.value.toFloatOrNull()?.let { editor.putFloat(e.key, it) }
                'L' -> e.value.toLongOrNull()?.let { editor.putLong(e.key, it) }
            }
        }
        editor.commit()
    }
}
