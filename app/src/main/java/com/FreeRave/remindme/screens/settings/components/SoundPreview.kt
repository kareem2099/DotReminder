package com.FreeRave.remindme.screens.settings.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owns a single, short preview and releases it on every completion or cancellation path. */
internal class SoundPreview(private val context: Context, private val scope: CoroutineScope) {
    var playing: String? by mutableStateOf(null)
        private set
    var failed by mutableStateOf(false)
        private set
    private var player: MediaPlayer? = null
    private var timeout: Job? = null

    fun toggle(resource: String) {
        if (playing == resource) { stop(); return }
        stop()
        failed = false
        val next = MediaPlayer()
        player = next
        playing = resource
        try {
            val id = context.resources.getIdentifier(resource, "raw", context.packageName)
            val uri = if (id != 0) Uri.parse("android.resource://${context.packageName}/$id")
                else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            next.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            next.setDataSource(context, uri)
            next.setOnPreparedListener {
                if (player === it) {
                    try {
                        it.start()
                        timeout = scope.launch { delay(5_000); stop() }
                    } catch (_: IllegalStateException) { stop(); failed = true }
                }
            }
            next.setOnCompletionListener { if (player === it) stop() }
            next.setOnErrorListener { source, _, _ ->
                if (player === source) { stop(); failed = true }
                true
            }
            next.prepareAsync()
        } catch (_: Exception) { stop(); failed = true }
    }

    fun stop() {
        timeout?.cancel()
        timeout = null
        player?.apply {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            release()
        }
        player = null
        playing = null
    }
}
