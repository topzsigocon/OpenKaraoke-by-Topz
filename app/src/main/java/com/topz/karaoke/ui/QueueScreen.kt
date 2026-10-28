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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun QueueScreen(vm: KaraokeViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))

        // Placeholder: darating ang player sa susunod na yugto.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KColors.NavyLight)
                .padding(16.dp)
        ) {
            Text("Ngayong tumutugtog", color = KColors.Gold, fontWeight = FontWeight.Bold)
            val next = vm.queue.firstOrNull()
            Text(
                text = next?.title ?: "Wala pa. (Darating ang player sa susunod na yugto.)",
                color = KColors.Cream
            )
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
            contentPadding = PaddingValues(bottom = 96.dp)
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
                    TextButton(onClick = { vm.removeFromQueue(index) }) { Text("✕") }
                }
            }
        }
    }
}
