package com.topz.karaoke.ui

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.topz.karaoke.storage.StoredFile
import java.io.File

/**
 * Background video: random na pagkakasunod ng mga video sa folder, paulit-ulit, walang tunog
 * (ang tunog ay galing sa synth). Nire-release ang player kapag umalis sa screen.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoBackground(files: List<StoredFile>, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val exoPlayer = remember(files) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            shuffleModeEnabled = true
            val items = files.map { MediaItem.fromUri(Uri.fromFile(File(it.path))) }
            setMediaItems(items, files.indices.random(), 0L)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                player = exoPlayer
            }
        }
    )
}
