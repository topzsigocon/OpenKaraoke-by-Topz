package com.topz.karaoke.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

/**
 * Sariling folder browser (gumagamit ng All Files Access), kaya puwedeng pumili
 * kahit saang folder sa internal storage o SD card, kasama ang root ng SD card.
 */
@Composable
fun FolderPickerDialog(
    roots: List<File>,
    startPath: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember {
        mutableStateOf<File?>(startPath?.let { File(it) }?.takeIf { it.isDirectory })
    }

    val folders: List<File> = remember(current) {
        val c = current
        if (c == null) {
            roots
        } else {
            c.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        }
    }

    fun labelOf(f: File): String = when {
        roots.none { it.absolutePath == f.absolutePath } -> f.name
        f.absolutePath == "/storage/emulated/0" -> "📱 Internal storage"
        else -> "💾 SD card (${f.name})"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = KColors.NavySurface,
            border = BorderStroke(1.dp, KColors.GoldDim)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Pumili ng folder", color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    current?.absolutePath ?: "Mga storage",
                    color = KColors.CreamDim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                    val c = current
                    if (c != null) {
                        item {
                            Text(
                                "⬆ Pataas",
                                color = KColors.Gold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        current = if (roots.any { it.absolutePath == c.absolutePath }) null else c.parentFile
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                    items(folders, key = { it.absolutePath }) { dir ->
                        Text(
                            if (current == null) labelOf(dir) else "📁 ${dir.name}",
                            color = KColors.Cream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { current = dir }
                                .padding(vertical = 12.dp)
                        )
                    }
                    if (folders.isEmpty() && c != null) {
                        item { Text("(Walang subfolder)", color = KColors.CreamDim, modifier = Modifier.padding(vertical = 12.dp)) }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Kanselahin") }
                    Row(Modifier.weight(1f)) {}
                    Button(
                        enabled = current != null,
                        onClick = { current?.let { onPick(it.absolutePath) } }
                    ) { Text("Piliin ito") }
                }
            }
        }
    }
}
