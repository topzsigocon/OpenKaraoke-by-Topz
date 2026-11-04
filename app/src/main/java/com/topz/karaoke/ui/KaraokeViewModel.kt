package com.topz.karaoke.ui

import com.topz.karaoke.data.SongSorting
import com.topz.karaoke.data.SongSort
import com.topz.karaoke.data.RepeatMode
import com.topz.karaoke.data.OrientationLock
import com.topz.karaoke.data.LyricsOutline
import com.topz.karaoke.data.BackupManager
import android.app.Application
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topz.karaoke.audio.DefaultSoundFont
import com.topz.karaoke.audio.MicEngine
import com.topz.karaoke.audio.Player
import com.topz.karaoke.data.BubbleSide
import com.topz.karaoke.data.BubbleSize
import com.topz.karaoke.data.HighlightColor
import com.topz.karaoke.data.IndexCache
import com.topz.karaoke.data.KaraokeLayout
import com.topz.karaoke.data.LyricsAlignment
import com.topz.karaoke.data.LyricsCase
import com.topz.karaoke.data.LyricsSize
import com.topz.karaoke.data.PerformanceMode
import com.topz.karaoke.data.LyricsPosition
import com.topz.karaoke.data.SettingsStore
import com.topz.karaoke.data.Song
import com.topz.karaoke.data.SongLists
import com.topz.karaoke.data.SongFileNameParser
import com.topz.karaoke.data.ZipSongScanner
import com.topz.karaoke.storage.FileKind
import com.topz.karaoke.storage.FileStorageProvider
import com.topz.karaoke.storage.StorageProvider
import com.topz.karaoke.storage.StoredFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

class KaraokeViewModel(app: Application) : AndroidViewModel(app) {

    private val storage: StorageProvider = FileStorageProvider(app)
    private val settings = SettingsStore(app)
    private val cacheFile = File(app.filesDir, "song_index.tsv")
    private val queueFile = File(app.filesDir, "queue.tsv")
    private val queueSaveLock = Mutex()
    private val historyFile = File(app.filesDir, "history.tsv")
    private val favoritesFile = File(app.filesDir, "favorites.tsv")

    /** Ang audio player (SoundFont synth). */
    val player = Player(viewModelScope, app.getSystemService(AudioManager::class.java), settings,
        { muteMelodyDefault }, { lyricsCharset() }) { resolveSoundFont() }

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
    var videoFiles by mutableStateOf<List<StoredFile>>(emptyList())
        private set
    var videoEnabled by mutableStateOf(settings.getVideoEnabled())
        private set
    var lyricsOffsetMs by mutableStateOf(settings.getLyricsOffsetMs())
        private set
    var introSeconds by mutableStateOf(settings.getIntroSeconds())
        private set
    var autoKaraoke by mutableStateOf(settings.getAutoKaraoke())
        private set

    // ----- Pag-customize -----
    var lyricsSize by mutableStateOf(settings.getLyricsSize())
        private set
    var highlightColor by mutableStateOf(settings.getHighlightColor())
        private set
    var lyricsCase by mutableStateOf(settings.getLyricsCase())
        private set
    var videoDimPercent by mutableStateOf(settings.getVideoDimPercent())
        private set
    var performanceMode by mutableStateOf(settings.getPerformanceMode())
        private set
    var muteMelodyDefault by mutableStateOf(settings.getMuteMelodyByDefault())
        private set
    var bubbleSize by mutableStateOf(settings.getBubbleSize())
        private set
    var bubbleOpacityPercent by mutableStateOf(settings.getBubbleOpacityPercent())
        private set
    var bubbleSide by mutableStateOf(settings.getBubbleSide())
        private set
    var lyricsEncoding by mutableStateOf(settings.getLyricsEncoding())
        private set
    var lyricsOutline by mutableStateOf(settings.getLyricsOutline())
        private set
    var repeatMode by mutableStateOf(settings.getRepeatMode())
        private set
    var orientationLock by mutableStateOf(settings.getOrientationLock())
        private set
    var songSort by mutableStateOf(settings.getSongSort())
        private set
    var showQueueIndicator by mutableStateOf(settings.getShowQueueIndicator())
        private set
    var showTopTitle by mutableStateOf(settings.getShowTopTitle())
        private set
    var topTitleNumber by mutableStateOf(settings.getTopTitleNumber())
        private set
    var countdownDots by mutableStateOf(settings.getCountdownDots())
        private set
    var autoPlayFavorites by mutableStateOf(settings.getAutoPlayFavorites())
        private set
    var keypadHaptics by mutableStateOf(settings.getKeypadHaptics())
        private set
    var padNumbers by mutableStateOf(settings.getPadNumbers())
        private set
    var keypadAutoDigits by mutableStateOf(settings.getKeypadAutoDigits())
        private set
    var bubbleFadeSeconds by mutableStateOf(settings.getBubbleFadeSeconds())
        private set
    var masterGainDb by mutableStateOf(settings.getMasterGainDb())
        private set
    var backupMessage by mutableStateOf<String?>(null)
        private set

