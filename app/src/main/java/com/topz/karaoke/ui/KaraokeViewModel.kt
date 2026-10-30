package com.topz.karaoke.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topz.karaoke.audio.DefaultSoundFont
import com.topz.karaoke.audio.Player
import com.topz.karaoke.data.IndexCache
import com.topz.karaoke.data.SettingsStore
import com.topz.karaoke.data.Song
import com.topz.karaoke.data.SongFileNameParser
import com.topz.karaoke.data.ZipSongScanner
import com.topz.karaoke.storage.FileKind
import com.topz.karaoke.storage.FileStorageProvider
import com.topz.karaoke.storage.StorageProvider
import com.topz.karaoke.storage.StoredFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class KaraokeViewModel(app: Application) : AndroidViewModel(app) {

    private val storage: StorageProvider = FileStorageProvider(app)
    private val settings = SettingsStore(app)
    private val cacheFile = File(app.filesDir, "song_index.tsv")

    /** Ang audio player (SoundFont synth). */
    val player = Player(viewModelScope) { resolveSoundFont() }

    var hasAccess by mutableStateOf(storage.hasAccess())
        private set
    var scanning by mutableStateOf(false)
        private set
    var scanProgress by mutableStateOf(0)
        private set
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var zipSongCount by mutableStateOf(0)
        private set
    var soundFontCount by mutableStateOf(0)
        private set
    var soundFontFiles by mutableStateOf<List<StoredFile>>(emptyList())
        private set
    var activeSoundFont by mutableStateOf(settings.getActiveSoundFont())
        private set
    var videoCount by mutableStateOf(0)
        private set
    var folders by mutableStateOf<Map<FileKind, String?>>(
        FileKind.values().associateWith { settings.getFolder(it) }
    )
        private set

    val queue = mutableStateListOf<Song>()

    private var byNumber: Map<Int, Song> = emptyMap()
    private var started = false

    init {
        player.onSongFinished = { playNextFromQueue() }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    /** Tinatawag sa background thread (kinokopya ang default SoundFont sa unang gamit). */
    private fun resolveSoundFont(): String? {
        val active = settings.getActiveSoundFont()
        if (active != null && File(active).isFile) return active
        return DefaultSoundFont.path(getApplication())
    }

    fun selectSoundFont(path: String?) {
        settings.setActiveSoundFont(path)
        activeSoundFont = path
    }

    fun volumeRoots(): List<File> = storage.volumeRoots()

    /** Tinatawag sa onResume: i-check ang access; sa unang pagkakataon, gamitin ang cache o mag-scan. */
    fun onResume() {
        hasAccess = storage.hasAccess()
        if (hasAccess && !started) {
            started = true
            loadCacheOrScan()
        }
    }

    private fun signature(): String = "v1|" + (folders[FileKind.SONG] ?: "default")

    private fun loadCacheOrScan() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { IndexCache.load(cacheFile, signature()) }
            if (cached != null && cached.isNotEmpty()) {
                applySongs(cached)
                refreshCounts()
            } else {
                rescan()
            }
        }
    }

    /** Buong pag-scan ng folder (at ng mga zip sa loob nito). Sine-save ang resulta sa cache. */
    fun rescan() {
        hasAccess = storage.hasAccess()
        if (!hasAccess) return
        started = true
        viewModelScope.launch {
            scanning = true
            scanProgress = 0
            withContext(Dispatchers.IO) { storage.ensureFolders() }

            val files = listFor(FileKind.SONG) { n -> scanProgress = n }
            val result = withContext(Dispatchers.IO) { buildSongs(files) }
            applySongs(result)
            withContext(Dispatchers.IO) { IndexCache.save(cacheFile, signature(), result) }
            refreshCounts()
            scanning = false
        }
    }

    /**
     * Pinagsasama ang mga hiwalay na MIDI file at ang mga nasa loob ng zip.
     * Kapag pareho ang pangalan ng file sa dalawa, ang hiwalay na file ang pinipili.
     */
    private fun buildSongs(files: List<StoredFile>): List<Song> {
        val seen = HashSet<String>()
        val out = ArrayList<Song>()

        fun add(fileName: String, song: Song) {
            if (seen.add(fileName.substringBeforeLast('.').lowercase())) out.add(song)
        }

        val zips = ArrayList<StoredFile>()
        for (f in files) {
            if (f.name.endsWith(".zip", ignoreCase = true)) {
                zips.add(f)
            } else {
                val p = SongFileNameParser.parse(f.name)
                add(f.name, Song(p.number, p.title, p.artist, f.path))
            }
        }
        for (z in zips) {
            for (e in ZipSongScanner.scan(File(z.path))) {
                val p = SongFileNameParser.parse(e.fileName)
                add(e.fileName, Song(p.number, p.title, p.artist, z.path, e.entryName))
            }
        }
        return out.sortedWith(compareBy({ it.number ?: Int.MAX_VALUE }, { it.title.lowercase() }))
    }

    private fun applySongs(list: List<Song>) {
        val map = HashMap<Int, Song>()
        for (s in list) {
            val n = s.number ?: continue
            if (n !in map) map[n] = s
        }
        byNumber = map
        songs = list
        zipSongCount = list.count { it.entry != null }
    }

    private suspend fun refreshCounts() {
        val fonts = listFor(FileKind.SOUNDFONT)
        soundFontFiles = fonts
        soundFontCount = fonts.size
        videoCount = listFor(FileKind.VIDEO).size
    }

    private suspend fun listFor(kind: FileKind, onProgress: (Int) -> Unit = {}): List<StoredFile> {
        val custom = folders[kind]
        return if (custom != null) {
            storage.listIn(File(custom), kind, onProgress)
        } else {
            storage.list(kind, onProgress)
        }
    }

    fun setFolder(kind: FileKind, path: String?) {
        settings.setFolder(kind, path)
        folders = folders + (kind to path)
        rescan()
    }

    // ----- Hanap -----

    fun findByNumber(n: Int): Song? = byNumber[n]

    fun search(query: String, limit: Int = 300): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return songs.take(limit)
        return songs.asSequence().filter { it.searchKey.contains(q) }.take(limit).toList()
    }

    // ----- Queue -----

    fun enqueue(song: Song) {
        queue.add(song)
    }

    fun removeFromQueue(index: Int) {
        if (index in queue.indices) queue.removeAt(index)
    }

    fun clearQueue() {
        queue.clear()
    }

    // ----- Pagtugtog -----

    fun playNow(song: Song) {
        player.start(song)
    }

    fun playQueueItem(index: Int) {
        if (index !in queue.indices) return
        player.start(queue.removeAt(index))
    }

    /** Susunod sa queue; kung wala na, hihinto ang player. */
    fun playNextFromQueue() {
        if (queue.isNotEmpty()) player.start(queue.removeAt(0)) else player.stop()
    }

    fun skip() = playNextFromQueue()
}
