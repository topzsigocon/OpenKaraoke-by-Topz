package com.topz.karaoke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KaraokeRoot(vm: KaraokeViewModel, onRequestAccess: () -> Unit) {
    var tab by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { AppHeader() },
            bottomBar = {
                NavigationBar(containerColor = KColors.NavySurface) {
                    val items = listOf(
                        "🎵" to "Library",
                        "📋" to "Queue (${vm.queue.size})",
                        "⚙️" to "Settings"
                    )
                    items.forEachIndexed { i, item ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(item.first) },
                            label = { Text(item.second) }
                        )
                    }
                }
            }
        ) { inner ->
            Box(Modifier.padding(inner).fillMaxSize()) {
                when (tab) {
                    0 -> LibraryScreen(vm, onRequestAccess, onOpenSettings = { tab = 2 })
                    1 -> QueueScreen(vm)
                    else -> SettingsScreen(vm, onRequestAccess)
                }
            }
        }

        // Floating controller: nasa ibabaw ng lahat ng screen, puwedeng hilahin.
        FloatingController(
            vm = vm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .imePadding()
                .padding(end = 16.dp, bottom = 96.dp)
        )
    }
}

@Composable
private fun AppHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(KColors.NavySurface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🎤", fontSize = 22.sp)
        Text(
            text = "  OpenKaraoke",
            color = KColors.Gold,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
