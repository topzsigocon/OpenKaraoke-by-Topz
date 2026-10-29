package com.topz.karaoke.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Audio output (AudioTrack) + ang thread na nagpapatakbo ng sequencer at ng synth.
 * Lahat ng tawag sa synth at sequencer ay nasa ilalim ng iisang [lock].
 */
class AudioEngine {

    val sampleRate: Int = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        .let { if (it in 8000..48000) it else 48000 }

    private val lock = ReentrantLock()
    private val wake = lock.newCondition()

    private var sequencer: MidiSequencer? = null
    private var playing = false
    private var released = false
    private var loadedSoundFont: String? = null
    private var thread: Thread? = null

    @Volatile
    private var positionSnapshotMs = 0L

    /** Tinatawag sa audio thread kapag natapos ang kanta. */
    @Volatile
    var onFinished: (() -> Unit)? = null

    val positionMs: Long get() = positionSnapshotMs

    /** Nilo-load ang SoundFont. Mabigat ito (ilang segundo), kaya tawagin sa background thread. */
    fun loadSoundFont(path: String): Boolean = lock.withLock {
        if (loadedSoundFont == path) {
            true
        } else {
            val ok = NativeSynth.available && NativeSynth.nativeLoad(path, sampleRate, GAIN_DB)
            loadedSoundFont = if (ok) path else null
            ok
        }
    }

    fun setSequencer(seq: MidiSequencer?) {
        lock.withLock {
            playing = false
            sequencer = seq
            seq?.seek(0, NativeSink)
            positionSnapshotMs = 0L
        }
    }

    fun play() {
        lock.withLock {
            if (sequencer == null || released) return
            playing = true
            ensureThread()
            wake.signalAll()
        }
    }

    fun pause() {
        lock.withLock { playing = false }
    }

    fun stop() {
        lock.withLock {
            playing = false
            sequencer?.seek(0, NativeSink)
            positionSnapshotMs = 0L
        }
    }

    fun seekMs(ms: Long) {
        lock.withLock {
            val seq = sequencer ?: return
            seq.seek(ms, NativeSink)
            positionSnapshotMs = seq.positionMs
        }
    }

    fun setTranspose(semitones: Int) {
        lock.withLock { sequencer?.transpose = semitones }
    }

    fun setTempoFactor(factor: Double) {
        lock.withLock { sequencer?.tempoFactor = factor }
    }

    fun setMuted(channel: Int, muted: Boolean) {
        lock.withLock { sequencer?.setMuted(channel, muted, NativeSink) }
    }

    fun release() {
        lock.withLock {
            released = true
            playing = false
            wake.signalAll()
        }
        thread?.join(1500)
        lock.withLock {
            if (NativeSynth.available) NativeSynth.nativeUnload()
            loadedSoundFont = null
            sequencer = null
        }
    }

    private fun ensureThread() {
        if (thread?.isAlive == true) return
        thread = Thread({ audioLoop() }, "karaoke-audio").also { it.start() }
    }

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val channelMask = AudioFormat.CHANNEL_OUT_STEREO
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val bufferBytes = maxOf(minBytes, BLOCK_FRAMES * 4 * 4)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        val pcm = ShortArray(BLOCK_FRAMES * 2)
        var started = false
        try {
            while (true) {
                var finished = false
                lock.lock()
                try {
                    while (!released && !(playing && sequencer != null)) {
                        if (started) {
                            track.pause()
                            track.flush()
                            started = false
                        }
                        wake.await()
                    }
                    if (released) break

                    val seq = sequencer!!
                    seq.advance(BLOCK_FRAMES, NativeSink)
                    if (NativeSynth.available) {
                        NativeSynth.nativeRender(pcm, BLOCK_FRAMES)
                    } else {
                        pcm.fill(0)
                    }
                    positionSnapshotMs = seq.positionMs
                    if (seq.isFinished) {
                        finished = true
                        playing = false
                    }
                } finally {
                    lock.unlock()
                }

                if (!started) {
                    track.play()
                    started = true
                }
                track.write(pcm, 0, pcm.size)
                if (finished) onFinished?.invoke()
            }
        } finally {
            try {
                track.stop()
            } catch (_: IllegalStateException) {
            }
            track.release()
        }
    }

    companion object {
        /** Mas malaking block = mas matatag, pero mas magaspang ang oras ng mga note (~21 ms sa 48 kHz). */
        const val BLOCK_FRAMES = 1024

        /** Para hindi mag-clip kapag maraming sabay na note. */
        const val GAIN_DB = -4.0f
    }
}
