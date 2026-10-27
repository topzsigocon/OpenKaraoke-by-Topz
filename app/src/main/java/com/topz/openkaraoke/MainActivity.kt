package com.topz.karaoke

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topz.karaoke.data.ParsedSongName
import com.topz.karaoke.data.SongFileNameParser
import com.topz.karaoke.storage.FileKind
import com.topz.karaoke.storage.FileStorageProvider
import com.topz.karaoke.storage.StorageProvider

class MainActivity : ComponentActivity() {

    private lateinit var storage: StorageProvider
    private var hasAccess by mutableStateOf(false)

    private val legacyPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasAccess = storage.hasAccess() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = FileStorageProvider(applicationContext)
        hasAccess = storage.hasAccess()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    HomeScreen(storage, hasAccess, ::requestAccess)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pagbalik galing sa Settings, i-check ulit ang permission.
        hasAccess = storage.hasAccess()
    }

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            legacyPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}

/** Pansamantalang screen para ma-test ang storage at filename parser. Papalitan ng Library screen. */
@Composable
private fun HomeScreen(storage: StorageProvider, hasAccess: Boolean, onRequestAccess: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("OpenKaraoke 🎤", style = MaterialTheme.typography.headlineSmall)

        if (!hasAccess) {
            Text("Kailangan ng access sa storage para mabasa ang mga kanta sa SD card.")
            Button(onClick = onRequestAccess) { Text("Bigyan ng access") }
            return@Column
        }

        val songs by produceState<List<ParsedSongName>?>(initialValue = null, hasAccess) {
            storage.ensureFolders()
            value = storage.list(FileKind.SONG)
                .map { SongFileNameParser.parse(it.name) }
                .sortedWith(compareBy({ it.number ?: Int.MAX_VALUE }, { it.title }))
        }

        Text("Mga folder: " + storage.appRoots().joinToString { it.absolutePath })
        val list = songs
        if (list == null) {
            Text("Nag-i-scan...")
        } else {
            Text("${list.size} na kanta ang nahanap")
            LazyColumn {
                items(list.take(200)) { s ->
                    val num = s.number?.toString()?.padStart(5, '0') ?: "-----"
                    Text("$num  ${s.title}" + (s.artist?.let { " — $it" } ?: ""))
                }
            }
        }
    }
}
