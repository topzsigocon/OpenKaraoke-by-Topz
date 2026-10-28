package com.topz.karaoke.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topz.karaoke.data.Song

@Composable
fun LibraryScreen(
    vm: KaraokeViewModel,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSong: (Song) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val results = remember(vm.songs, query) { vm.search(query, 300) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))

        if (!vm.hasAccess) {
            NoticeCard(
                message = "Kailangan ng access sa storage para mabasa ang mga kanta sa SD card.",
                buttonText = "Bigyan ng access",
                onClick = onRequestAccess
            )
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Hanapin: numero, pamagat, artist") },
            colors = fieldColors()
        )
        Spacer(Modifier.height(8.dp))

        val zipNote = if (vm.zipSongCount > 0) " (${vm.zipSongCount} mula sa zip)" else ""
        val status = when {
            vm.scanning -> "Nag-i-scan... ${vm.scanProgress} na ang nahanap"
            query.isBlank() ->
                "${vm.songs.size} na kanta$zipNote" +
                    (if (vm.songs.size > results.size) " • unang ${results.size} ang ipinapakita, mag-search para sa iba" else "")
            else -> "${results.size} tugma sa ${vm.songs.size} na kanta"
        }
        Text(status, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        if (!vm.scanning && vm.songs.isEmpty()) {
            NoticeCard(
                message = "Walang nahanap na kanta. Ilagay ang .mid / .kar (o zip na may MIDI) sa folder na " +
                    "OpenKaraoke/songs, o piliin ang sarili mong folder.",
                buttonText = "Pumili ng folder",
                onClick = onOpenSettings
            )
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(results, key = { it.key }) { song ->
                SongRow(song, onOpen = { onOpenSong(song) }) {
                    vm.enqueue(song)
                    Toast.makeText(context, "Naidagdag: ${song.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onOpen: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KColors.NavySurface)
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = song.number?.toString()?.padStart(5, '0') ?: "-----",
            color = KColors.Gold,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = KColors.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (song.artist != null) {
                Text(
                    song.artist,
                    color = KColors.CreamDim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("+ Queue") }
    }
}

@Composable
fun NoticeCard(message: String, buttonText: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KColors.NavySurface)
            .padding(16.dp)
    ) {
        Text(message, color = KColors.Cream)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}
