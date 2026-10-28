package com.topz.karaoke.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topz.karaoke.data.Song
import com.topz.karaoke.data.SongReader
import com.topz.karaoke.midi.LyricLine
import com.topz.karaoke.midi.Lyrics
import com.topz.karaoke.midi.LyricsExtractor
import com.topz.karaoke.midi.MidiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed class LoadState {
    object Loading : LoadState()
    class Failed(val message: String) : LoadState()
    class Ready(val lyrics: Lyrics, val trackCount: Int, val format: Int) : LoadState()
}

/**
 * Binabasa ang MIDI/KAR ng kanta at ipinapakita ang lyrics. Ang "silent preview" ay
 * nagpapatakbo ng oras (walang tunog pa) para makita kung tama ang timing ng lyrics.
 */
@Composable
fun LyricsScreen(song: Song, onBack: () -> Unit, onAddToQueue: () -> Unit) {
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
            Button(onClick = onAddToQueue) { Text("+ Queue") }
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
            is LoadState.Ready -> LyricsPreview(s)
        }
    }
}

@Composable
private fun LyricsPreview(ready: LoadState.Ready) {
    val lyrics = ready.lyrics
    var running by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    val current = lyrics.lineIndexAt(position)

    LaunchedEffect(running) {
        if (running) {
            val base = SystemClock.elapsedRealtime() - position
            while (true) {
                position = SystemClock.elapsedRealtime() - base
                if (position > lyrics.durationMs + 1000) {
                    running = false
                    break
                }
                delay(40)
            }
        }
    }

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

    if (lyrics.lines.isEmpty()) {
        Text(
            "Walang lyrics na nakita sa MIDI na ito. (Kailangan ng Lyric o Text events.)",
            color = KColors.Cream
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { running = !running }) {
            Text(if (running) "⏸ Ihinto" else "▶ Silent preview")
        }
        Spacer(Modifier.padding(horizontal = 4.dp))
        OutlinedButton(onClick = {
            running = false
            position = 0L
        }) { Text("⏮") }
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            "${formatMs(position)} / ${formatMs(lyrics.durationMs)}",
            color = KColors.Gold,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(Modifier.height(8.dp))

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

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
