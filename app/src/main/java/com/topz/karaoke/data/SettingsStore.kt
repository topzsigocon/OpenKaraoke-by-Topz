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

    /** Pahinga (segundo) bago magsimula ang kanta sa Karaoke Mode. 0 = walang pahinga. */
    fun getIntroSeconds(): Int = prefs.getInt("intro_seconds", 3)

    fun setIntroSeconds(value: Int) {
        prefs.edit().putInt("intro_seconds", value).apply()
    }

    /** Kusang pumapasok sa Karaoke Mode kapag nagsimula ang kanta. Naka-off bilang default. */
    fun getAutoKaraoke(): Boolean = prefs.getBoolean("auto_karaoke", false)

    fun setAutoKaraoke(value: Boolean) {
        prefs.edit().putBoolean("auto_karaoke", value).apply()
    }

    // ---- Pag-customize (default = kasalukuyang ayos) ----
    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == prefs.getString(key, null) } ?: default

    private fun putEnum(key: String, value: Enum<*>) {
        prefs.edit().putString(key, value.name).apply()
    }

    fun getLyricsSize(): LyricsSize = enumPref("lyrics_size", LyricsSize.MEDIUM)
    fun setLyricsSize(v: LyricsSize) = putEnum("lyrics_size", v)

    fun getHighlightColor(): HighlightColor = enumPref("highlight_color", HighlightColor.GOLD)
    fun setHighlightColor(v: HighlightColor) = putEnum("highlight_color", v)

    fun getLyricsCase(): LyricsCase = enumPref("lyrics_case", LyricsCase.ORIGINAL)
    fun setLyricsCase(v: LyricsCase) = putEnum("lyrics_case", v)

    /** Dilim ng takip sa video sa Karaoke Mode (0 hanggang 80). */
    fun getVideoDimPercent(): Int = prefs.getInt("video_dim_percent", 45)
    fun setVideoDimPercent(v: Int) {
        prefs.edit().putInt("video_dim_percent", v).apply()
    }

    fun getPerformanceMode(): PerformanceMode = enumPref("performance_mode", PerformanceMode.HIGH)
    fun setPerformanceMode(v: PerformanceMode) = putEnum("performance_mode", v)

    fun getMuteMelodyByDefault(): Boolean = prefs.getBoolean("mute_melody_default", false)
    fun setMuteMelodyByDefault(v: Boolean) {
        prefs.edit().putBoolean("mute_melody_default", v).apply()
    }

    fun getBubbleSize(): BubbleSize = enumPref("bubble_size", BubbleSize.MEDIUM)
    fun setBubbleSize(v: BubbleSize) = putEnum("bubble_size", v)

    fun getBubbleOpacityPercent(): Int = prefs.getInt("bubble_opacity", 100)
    fun setBubbleOpacityPercent(v: Int) {
        prefs.edit().putInt("bubble_opacity", v).apply()
    }

    fun getBubbleSide(): BubbleSide = enumPref("bubble_side", BubbleSide.RIGHT)
    fun setBubbleSide(v: BubbleSide) = putEnum("bubble_side", v)

    /** "AUTO" o pangalan ng charset (hal. "windows-874"). */
    fun getLyricsEncoding(): String = prefs.getString("lyrics_encoding", "AUTO") ?: "AUTO"
    fun setLyricsEncoding(v: String) {
        prefs.edit().putString("lyrics_encoding", v).apply()
    }
}
