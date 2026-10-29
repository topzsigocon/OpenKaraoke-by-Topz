package com.topz.karaoke.audio

/** JNI sa TinySoundFont (app/src/main/cpp/synth.c). Dapat tawagin lang sa ilalim ng lock ng AudioEngine. */
object NativeSynth {
    @Volatile
    var available: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("karaoke_synth")
            available = true
        } catch (_: UnsatisfiedLinkError) {
            available = false
        }
    }

    external fun nativeLoad(path: String, sampleRate: Int, gainDb: Float): Boolean
    external fun nativeUnload()
    external fun nativeRender(buffer: ShortArray, frames: Int)
    external fun nativeNoteOn(channel: Int, key: Int, velocity: Int)
    external fun nativeNoteOff(channel: Int, key: Int)
    external fun nativeProgram(channel: Int, program: Int)
    external fun nativeControl(channel: Int, controller: Int, value: Int)
    external fun nativePitchBend(channel: Int, value: Int)
    external fun nativeChannelNotesOff(channel: Int)
    external fun nativeReset()
}

/** Ang ipinapadala ng sequencer. May hiwalay na implementasyon para sa native at para sa mga test. */
interface SynthSink {
    fun noteOn(channel: Int, key: Int, velocity: Int)
    fun noteOff(channel: Int, key: Int)
    fun programChange(channel: Int, program: Int)
    fun controlChange(channel: Int, controller: Int, value: Int)
    fun pitchBend(channel: Int, value: Int)
    fun channelNotesOff(channel: Int)
    fun reset()
}

object NativeSink : SynthSink {
    override fun noteOn(channel: Int, key: Int, velocity: Int) = NativeSynth.nativeNoteOn(channel, key, velocity)
    override fun noteOff(channel: Int, key: Int) = NativeSynth.nativeNoteOff(channel, key)
    override fun programChange(channel: Int, program: Int) = NativeSynth.nativeProgram(channel, program)
    override fun controlChange(channel: Int, controller: Int, value: Int) =
        NativeSynth.nativeControl(channel, controller, value)
    override fun pitchBend(channel: Int, value: Int) = NativeSynth.nativePitchBend(channel, value)
    override fun channelNotesOff(channel: Int) = NativeSynth.nativeChannelNotesOff(channel)
    override fun reset() = NativeSynth.nativeReset()
}
