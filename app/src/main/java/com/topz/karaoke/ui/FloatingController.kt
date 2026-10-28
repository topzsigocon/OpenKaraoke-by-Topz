package com.topz.karaoke.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Floating controller: bilog na gintong button na puwedeng hilahin kahit saan.
 * Pindutin para bumuka ang panel na may keypad (numero ng kanta) at paghahanap.
 * Nasa ibabaw ito ng lahat ng screen, kaya magagamit habang kumakanta.
 */
@Composable
fun FloatingController(vm: KaraokeViewModel, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }) {
        if (!expanded) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(KColors.Gold)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offset = offset + drag
                        }
                    }
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Text("123", color = KColors.Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        } else {
            ControllerPanel(
                vm = vm,
                onDrag = { offset = offset + it },
                onClose = { expanded = false }
            )
        }
    }
}

@Composable
private fun ControllerPanel(vm: KaraokeViewModel, onDrag: (Offset) -> Unit, onClose: () -> Unit) {
    var mode by remember { mutableStateOf(0) } // 0 = numero, 1 = hanapin
    var digits by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = KColors.NavySurface,
        border = BorderStroke(1.5.dp, KColors.Gold),
        shadowElevation = 12.dp,
        modifier = Modifier.width(272.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Hawakan: hilahin dito para ilipat ang panel.
            Row(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onDrag(drag)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "☰  Controller (hilahin)",
                    color = KColors.Gold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
                TextButton(onClick = onClose) { Text("✕") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("Numero", mode == 0, Modifier.weight(1f)) { mode = 0 }
                ModeChip("Hanapin", mode == 1, Modifier.weight(1f)) { mode = 1 }
            }
            Spacer(Modifier.height(10.dp))

            if (mode == 0) {
                val match = digits.toIntOrNull()?.let { vm.findByNumber(it) }

                Text(
                    text = if (digits.isEmpty()) "- - -" else digits,
                    color = KColors.Gold,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = when {
                        digits.isEmpty() -> "I-type ang numero ng kanta"
                        match != null -> match.title + (match.artist?.let { " — $it" } ?: "")
                        else -> "Walang kantang may numerong $digits"
                    },
                    color = if (match != null) KColors.Cream else KColors.CreamDim,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("⌫", "0", "OK")
                )
                for (row in rows) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (key in row) {
                            KeyButton(key, accent = key == "OK", modifier = Modifier.weight(1f)) {
                                when (key) {
                                    "⌫" -> {
                                        digits = digits.dropLast(1)
                                        message = null
                                    }
                                    "OK" -> {
                                        if (match != null) {
                                            vm.enqueue(match)
                                            message = "Naidagdag: ${match.title}"
                                            digits = ""
                                        } else {
                                            message = "Walang kantang may ganyang numero"
                                        }
                                    }
                                    else -> {
                                        if (digits.length < 6) digits += key
                                        message = null
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Pamagat, artist, numero") },
                    colors = fieldColors()
                )
                Spacer(Modifier.height(6.dp))
                val results = remember(vm.songs, query) {
                    if (query.isBlank()) emptyList() else vm.search(query, 20)
                }
                LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
                    items(results, key = { it.key }) { song ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = KColors.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(song.number?.toString()?.padStart(5, '0'), song.artist).joinToString(" • "),
                                    color = KColors.CreamDim,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    vm.enqueue(song)
                                    message = "Naidagdag: ${song.title}"
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) { Text("+") }
                        }
                    }
                }
            }

            val msg = message
            if (msg != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    msg,
                    color = KColors.Gold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "Queue: ${vm.queue.size}",
                color = KColors.CreamDim,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) KColors.Gold else KColors.NavyLight)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) KColors.Navy else KColors.Cream,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KeyButton(label: String, accent: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) KColors.Gold else KColors.NavyLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (accent) KColors.Navy else KColors.Cream,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