    /** Sa Economy mode, hindi ginagamit ang background video. */
    val videoActive: Boolean
        get() = videoEnabled && performanceMode != PerformanceMode.ECONOMY

    val karaokeStyle: KaraokeStyle
        get() = KaraokeStyle(lyricsSize.scale(), highlightColor.toColor(), lyricsCase, lyricsOutline)

    /** Piniling charset ng lyrics; null = awtomatiko. */
    fun lyricsCharset(): Charset? =
        if (lyricsEncoding == "AUTO") null else try { Charset.forName(lyricsEncoding) } catch (_: Exception) { null }

    /** Karaoke Mode: nakatago ang normal na UI. Hindi apektado ang pagtugtog. */
    var karaokeMode by mutableStateOf(false)
        private set
    var karaokeLayout by mutableStateOf(settings.getKaraokeLayout())
        private set
    var lyricsPosition by mutableStateOf(settings.getLyricsPosition())
        private set
    var lyricsAlignment by mutableStateOf(settings.getLyricsAlignment())
        private set

    // ----- Mic echo/reverb -----
    var micEnabled by mutableStateOf(false)
        private set
    var micError by mutableStateOf<String?>(null)
        private set
    var micVolume by mutableStateOf(settings.getMicVolume())
        private set
    var micReverb by mutableStateOf(settings.getMicReverb())
        private set
    var micEcho by mutableStateOf(settings.getMicEcho())
        private set
    private var micResumeOnStart = false
    var folders by mutableStateOf<Map<FileKind, String?>>(
        FileKind.values().associateWith { settings.getFolder(it) }
    )
        private set

    val queue = mutableStateListOf<Song>()

    /** Mga kinanta kamakailan (pinakabago ang una) at mga paborito. Naka-save sa file. */
    var history by mutableStateOf<List<Song>>(emptyList())
        private set
    var favorites by mutableStateOf<List<Song>>(emptyList())
        private set
    var favoriteKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    private var byNumber: Map<Int, Song> = emptyMap()
    private var started = false

    init {
        player.onSongFinished = { finished -> handleSongFinished(finished) }
        player.applyVoiceLimit(performanceMode.voices)
        player.applyGain(masterGainDb)
        SongNumberFormat.padded = padNumbers
        // Ibalik ang queue na na-save noong nakaraang gamit
        IndexCache.load(queueFile, "queue")?.let { queue.addAll(it) }
        IndexCache.load(historyFile, "history")?.let { history = it }
        IndexCache.load(favoritesFile, "favorites")?.let { list ->
            favorites = list
            favoriteKeys = list.map { it.key }.toSet()
        }
    }

    private fun persistQueue() {
        val snapshot = queue.toList()
        viewModelScope.launch(Dispatchers.IO) {
            queueSaveLock.withLock { IndexCache.save(queueFile, "queue", snapshot) }
        }
    }

    private fun pushMicLevels() {
        if (MicEngine.available) MicEngine.nativeSetParams(micVolume, micReverb, micEcho)
    }

