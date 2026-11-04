package com.FreeRave.remindme.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.SnoozeOption
import com.FreeRave.remindme.screens.settings.components.SettingsChoiceRow
import com.FreeRave.remindme.screens.settings.components.SettingsChoiceSheet

@Composable
fun SnoozeBottomSheet(
    showSheet: Boolean, availableDurations: List<SnoozeOption>, selectedMinutes: Int,
    onDurationSelected: (Int) -> Unit, onDismiss: () -> Unit
) {
    if (!showSheet) return
    SettingsChoiceSheet(stringResource(R.string.settings_snooze), stringResource(R.string.settings_snooze_hint), onDismiss) {
        availableDurations.forEach { option ->
            SettingsChoiceRow(pluralStringResource(R.plurals.settings_minutes, option.minutes, option.minutes),
                selectedMinutes == option.minutes, { onDurationSelected(option.minutes); onDismiss() })
        }
    }
}
