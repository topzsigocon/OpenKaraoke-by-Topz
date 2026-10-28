package com.topz.karaoke.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topz.karaoke.storage.FileKind

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