    fun enableMic() {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                if (!MicEngine.available) {
                    false
                } else {
                    pushMicLevels()
                    MicEngine.nativeStart()
                }
            }
            micEnabled = ok
            micError = when {
                ok -> null
                !MicEngine.available -> "Hindi ma-load ang mic engine."
                else -> "Hindi mabuksan ang mic. Tiyaking may permission at walang ibang app na gumagamit nito."
            }
        }
    }

    fun disableMic() {
        if (MicEngine.available) MicEngine.nativeStop()
        micEnabled = false
        micError = null
    }

    fun changeMicVolume(value: Float) {
        micVolume = value
        pushMicLevels()
        settings.setMicLevels(micVolume, micReverb, micEcho)
    }

    fun changeMicReverb(value: Float) {
        micReverb = value
        pushMicLevels()
        settings.setMicLevels(micVolume, micReverb, micEcho)
    }

    fun changeMicEcho(value: Float) {
        micEcho = value
        pushMicLevels()
        settings.setMicLevels(micVolume, micReverb, micEcho)
    }

    /** Tinatawag kapag napunta sa background ang app: bitawan ang mic. */
    fun onAppStopped() {
        if (micEnabled) {
            disableMic()
            micResumeOnStart = true
        }
    }

    fun chooseKaraokeLayout(value: KaraokeLayout) {
        karaokeLayout = value
        settings.setKaraokeLayout(value)
    }

    fun chooseLyricsPosition(value: LyricsPosition) {
        lyricsPosition = value
        settings.setLyricsPosition(value)
    }

    fun chooseLyricsAlignment(value: LyricsAlignment) {
        lyricsAlignment = value
        settings.setLyricsAlignment(value)
    }

    fun chooseLyricsSize(value: LyricsSize) {
        lyricsSize = value
        settings.setLyricsSize(value)
    }

    fun chooseHighlightColor(value: HighlightColor) {
        highlightColor = value
        settings.setHighlightColor(value)
    }

    fun chooseLyricsCase(value: LyricsCase) {
        lyricsCase = value
        settings.setLyricsCase(value)
    }

    fun changeVideoDim(percent: Int) {
        videoDimPercent = percent.coerceIn(0, 80)
        settings.setVideoDimPercent(videoDimPercent)
    }

    fun choosePerformanceMode(value: PerformanceMode) {
        performanceMode = value
        settings.setPerformanceMode(value)
        player.applyVoiceLimit(value.voices)
    }

    fun toggleMuteMelodyDefault() {
        muteMelodyDefault = !muteMelodyDefault
        settings.setMuteMelodyByDefault(muteMelodyDefault)
    }

    fun chooseBubbleSize(value: BubbleSize) {
        bubbleSize = value
        settings.setBubbleSize(value)
    }

    fun changeBubbleOpacity(percent: Int) {
        bubbleOpacityPercent = percent.coerceIn(30, 100)
        settings.setBubbleOpacityPercent(bubbleOpacityPercent)
    }

    fun chooseBubbleSide(value: BubbleSide) {
        bubbleSide = value
        settings.setBubbleSide(value)
    }

    fun chooseLyricsEncoding(name: String) {
        lyricsEncoding = name
        settings.setLyricsEncoding(name)
    }

    // ----- Mga dagdag na setting -----

    fun chooseLyricsOutline(value: LyricsOutline) {
        lyricsOutline = value
        settings.setLyricsOutline(value)
    }

    fun chooseRepeatMode(value: RepeatMode) {
        repeatMode = value
        settings.setRepeatMode(value)
    }

    fun chooseOrientationLock(value: OrientationLock) {
        orientationLock = value
        settings.setOrientationLock(value)
    }

    fun chooseSongSort(value: SongSort) {
        songSort = value
        settings.setSongSort(value)
        viewModelScope.launch {
            songs = withContext(Dispatchers.Default) { SongSorting.sort(songs, value) }
        }
    }

    fun toggleShowQueueIndicator() {
        showQueueIndicator = !showQueueIndicator
        settings.setShowQueueIndicator(showQueueIndicator)
    }

    fun toggleShowTopTitle() {
        showTopTitle = !showTopTitle
        settings.setShowTopTitle(showTopTitle)
    }

    fun toggleTopTitleNumber() {
        topTitleNumber = !topTitleNumber
        settings.setTopTitleNumber(topTitleNumber)
    }

    fun toggleCountdownDots() {
        countdownDots = !countdownDots
        settings.setCountdownDots(countdownDots)
    }

    fun toggleAutoPlayFavorites() {
        autoPlayFavorites = !autoPlayFavorites
        settings.setAutoPlayFavorites(autoPlayFavorites)
    }

    fun toggleKeypadHaptics() {
        keypadHaptics = !keypadHaptics
        settings.setKeypadHaptics(keypadHaptics)
    }

    fun togglePadNumbers() {
        padNumbers = !padNumbers
        SongNumberFormat.padded = padNumbers
        settings.setPadNumbers(padNumbers)
    }

    fun chooseKeypadAutoDigits(value: Int) {
        keypadAutoDigits = value
        settings.setKeypadAutoDigits(value)
    }

    fun chooseBubbleFadeSeconds(value: Int) {
        bubbleFadeSeconds = value
        settings.setBubbleFadeSeconds(value)
    }

    fun changeMasterGain(db: Float) {
        masterGainDb = db.coerceIn(-16f, 4f)
        player.applyGain(masterGainDb)
        settings.setMasterGainDb(masterGainDb)
    }

    fun applyMicPreset(reverb: Float, echo: Float) {
        micReverb = reverb
        micEcho = echo
        pushMicLevels()
        settings.setMicLevels(micVolume, micReverb, micEcho)
    }

    fun shuffleQueue() {
        if (queue.size > 1) {
            queue.shuffle()
            persistQueue()
        }
    }

    /** Pagkatapos ng kanta: repeat (isa o buong queue), party mode (random na paborito), o susunod sa queue. */
    private fun handleSongFinished(finished: Song) {
        when (repeatMode) {
            RepeatMode.ONE -> {
                startSong(finished)
                return
            }
            RepeatMode.QUEUE -> {
                queue.add(finished)
                persistQueue()
            }
            RepeatMode.OFF -> Unit
        }
        if (queue.isEmpty() && autoPlayFavorites && favorites.isNotEmpty()) {
            val pool = favorites.filter { it.key != finished.key }.ifEmpty { favorites }
            startSong(pool.random())
        } else {
            playNextFromQueue()
        }
    }

    // ----- Backup at restore -----

    private fun backupFileIn(root: File) = File(root, "OpenKaraoke/openkaraoke-backup.zip")

    fun backupNow() {
        viewModelScope.launch {
            val roots = storage.volumeRoots()
            if (roots.isEmpty() || !storage.hasAccess()) {
                backupMessage = "Kailangan ng access sa storage para mag-backup."
                return@launch
            }
            val target = backupFileIn(roots.first())
            val error = withContext(Dispatchers.IO) { BackupManager.export(getApplication(), target) }
            backupMessage = if (error == null) "Na-backup: ${target.absolutePath}" else "Hindi nabackup: $error"
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            val source = storage.volumeRoots().map { backupFileIn(it) }.filter { it.isFile }.maxByOrNull { it.lastModified() }
            if (source == null) {
                backupMessage = "Walang nahanap na backup (OpenKaraoke/openkaraoke-backup.zip)."
                return@launch
            }
            val error = withContext(Dispatchers.IO) { BackupManager.import(getApplication(), source) }
            backupMessage = if (error == null) {
                "Na-restore mula sa ${source.absolutePath}. Isara at buksan ulit ang app para magamit ang mga ito."
            } else {
                "Hindi na-restore: $error"
            }
        }
    }

    fun toggleAutoKaraoke() {
        autoKaraoke = !autoKaraoke
        settings.setAutoKaraoke(autoKaraoke)
    }

    fun changeIntroSeconds(delta: Int) {
        introSeconds = (introSeconds + delta).coerceIn(0, 10)
        settings.setIntroSeconds(introSeconds)
    }

    /** Sa Karaoke Mode, may pahinga muna (at ipinapakita ang pamagat) bago magsimula ang kanta. */
    private fun startSong(song: Song) {
        // Kung naka-on ang awtomatikong Karaoke Mode, pumasok bago magsimula para lumabas ang pahinga at pamagat.
        if (autoKaraoke) karaokeMode = true
        recordHistory(song)
        player.start(song, if (karaokeMode) introSeconds * 1000L else 0L)
    }

    // ----- History at Favorites -----

    private fun persistList(file: File, signature: String, list: List<Song>) {
        viewModelScope.launch(Dispatchers.IO) {
            queueSaveLock.withLock { IndexCache.save(file, signature, list) }
        }
    }

    private fun recordHistory(song: Song) {
        val updated = SongLists.pushFront(history, song, 100)
        history = updated
        persistList(historyFile, "history", updated)
    }

    fun clearHistory() {
        history = emptyList()
        persistList(historyFile, "history", emptyList())
    }

    fun isFavorite(song: Song): Boolean = song.key in favoriteKeys

    fun toggleFavorite(song: Song) {
        val updated = SongLists.toggle(favorites, song)
        favorites = updated
        favoriteKeys = updated.map { it.key }.toSet()
        persistList(favoritesFile, "favorites", updated)
    }

    fun enterKaraokeMode() {
        karaokeMode = true
    }

    fun exitKaraokeMode() {
        karaokeMode = false
    }

    fun toggleKaraokeMode() {
        karaokeMode = !karaokeMode
    }

    fun toggleVideo() {
        videoEnabled = !videoEnabled
        settings.setVideoEnabled(videoEnabled)
    }

    fun changeLyricsOffset(deltaMs: Int) {
        lyricsOffsetMs = (lyricsOffsetMs + deltaMs).coerceIn(-500, 1000)
        settings.setLyricsOffsetMs(lyricsOffsetMs)
    }

    override fun onCleared() {
        if (MicEngine.available) MicEngine.nativeStop()
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
        if (micResumeOnStart) {
            micResumeOnStart = false
            enableMic()
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
        songs = if (songSort == SongSort.NUMBER) list else SongSorting.sort(list, songSort)
        zipSongCount = list.count { it.entry != null }
    }

    private suspend fun refreshCounts() {
        val fonts = listFor(FileKind.SOUNDFONT)
        soundFontFiles = fonts
        soundFontCount = fonts.size
        val videos = listFor(FileKind.VIDEO)
        videoFiles = videos
        videoCount = videos.size
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

    fun search(query: String, limit: Int = 300, source: List<Song> = songs): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return source.take(limit)
        return source.asSequence().filter { it.searchKey.contains(q) }.take(limit).toList()
    }

    // ----- Queue -----

    fun enqueue(song: Song) {
        queue.add(song)
        persistQueue()
    }

    fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            queue.removeAt(index)
            persistQueue()
        }
    }

    /** Ilipat sa itaas ng queue (susunod na tutugtog). */
    fun moveToTop(index: Int) {
        if (index in 1 until queue.size) {
            val song = queue.removeAt(index)
            queue.add(0, song)
            persistQueue()
        }
    }

    fun clearQueue() {
        queue.clear()
        persistQueue()
    }

    // ----- Pagtugtog -----

    fun playNow(song: Song) {
        startSong(song)
    }

    fun playQueueItem(index: Int) {
        if (index !in queue.indices) return
        val song = queue.removeAt(index)
        persistQueue()
        startSong(song)
    }

    /** Susunod sa queue; kung wala na, hihinto ang player. */
    fun playNextFromQueue() {
        if (queue.isNotEmpty()) {
            val song = queue.removeAt(0)
            persistQueue()
            startSong(song)
        } else {
            player.stop()
        }
    }

    fun skip() = playNextFromQueue()
}
