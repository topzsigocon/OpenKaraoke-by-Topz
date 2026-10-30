package com.topz.karaoke.audio

/** JNI sa app/src/main/cpp/mic.cpp (Oboe): mic -> echo/reverb -> output. */
object MicEngine {
    @Volatile
    var available: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("karaoke_mic")
            available = true
        } catch (_: UnsatisfiedLinkError) {
            available = false
        }
    }

    external fun nativeStart(): Boolean
    external fun nativeStop()
    external fun nativeSetParams(volume: Float, reverb: Float, echo: Float)
}
