package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R

@Composable
internal fun NotificationSettingsSection(
    soundLabel: String, selectedSnoozeMinutes: Int, vibrationEnabled: Boolean,
    onSoundClicked: () -> Unit, onSnoozeClicked: () -> Unit, onVibrationChanged: (Boolean) -> Unit
) {
    SettingsSection(stringResource(R.string.settings_notifications)) {
        SettingItem(stringResource(R.string.settings_sound), soundLabel, onSoundClicked, icon = Icons.Outlined.MusicNote)
        SettingsDivider()
        SettingItem(stringResource(R.string.settings_snooze),
            pluralStringResource(R.plurals.settings_minutes, selectedSnoozeMinutes, selectedSnoozeMinutes),
            onSnoozeClicked, icon = Icons.Outlined.Snooze)
        SettingsDivider()
        SettingItemWithSwitch(stringResource(R.string.settings_vibrate), vibrationEnabled, onVibrationChanged,
            subtitle = stringResource(R.string.settings_vibrate_hint), icon = Icons.Outlined.Vibration)
    }
}
