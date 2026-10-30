package com.topz.karaoke.audio

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.topz.karaoke.data.Song
import com.topz.karaoke.data.SongReader
import com.topz.karaoke.midi.Lyrics
import com.topz.karaoke.midi.LyricsExtractor
import com.topz.karaoke.midi.MidiParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PlayerStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

/** Ang estado at mga kontrol ng pagtugtog para sa UI. Gamitin lang sa main thread. */
class Player(
    private val scope: CoroutineScope,
    private val audioManager: AudioManager,
    /** Tinatawag sa background thread; ibinabalik ang path ng SoundFont na gagamitin. */
    private val soundFontProvider: () -> String?
) {
    private val engine = AudioEngine()
    private val mainHandler = Handler(Looper.getMainLooper())

    var status by mutableStateOf(PlayerStatus.IDLE)
        private set
    var song by mutableStateOf<Song?>(null)
        private set
    var lyrics by mutableStateOf<Lyrics?>(null)
        private set
    var channels by mutableStateOf<List<ChannelInfo>>(emptyList())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var transpose by mutableStateOf(0)
        private set
    var tempoPercent by mutableStateOf(100)
        private set
    var mutedChannels by mutableStateOf<Set<Int>>(emptySet())
        private set

    /** Tinatawag kapag natapos ang kanta (para sa susunod sa queue). */
    var onSongFinished: (() -> Unit)? = null

    private var loadJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    /** Titigil ang kanta kapag may tumawag o ibang app na tumutugtog; magpapatuloy kapag bumalik. */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                pauseForFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = status == PlayerStatus.PLAYING
                pauseForFocus()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    if (status == PlayerStatus.PAUSED) togglePlayPause()
                }
            }
            else -> Unit
        }
    }

    private fun pauseForFocus() {
        if (status == PlayerStatus.PLAYING) togglePlayPause()
    }

    private fun requestFocus() {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        resumeOnFocusGain = false
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }
    private var ticker: Job? = null

    private class Prepared(val sequencer: MidiSequencer, val lyrics: Lyrics)

    init {
        engine.onFinished = { mainHandler.post { handleFinished() } }
    }

    fun start(song: Song) {
        loadJob?.cancel()
        ticker?.cancel()
        engine.stop()

        this.song = song
        status = PlayerStatus.LOADING
        errorMessage = null
        positionMs = 0L
        durationMs = 0L
        lyrics = null
        channels = emptyList()

        loadJob = scope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) { prepare(song) }
                engine.setSequencer(prepared.sequencer)
                lyrics = prepared.lyrics
                durationMs = prepared.sequencer.durationMs
                channels = prepared.sequencer.channelInfos
                // Ilapat ang mga setting (key, tempo, mute) na pinili na ng user
                engine.setTranspose(transpose)
                engine.setTempoFactor(tempoPercent / 100.0)
                for (ch in mutedChannels) engine.setMuted(ch, true)
                requestFocus()
                engine.play()
                status = PlayerStatus.PLAYING
                startTicker()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = PlayerStatus.ERROR
                errorMessage = e.message ?: e.javaClass.simpleName
            }
        }
    }

    private fun prepare(song: Song): Prepared {
        val midi = MidiParser.parse(SongReader.read(song))
        val lyrics = LyricsExtractor.extract(midi)
        if (!NativeSynth.available) {
            throw IllegalStateException("Hindi ma-load ang audio engine (native library).")
        }
        val soundFont = soundFontProvider()
            ?: throw IllegalStateException(
                "Walang SoundFont. Ilagay ang .sf2 sa folder na soundfonts at piliin ito sa Settings."
            )
        if (!engine.loadSoundFont(soundFont)) {
            throw IllegalStateException("Hindi mabasa ang SoundFont: ${File(soundFont).name}")
        }
        return Prepared(MidiSequencer(midi, engine.sampleRate), lyrics)
    }

    fun togglePlayPause() {
        when (status) {
            PlayerStatus.PLAYING -> {
                engine.pause()
                ticker?.cancel()
                status = PlayerStatus.PAUSED
            }
            PlayerStatus.PAUSED -> {
                requestFocus()
                engine.play()
                status = PlayerStatus.PLAYING
                startTicker()
            }
            else -> Unit
        }
    }

    /** Humihinto at nililinis ang kasalukuyang kanta. */
    fun stop() {
        loadJob?.cancel()
        ticker?.cancel()
        engine.stop()
        abandonFocus()
        status = PlayerStatus.IDLE
        song = null
        lyrics = null
        channels = emptyList()
        positionMs = 0L
        durationMs = 0L
        errorMessage = null
    }

    fun seekTo(ms: Long) {
        if (status != PlayerStatus.PLAYING && status != PlayerStatus.PAUSED) return
        val target = ms.coerceIn(0L, durationMs)
        engine.seekMs(target)
        positionMs = target
    }

    fun changeTranspose(delta: Int) {
        transpose = (transpose + delta).coerceIn(-12, 12)
        engine.setTranspose(transpose)
    }

    fun changeTempo(deltaPercent: Int) {
        tempoPercent = (tempoPercent + deltaPercent).coerceIn(50, 150)
        engine.setTempoFactor(tempoPercent / 100.0)
    }

    fun toggleMute(channel: Int) {
        val nowMuted = channel !in mutedChannels
        mutedChannels = if (nowMuted) mutedChannels + channel else mutedChannels - channel
        engine.setMuted(channel, nowMuted)
    }

    fun release() {
        abandonFocus()
        loadJob?.cancel()
        ticker?.cancel()
        engine.onFinished = null
        engine.release()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && status == PlayerStatus.PLAYING) {
                positionMs = engine.positionMs
                delay(33)
            }
        }
    }

    private fun handleFinished() {
        if (status != PlayerStatus.PLAYING) return
        abandonFocus()
        ticker?.cancel()
        positionMs = 0L
        status = PlayerStatus.IDLE
        song = null
        lyrics = null
        channels = emptyList()
        onSongFinished?.invoke()
    }
}
