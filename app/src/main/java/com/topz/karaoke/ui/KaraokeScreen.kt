package com.topz.karaoke.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.topz.karaoke.audio.PlayerStatus
import com.topz.karaoke.midi.LyricLine
import com.topz.karaoke.midi.Lyrics

/**
 * Karaoke Mode: full-screen na display na parang tunay na karaoke machine.
 * Background video, buong linya ng lyrics (naka-highlight ang kasalukuyang salita), susunod na linya
 * sa ibaba, at ang QUEUE indicator. Ang floating bubble ay nasa ibabaw nito (mula sa KaraokeRoot).
 */
@Composable
fun KaraokeScreen(vm: KaraokeViewModel) {
    val player = vm.player
    val song = player.song
    val lyrics = player.lyrics
    val position = (player.positionMs - vm.lyricsOffsetMs).coerceAtLeast(0L)
    var showQueue by remember { mutableStateOf(false) }

    val showVideo = vm.videoEnabled && vm.videoFiles.isNotEmpty() && song != null &&
        player.status != PlayerStatus.ERROR

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KColors.Navy, KColors.NavyLight)))
    ) {
        if (showVideo) {
            VideoBackground(vm.videoFiles, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }

        Box(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (song == null) {
                CenterMessage("🎤 OpenKaraoke", "Pindutin ang bubble (123) at i-type ang numero ng kanta.")
            } else if (player.status == PlayerStatus.ERROR) {
                CenterMessage("May error", player.errorMessage ?: "")
            } else if (lyrics == null) {
                CenterMessage(song.title, "Nilo-load...")
            } else if (lyrics.lines.isEmpty()) {
                CenterMessage("♪ ${song.title} ♪", "Walang lyrics ang kantang ito.")
            } else {
                KaraokeLyrics(lyrics, position)
            }
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayChip("☰ UI", gold = false) { vm.exitKaraokeMode() }
            Spacer(Modifier.weight(1f))
            OverlayChip("QUEUE: %02d".format(vm.queue.size), gold = true) { showQueue = true }
        }
    }

    if (showQueue) QueuePeekDialog(vm) { showQueue = false }
}

@Composable
private fun KaraokeLyrics(lyrics: Lyrics, position: Long) {
    val current = lyrics.lineIndexAt(position)
    // Bago magsimula, ipinapakita na ang unang linya para makapaghanda ang kumakanta.
    val index = maxOf(current, 0)
    val line = lyrics.lines[index]
    val next = lyrics.lines.getOrNull(index + 1)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        KaraokeLine(line, position, scale = 1f, highlight = current >= 0)
        Spacer(Modifier.height(28.dp))
        if (next != null) KaraokeLine(next, position, scale = 0.6f, highlight = false)
    }
}

/** Buong linya na laging nakikita. Ang mga salitang nakanta na ay ginto; ang iba ay puti. */
@Composable
private fun KaraokeLine(line: LyricLine, position: Long, scale: Float, highlight: Boolean) {
    val chars = line.text.length
    val baseSize = when {
        chars <= 24 -> 40f
        chars <= 40 -> 32f
        else -> 26f
    }
    val text = buildAnnotatedString {
        for (w in line.words) {
            val color = when {
                !highlight -> Color.White.copy(alpha = 0.75f)
                position >= w.timeMs -> KColors.Gold
                else -> Color.White
            }
            withStyle(SpanStyle(color = color)) { append(w.text) }
        }
    }
    Text(
        text = text,
        style = TextStyle(
            fontSize = (baseSize * scale).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            shadow = Shadow(Color.Black, Offset(2f, 2f), 6f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CenterMessage(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            color = Color.White,
            style = TextStyle(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(Color.Black, Offset(2f, 2f), 6f)
            )
        )
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = KColors.CreamDim, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun OverlayChip(text: String, gold: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (gold) KColors.Gold else KColors.Cream,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/** Listahan ng susunod na kanta; puwedeng ilipat sa itaas o alisin. */
@Composable
private fun QueuePeekDialog(vm: KaraokeViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = KColors.NavySurface,
            border = BorderStroke(1.dp, KColors.GoldDim)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Susunod na kanta (${vm.queue.size})",
                    color = KColors.Gold,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (vm.queue.isEmpty()) {
                    Text("Walang nakapila.", color = KColors.CreamDim, modifier = Modifier.padding(vertical = 12.dp))
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    itemsIndexed(vm.queue) { index, song ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", color = KColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = KColors.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = listOfNotNull(song.number?.toString()?.padStart(5, '0'), song.artist)
                                    .joinToString(" • ")
                                if (sub.isNotEmpty()) {
                                    Text(sub, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                            if (index > 0) {
                                TextButton(onClick = { vm.moveToTop(index) }) { Text("▲") }
                            }
                            TextButton(onClick = { vm.removeFromQueue(index) }) { Text("✕") }
                        }
                    }
                }
                TextButton(onClick = onDismiss) { Text("Isara") }
            }
        }
    }
}
