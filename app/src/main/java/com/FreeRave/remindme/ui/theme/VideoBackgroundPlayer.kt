package com.FreeRave.remindme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@UnstableApi
@Composable
fun VideoBackgroundPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoPath) { // Re-initialize if videoPath changes
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE // Loop the video
            volume = 0f // Mute the video
        }
    }

    LaunchedEffect(videoPath) { // React to videoPath changes
        val assetUri = "file:///android_asset/$videoPath".toUri()
        val mediaItem = MediaItem.fromUri(assetUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    // Clean up when composable leaves composition or player instance changes
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide video controls
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        modifier = modifier
    )
}
