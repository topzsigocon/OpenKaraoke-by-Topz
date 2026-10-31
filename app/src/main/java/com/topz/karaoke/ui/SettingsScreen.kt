package com.topz.karaoke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topz.karaoke.storage.FileKind
import com.topz.karaoke.midi.Lyrics
import com.topz.karaoke.midi.LyricWord
import com.topz.karaoke.midi.LyricLine
import com.topz.karaoke.data.LyricsPosition
import com.topz.karaoke.data.LyricsAlignment
import com.topz.karaoke.data.KaraokeLayout
import androidx.compose.ui.graphics.Brush

@Composable
fun SettingsScreen(vm: KaraokeViewModel, onRequestAccess: () -> Unit) {
    var picking by remember { mutableStateOf<FileKind?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Storage", color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (vm.hasAccess) {
            Text("✅ May access sa storage (kasama ang SD card)", color = KColors.Cream)
        } else {
            NoticeCard(
                message = "Kailangan ng access sa storage para mabasa ang SD card.",
                buttonText = "Bigyan ng access",
                onClick = onRequestAccess
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Mga folder", color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        for (kind in FileKind.values()) {
            val (label, count) = when (kind) {
                FileKind.SONG -> "Mga kanta (.mid / .kar / .zip)" to "${vm.songs.size} nahanap"
                FileKind.SOUNDFONT -> "SoundFont (.sf2)" to "${vm.soundFontCount} nahanap"
                FileKind.VIDEO -> "Background video" to "${vm.videoCount} nahanap"
            }
            val custom = vm.folders[kind]
            FolderCard(
                label = label,
                path = custom ?: "Default: OpenKaraoke/${kind.folder} sa internal at SD card",
                count = count,
                isCustom = custom != null,
                onPick = { picking = kind },
                onReset = { vm.setFolder(kind, null) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Background video", color = KColors.Cream, fontWeight = FontWeight.Bold)
                Text(
                    if (vm.videoFiles.isEmpty()) {
                        "Walang video na nahanap sa folder ng video."
                    } else {
                        "Random na video mula sa ${vm.videoFiles.size} nahanap, sa likod ng lyrics. " +
                            "Kung nauutal ang tunog, patayin ito."
                    },
                    color = KColors.CreamDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = vm.videoEnabled, onCheckedChange = { vm.toggleVideo() })
        }
        Spacer(Modifier.height(16.dp))
        Text("Karaoke Mode", color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            "Ang Default ang kasalukuyang ayos. Binabago lang ng mga option dito ang itsura ng Karaoke Mode.",
            color = KColors.CreamDim,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        KaraokePreview(vm.karaokeLayout, vm.lyricsPosition, vm.lyricsAlignment)
        Spacer(Modifier.height(12.dp))
        ChoiceRow(
            title = "Layout ng lyrics",
            options = listOf(KaraokeLayout.DEFAULT to "Default", KaraokeLayout.TWO_LINE to "Two-line"),
            selected = vm.karaokeLayout,
            onSelect = { vm.chooseKaraokeLayout(it) }
        )
        Text(
            "Default: kasalukuyang linya, at ang susunod na mas maliit sa ibaba. " +
                "Two-line: dalawang linya ang nakikita at nagpapalitan.",
            color = KColors.CreamDim,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        ChoiceRow(
            title = "Lyrics position",
            options = listOf(LyricsPosition.CENTER to "Center", LyricsPosition.BOTTOM to "Bottom"),
            selected = vm.lyricsPosition,
            onSelect = { vm.chooseLyricsPosition(it) }
        )
        Spacer(Modifier.height(12.dp))
        ChoiceRow(
            title = "Lyrics alignment",
            options = listOf(LyricsAlignment.CENTERED to "Centered", LyricsAlignment.OPPOSING to "Opposing"),
            selected = vm.lyricsAlignment,
            onSelect = { vm.chooseLyricsAlignment(it) }
        )
        Text(
            "Opposing: ang unang linya ay sa kaliwa at ang pangalawa ay sa kanan.",
            color = KColors.CreamDim,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(20.dp))

        MicPanel(vm)
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(8.dp))
        Text("SoundFont na gagamitin", color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SoundFontRow(
            name = "Default (GeneralUser GS, kasama sa app)",
            selected = vm.activeSoundFont == null,
            onClick = { vm.selectSoundFont(null) }
        )
        for (font in vm.soundFontFiles) {
            SoundFontRow(
                name = font.name,
                selected = vm.activeSoundFont == font.path,
                onClick = { vm.selectSoundFont(font.path) }
            )
        }
        Text(
            "Ilagay ang iyong .sf2 sa folder ng SoundFont at i-scan ulit. Ang napakalalaking SF2 " +
                "(100 MB pataas) ay maaaring maubusan ng memorya sa phone. Magagamit ang bago sa susunod na kantang i-play.",
            color = KColors.CreamDim,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        Button(onClick = { vm.rescan() }, enabled = vm.hasAccess && !vm.scanning) {
            Text(if (vm.scanning) "Nag-i-scan..." else "I-scan ulit")
        }
        Spacer(Modifier.height(96.dp))
    }

    val kind = picking
    if (kind != null) {
        FolderPickerDialog(
            roots = vm.volumeRoots(),
            startPath = vm.folders[kind],
            onPick = { path ->
                vm.setFolder(kind, path)
                picking = null
            },
            onDismiss = { picking = null }
        )
    }
}

@Composable
private fun FolderCard(
    label: String,
    path: String,
    count: String,
    isCustom: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KColors.NavySurface)
            .padding(16.dp)
    ) {
        Text(label, color = KColors.Cream, fontWeight = FontWeight.Bold)
        Text(path, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
        Text(count, color = KColors.Gold, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = onPick) { Text("Pumili ng folder") }
            if (isCustom) {
                TextButton(onClick = onReset) { Text("I-reset") }
            }
        }
    }
}

@Composable
private fun SoundFontRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(if (selected) "◉" else "○", color = KColors.Gold)
        Text("  $name", color = if (selected) KColors.Cream else KColors.CreamDim)
    }
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Text(title, color = KColors.Cream, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) {
            val isSelected = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) KColors.Gold else KColors.NavyLight)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) KColors.Navy else KColors.Cream,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

/** Maliit na preview ng napiling ayos, gamit ang sample na lyrics. */
@Composable
private fun KaraokePreview(layout: KaraokeLayout, position: LyricsPosition, alignment: LyricsAlignment) {
    val sample = remember {
        Lyrics(
            lines = listOf(
                LyricLine(listOf(LyricWord(0, "Sample "), LyricWord(500, "lyrics "), LyricWord(1000, "line one"))),
                LyricLine(listOf(LyricWord(2000, "Next line "), LyricWord(2500, "preview"))),
                LyricLine(listOf(LyricWord(4000, "Third line")))
            ),
            info = emptyList(),
            source = Lyrics.Source.LYRIC_EVENTS,
            durationMs = 6000
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(KColors.Navy, KColors.NavyLight)))
            .padding(12.dp),
        contentAlignment = if (position == LyricsPosition.BOTTOM) Alignment.BottomCenter else Alignment.Center
    ) {
        KaraokeLyrics(sample, 700L, layout, alignment, fontScale = 0.45f)
    }
}
