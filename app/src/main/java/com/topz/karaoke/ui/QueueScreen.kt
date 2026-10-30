package com.topz.karaoke.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topz.karaoke.audio.PlayerStatus

@Composable
fun QueueScreen(vm: KaraokeViewModel) {
    val player = vm.player

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KColors.NavyLight)
                .padding(16.dp)
        ) {
            Text("Ngayong tumutugtog", color = KColors.Gold, fontWeight = FontWeight.Bold)
            val now = player.song
            Text(
                text = now?.title ?: "Wala pa",
                color = KColors.Cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (player.status == PlayerStatus.ERROR) {
                Text(player.errorMessage ?: "May error", color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (now == null) {
                    Button(onClick = { vm.playNextFromQueue() }, enabled = vm.queue.isNotEmpty()) {
                        Text("▶ Simulan ang queue")
                    }
                } else {
                    Button(
                        onClick = { player.togglePlayPause() },
                        enabled = player.status == PlayerStatus.PLAYING || player.status == PlayerStatus.PAUSED
                    ) { Text(if (player.status == PlayerStatus.PLAYING) "⏸" else "▶") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.skip() }) { Text("⏭ Susunod") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { player.stop() }) { Text("⏹") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Susunod (${vm.queue.size})",
                color = KColors.Gold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (vm.queue.isNotEmpty()) {
                TextButton(onClick = { vm.clearQueue() }) { Text("Burahin lahat") }
            }
        }

        if (vm.queue.isEmpty()) {
            Text(
                "Walang kanta sa queue. Pumili sa Library o gamitin ang floating controller.",
                color = KColors.CreamDim,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            itemsIndexed(vm.queue) { index, song ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KColors.NavySurface)
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}.", color = KColors.Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = KColors.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sub = listOfNotNull(
                            song.number?.toString()?.padStart(5, '0'),
                            song.artist
                        ).joinToString(" • ")
                        if (sub.isNotEmpty()) {
                            Text(sub, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                    if (index > 0) {
                        TextButton(onClick = { vm.moveToTop(index) }) { Text("▲") }
                    }
                    TextButton(onClick = { vm.playQueueItem(index) }) { Text("▶") }
                    TextButton(onClick = { vm.removeFromQueue(index) }) { Text("✕") }
                }
            }
        }
    }
}
