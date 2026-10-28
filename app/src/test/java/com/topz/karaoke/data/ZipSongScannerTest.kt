package com.topz.karaoke.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipSongScannerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @After
    fun tearDown() = SongReader.closeAll()

    private fun makeZip(): File {
        val f = tmp.newFile("songs.zip")
        ZipOutputStream(f.outputStream()).use { z ->
            fun add(name: String, text: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(text.toByteArray())
                z.closeEntry()
            }
            add("00001 - Isang Kanta - Artist.mid", "AAA")
            add("sub/folder/00002.KAR", "BBB")
            add("readme.txt", "hindi kanta")
            add("__MACOSX/._00003.mid", "junk")
            add("sub/.hidden.mid", "junk")
            z.putNextEntry(ZipEntry("emptydir/"))
            z.closeEntry()
        }
        return f
    }

    @Test
    fun scanFindsOnlySongs() {
        val names = ZipSongScanner.scan(makeZip()).map { it.fileName }.sorted()
        assertEquals(listOf("00001 - Isang Kanta - Artist.mid", "00002.KAR"), names)
    }

    @Test
    fun brokenZipGivesEmptyList() {
        val bad = tmp.newFile("bad.zip")
        bad.writeText("hindi ito zip")
        assertEquals(emptyList<ZipSongEntry>(), ZipSongScanner.scan(bad))
    }

    @Test
    fun readerReadsFromZip() {
        val zip = makeZip()
        val song = Song(2, "00002", null, zip.path, "sub/folder/00002.KAR")
        assertEquals("BBB", String(SongReader.read(song)))
    }

    @Test
    fun readerReadsLooseFile() {
        val f = tmp.newFile("a.mid")
        f.writeText("CCC")
        assertEquals("CCC", String(SongReader.read(Song(1, "a", null, f.path))))
    }
}
