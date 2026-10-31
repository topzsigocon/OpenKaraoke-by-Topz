package com.topz.karaoke.data

import android.content.Context
import com.topz.karaoke.storage.FileKind

/** Nagse-save ng mga folder na pinili ng user. Null ang ibig sabihin ay default. */
class SettingsStore(context: Context) : SongPrefsStore {
    private val prefs = context.getSharedPreferences("openkaraoke", Context.MODE_PRIVATE)

    fun getFolder(kind: FileKind): String? = prefs.getString("folder_" + kind.name, null)

    fun setFolder(kind: FileKind, path: String?) {
        val editor = prefs.edit()
        if (path == null) editor.remove("folder_" + kind.name) else editor.putString("folder_" + kind.name, path)
        editor.apply()
    }

    /** Napiling SoundFont (path). Null = gamitin ang default na kasama sa app. */
    fun getActiveSoundFont(): String? = prefs.getString("active_soundfont", null)

    fun setActiveSoundFont(path: String?) {
        val editor = prefs.edit()
        if (path == null) editor.remove("active_soundfont") else editor.putString("active_soundfont", path)
        editor.apply()
    }

    override fun load(songKey: String): SongPrefs? =
        SongPrefs.decode(prefs.getString("song:$songKey", null))

    override fun save(songKey: String, prefs: SongPrefs) {
        this.prefs.edit().putString("song:$songKey", prefs.encode()).apply()
    }

    /** Background video sa likod ng lyrics. */
    fun getVideoEnabled(): Boolean = prefs.getBoolean("video_enabled", true)

    fun setVideoEnabled(value: Boolean) {
        prefs.edit().putBoolean("video_enabled", value).apply()
    }

    /** Pagkaantala ng lyrics (ms) para tumugma sa narinig na tunog (latency ng audio output). */
    fun getLyricsOffsetMs(): Int = prefs.getInt("lyrics_offset_ms", 100)

    fun setLyricsOffsetMs(value: Int) {
        prefs.edit().putInt("lyrics_offset_ms", value).apply()
    }

    /** Mga antas ng mic (0.0 hanggang 1.0). Hindi sine-save kung naka-on ang mic, para laging naka-off pagbukas. */
    fun getMicVolume(): Float = prefs.getFloat("mic_volume", 0.8f)
    fun getMicReverb(): Float = prefs.getFloat("mic_reverb", 0.3f)
    fun getMicEcho(): Float = prefs.getFloat("mic_echo", 0.2f)

    fun setMicLevels(volume: Float, reverb: Float, echo: Float) {
        prefs.edit()
            .putFloat("mic_volume", volume)
            .putFloat("mic_reverb", reverb)
            .putFloat("mic_echo", echo)
            .apply()
    }

    // ---- Mga option ng Karaoke Mode (default = kasalukuyang ayos) ----
    fun getKaraokeLayout(): KaraokeLayout =
        KaraokeLayout.values().firstOrNull { it.name == prefs.getString("k_layout", null) } ?: KaraokeLayout.DEFAULT

    fun setKaraokeLayout(value: KaraokeLayout) {
        prefs.edit().putString("k_layout", value.name).apply()
    }

    fun getLyricsPosition(): LyricsPosition =
        LyricsPosition.values().firstOrNull { it.name == prefs.getString("k_position", null) } ?: LyricsPosition.CENTER

    fun setLyricsPosition(value: LyricsPosition) {
        prefs.edit().putString("k_position", value.name).apply()
    }

    fun getLyricsAlignment(): LyricsAlignment =
        LyricsAlignment.values().firstOrNull { it.name == prefs.getString("k_alignment", null) } ?: LyricsAlignment.CENTERED

    fun setLyricsAlignment(value: LyricsAlignment) {
        prefs.edit().putString("k_alignment", value.name).apply()
    }
}
