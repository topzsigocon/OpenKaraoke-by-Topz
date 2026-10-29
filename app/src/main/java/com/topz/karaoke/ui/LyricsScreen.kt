package com.topz.karaoke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topz.karaoke.audio.Player
import com.topz.karaoke.audio.PlayerStatus
import com.topz.karaoke.data.Song
import com.topz.karaoke.data.SongReader
import com.topz.karaoke.midi.LyricLine
import com.topz.karaoke.midi.Lyrics
import com.topz.karaoke.midi.LyricsExtractor
import com.topz.karaoke.midi.MidiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed class LoadState {
    object Loading : LoadState()
    class Failed(val message: String) : LoadState()
    class Ready(val lyrics: Lyrics, val trackCount: Int, val format: Int) : LoadState()
}

/**
 * Lyrics ng kanta. Kapag ang kanta ay ang kasalukuyang tumutugtog, may mga kontrol (play/pause,
 * seek, key, tempo, mute ng channel) at sumasabay ang pag-highlight sa tunog.
 */
@Composable
fun LyricsScreen(song: Song, vm: KaraokeViewModel, onBack: () -> Unit) {
    val player = vm.player
    val isCurrent = player.song?.key == song.key
    var state by remember(song.key) { mutableStateOf<LoadState>(LoadState.Loading) }

    LaunchedEffect(song.key) {
        state = withContext<LoadState>(Dispatchers.IO) {
            try {
                val midi = MidiParser.parse(SongReader.read(song))
                LoadState.Ready(LyricsExtractor.extract(midi), midi.tracks.size, midi.format)
            } catch (e: Exception) {
                LoadState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Bumalik") }
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.enqueue(song) }) { Text("+ Queue") }
        }
        Text(
            song.title,
            color = KColors.Gold,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        val sub = listOfNotNull(
            song.number?.toString()?.padStart(5, '0'),
            song.artist,
            if (song.entry != null) "mula sa zip" else null
        ).joinToString(" • ")
        if (sub.isNotEmpty()) {
            Text(sub, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))

        when (val s = state) {
            is LoadState.Loading -> Text("Binabasa ang kanta...", color = KColors.CreamDim)
            is LoadState.Failed -> NoticeCard(
                message = "Hindi mabasa ang kanta: ${s.message}",
                buttonText = "Bumalik",
                onClick = onBack
            )
            is LoadState.Ready -> LyricsView(s, song, vm, isCurrent)
        }
    }
}

@Composable
private fun LyricsView(ready: LoadState.Ready, song: Song, vm: KaraokeViewModel, isCurrent: Boolean) {
    val player = vm.player
    val lyrics = ready.lyrics
    val position = if (isCurrent) player.positionMs else 0L
    val running = isCurrent && player.status == PlayerStatus.PLAYING
    val current = lyrics.lineIndexAt(position)
    val listState = rememberLazyListState()

    LaunchedEffect(current, running) {
        if (running && current >= 0) listState.animateScrollToItem(maxOf(current - 1, 0))
    }

    val sourceLabel = when (lyrics.source) {
        Lyrics.Source.LYRIC_EVENTS -> "lyric events"
        Lyrics.Source.TEXT_EVENTS -> "text events (KAR)"
        Lyrics.Source.NONE -> "walang lyrics"
    }
    Text(
        "Haba ${formatMs(lyrics.durationMs)} • ${lyrics.lines.size} linya • $sourceLabel • " +
            "${ready.trackCount} track (format ${ready.format})",
        color = KColors.CreamDim,
        style = MaterialTheme.typography.bodySmall
    )
    if (lyrics.info.isNotEmpty()) {
        Text(lyrics.info.joinToString(" • "), color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))

    if (isCurrent) {
        PlayerControls(player)
    } else {
        Button(onClick = { vm.playNow(song) }) { Text("▶ I-play ngayon") }
    }
    Spacer(Modifier.height(8.dp))

    if (lyrics.lines.isEmpty()) {
        Text(
            "Walang lyrics na nakita sa MIDI na ito. (Kailangan ng Lyric o Text events.)",
            color = KColors.Cream
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            // Ang oras ay ipinapasa lang sa kasalukuyang linya para hindi mag-recompose ang iba.
            LyricLineView(line, index, current, if (index == current) position else 0L)
        }
    }
}

@Composable
private fun PlayerControls(player: Player) {
    val status = player.status

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { player.togglePlayPause() },
            enabled = status == PlayerStatus.PLAYING || status == PlayerStatus.PAUSED
        ) { Text(if (status == PlayerStatus.PLAYING) "⏸" else "▶") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { player.stop() }) { Text("⏹") }
        Spacer(Modifier.width(12.dp))
        Text(
            text = when (status) {
                PlayerStatus.LOADING -> "Nilo-load ang SoundFont..."
                PlayerStatus.ERROR -> "May error"
                else -> "${formatMs(player.positionMs)} / ${formatMs(player.durationMs)}"
            },
            color = KColors.Gold,
            fontWeight = FontWeight.Bold
        )
    }

    if (status == PlayerStatus.ERROR) {
        Text(player.errorMessage ?: "", color = KColors.Cream, modifier = Modifier.padding(top = 8.dp))
    }

    if (status == PlayerStatus.PLAYING || status == PlayerStatus.PAUSED) {
        var dragging by remember { mutableStateOf<Float?>(null) }
        val duration = player.durationMs.coerceAtLeast(1L)
        Slider(
            value = dragging ?: (player.positionMs.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { player.seekTo((it * duration).toLong()) }
                dragging = null
            },
            colors = SliderDefaults.colors(
                thumbColor = KColors.Gold,
                activeTrackColor = KColors.Gold,
                inactiveTrackColor = KColors.NavyLight
            )
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stepper(
                label = "Key",
                value = (if (player.transpose > 0) "+" else "") + player.transpose,
                onMinus = { player.changeTranspose(-1) },
                onPlus = { player.changeTranspose(1) }
            )
            Stepper(
                label = "Tempo",
                value = "${player.tempoPercent}%",
                onMinus = { player.changeTempo(-5) },
                onPlus = { player.changeTempo(5) }
            )
        }

        if (player.channels.isNotEmpty()) {
            Text(
                "I-mute ang channel (hal. ang melody para sa karaoke):",
                color = KColors.CreamDim,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(player.channels, key = { it.channel }) { info ->
                    val muted = info.channel in player.mutedChannels
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (muted) KColors.NavyLight else KColors.Gold)
                            .clickable { player.toggleMute(info.channel) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            (if (muted) "🔇 " else "") + info.label,
                            color = if (muted) KColors.CreamDim else KColors.Navy,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onMinus) { Text("−", fontSize = 20.sp) }
        Text(value, color = KColors.Gold, fontWeight = FontWeight.Bold)
        TextButton(onClick = onPlus) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
private fun LyricLineView(line: LyricLine, index: Int, current: Int, position: Long) {
    val text = buildAnnotatedString {
        for (w in line.words) {
            val color = when {
                index < current -> KColors.GoldDim
                index == current && w.timeMs <= position -> KColors.Gold
                index == current -> KColors.Cream
                else -> KColors.CreamDim
            }
            withStyle(SpanStyle(color = color)) { append(w.text) }
        }
    }
    Text(
        text = text,
        fontSize = if (index == current) 24.sp else 18.sp,
        fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

internal fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
