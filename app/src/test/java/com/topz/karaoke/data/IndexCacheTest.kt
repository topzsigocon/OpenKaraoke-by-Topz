package com.topz.karaoke.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IndexCacheTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val songs = listOf(
        Song(123, "Pamagat", "Artist", "/storage/a/00123 - Pamagat - Artist.mid"),
        Song(null, "Walang numero", null, "/storage/a/Walang numero.mid"),
        Song(7, "Sa zip", null, "/storage/b/songs.zip", "sub/00007 Sa zip.mid")
    )

    @Test
    fun roundTrip() {
        val f = tmp.newFile("index.tsv")
        IndexCache.save(f, "v1|default", songs)
        assertEquals(songs, IndexCache.load(f, "v1|default"))
    }

    @Test
    fun differentSignatureIsIgnored() {
        val f = tmp.newFile("index.tsv")
        IndexCache.save(f, "v1|default", songs)
        assertNull(IndexCache.load(f, "v1|/storage/iba"))
    }

    @Test
    fun missingFileGivesNull() {
        assertNull(IndexCache.load(tmp.root.resolve("wala.tsv"), "v1|default"))
    }
}
