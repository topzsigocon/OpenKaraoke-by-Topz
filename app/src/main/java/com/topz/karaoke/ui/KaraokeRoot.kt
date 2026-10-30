package com.topz.karaoke.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.topz.karaoke.audio.PlayerStatus
import com.topz.karaoke.data.Song

@Composable
fun KaraokeRoot(vm: KaraokeViewModel, onRequestAccess: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var openSong by remember { mutableStateOf<Song?>(null) }
    val player = vm.player

    // Back button: isara muna ang lyrics screen bago lumabas ng app.
    BackHandler(enabled = openSong != null) { openSong = null }
    // Sa Karaoke Mode, ang Back ay nagbabalik lang ng normal na UI (hindi lumalabas ng app).
    BackHandler(enabled = vm.karaokeMode) { vm.exitKaraokeMode() }

    // Huwag patayin ang screen habang tumutugtog.
    val view = LocalView.current
    val playing = player.status == PlayerStatus.PLAYING
    DisposableEffect(playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }

    // Sa Karaoke Mode, itago pati ang status bar at navigation bar ng phone (swipe para ipakita).
    DisposableEffect(vm.karaokeMode) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (vm.karaokeMode) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val barVisible = player.song != null && player.status != PlayerStatus.IDLE

    if (vm.karaokeMode) {
        Box(Modifier.fillMaxSize()) {
            KaraokeScreen(vm)
            FloatingController(
                vm = vm,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(end = 16.dp, bottom = 24.dp)
            )
        }
        return
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { AppHeader() },
            bottomBar = {
                Column {
                    NowPlayingBar(vm) { player.song?.let { openSong = it } }
                    NavigationBar(containerColor = KColors.NavySurface) {
                        val items = listOf(
                            "🎵" to "Library",
                            "📋" to "Queue (${vm.queue.size})",
                            "⚙️" to "Settings"
                        )
                        items.forEachIndexed { i, item ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = {
                                    tab = i
                                    openSong = null
                                },
                                icon = { Text(item.first) },
                                label = { Text(item.second) }
                            )
                        }
                    }
                }
            }
        ) { inner ->
            Box(Modifier.padding(inner).fillMaxSize()) {
                val opened = openSong
                if (opened != null) {
                    LyricsScreen(song = opened, vm = vm, onBack = { openSong = null })
                } else {
                    when (tab) {
                        0 -> LibraryScreen(
                            vm = vm,
                            onRequestAccess = onRequestAccess,
                            onOpenSettings = { tab = 2 },
                            onOpenSong = { openSong = it }
                        )
                        1 -> QueueScreen(vm)
                        else -> SettingsScreen(vm, onRequestAccess)
                    }
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
                .padding(end = 16.dp, bottom = if (barVisible) 156.dp else 96.dp)
        )
    }
}

/** Maliit na player bar na laging nakikita habang may kanta; pindutin para buksan ang lyrics. */
@Composable
private fun NowPlayingBar(vm: KaraokeViewModel, onOpen: () -> Unit) {
    val player = vm.player
    val song = player.song
    if (song == null || player.status == PlayerStatus.IDLE) return

    val statusText = when (player.status) {
        PlayerStatus.LOADING -> "Nilo-load..."
        PlayerStatus.PLAYING -> "Tumutugtog • ${formatMs(player.positionMs)} / ${formatMs(player.durationMs)}"
        PlayerStatus.PAUSED -> "Naka-pause • ${formatMs(player.positionMs)} / ${formatMs(player.durationMs)}"
        PlayerStatus.ERROR -> "May error — pindutin para makita"
        PlayerStatus.IDLE -> ""
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(KColors.NavyLight)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = KColors.Gold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(statusText, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        TextButton(onClick = { vm.enterKaraokeMode() }) { Text("📺", fontSize = 22.sp) }
        TextButton(
            onClick = { player.togglePlayPause() },
            enabled = player.status == PlayerStatus.PLAYING || player.status == PlayerStatus.PAUSED
        ) { Text(if (player.status == PlayerStatus.PLAYING) "⏸" else "▶", fontSize = 22.sp) }
        TextButton(onClick = { vm.skip() }) { Text("⏭", fontSize = 22.sp) }
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

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
