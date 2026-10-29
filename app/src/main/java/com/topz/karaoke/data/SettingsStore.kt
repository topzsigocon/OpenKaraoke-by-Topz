package com.topz.karaoke.data

import android.content.Context
import com.topz.karaoke.storage.FileKind

/** Nagse-save ng mga folder na pinili ng user. Null ang ibig sabihin ay default. */
class SettingsStore(context: Context) {
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
}
