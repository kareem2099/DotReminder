package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.SoundOption

@Composable
internal fun SoundBottomSheet(
    sounds: List<SoundOption>, selectedResource: String,
    onSelected: (String) -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val preview = remember(context, scope) { SoundPreview(context, scope) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(preview, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) preview.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); preview.stop() }
    }
    SettingsChoiceSheet(stringResource(R.string.settings_choose_sound), stringResource(R.string.settings_sound_hint), onDismiss) {
        sounds.forEach { sound ->
            SettingsChoiceRow(sound.displayName, selectedResource == sound.resourceName,
                onClick = { preview.stop(); onSelected(sound.resourceName); onDismiss() },
                trailing = {
                    RadioButton(selected = selectedResource == sound.resourceName, onClick = null)
                    IconButton(onClick = { preview.toggle(sound.resourceName) }) {
                        val playing = preview.playing == sound.resourceName
                        Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            if (playing) stringResource(R.string.settings_stop_preview)
                            else stringResource(R.string.settings_preview_sound, sound.displayName),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                })
        }
        if (preview.failed) Text(stringResource(R.string.settings_preview_failed), color = MaterialTheme.colorScheme.error)
    }
}
