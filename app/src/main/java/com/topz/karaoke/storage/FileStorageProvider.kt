package com.topz.karaoke.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileStorageProvider(private val context: Context) : StorageProvider {

    override fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Nakukuha ang root ng bawat volume (kasama ang SD card) mula sa
     * getExternalFilesDirs. Walang permission na kailangan para dito, at
     * null ang entry kapag wala o tinanggal ang card.
     */
    override fun volumeRoots(): List<File> =
        context.getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull { dir ->
                val p = dir.absolutePath
                val i = p.indexOf("/Android/data")
                if (i > 0) File(p.substring(0, i)) else null
            }
            .distinct()

    override fun appRoots(): List<File> = volumeRoots().map { File(it, ROOT_FOLDER) }

    override fun ensureFolders() {
        for (root in appRoots()) {
            for (kind in FileKind.values()) {
                try {
                    File(root, kind.folder).mkdirs()
                } catch (_: SecurityException) {
                    // Walang access sa volume na ito; laktawan.
                }
            }
        }
    }

    override suspend fun list(kind: FileKind, onProgress: (Int) -> Unit): List<StoredFile> =
        withContext(Dispatchers.IO) {
            appRoots().flatMap { root -> scan(File(root, kind.folder), kind, onProgress) }
                .sortedBy { it.name.lowercase() }
        }

    override suspend fun listIn(dir: File, kind: FileKind, onProgress: (Int) -> Unit): List<StoredFile> =
        withContext(Dispatchers.IO) { scan(dir, kind, onProgress).sortedBy { it.name.lowercase() } }

    private fun scan(dir: File, kind: FileKind, onProgress: (Int) -> Unit): List<StoredFile> {
        val out = ArrayList<StoredFile>()
        try {
            for (f in dir.walkTopDown().onFail { _, _ -> }) {
                if (f.isFile &&
                    !f.name.startsWith(".") && // iwas ._file na gawa ng macOS
                    f.extension.lowercase() in kind.extensions
                ) {
                    out.add(StoredFile(f.absolutePath, f.name, 0L))
                    if (out.size % 500 == 0) onProgress(out.size)
                }
            }
        } catch (_: SecurityException) {
            // Walang access sa folder na ito.
        }
        onProgress(out.size)
        return out
    }

    companion object {
        const val ROOT_FOLDER = "OpenKaraoke"
    }
}
