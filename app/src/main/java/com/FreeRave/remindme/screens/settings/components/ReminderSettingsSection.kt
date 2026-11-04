package com.FreeRave.remindme.screens.settings.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.hasDefaultReminderTime
import java.util.Calendar
import java.util.Locale

@Composable
fun ReminderSettingsSection(
    defaultHour: Int, defaultMinute: Int, onDefaultTimeSettingClicked: () -> Unit, onClearDefaultTimeClicked: () -> Unit
) {
    val context = LocalContext.current
    val use24Hour = DateFormat.is24HourFormat(context)
    val formatter = remember(use24Hour, Locale.getDefault()) { DateFormat.getTimeFormat(context) }
    val hasTime = hasDefaultReminderTime(defaultHour, defaultMinute)
    val label = if (hasTime) formatter.format(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, defaultHour); set(Calendar.MINUTE, defaultMinute)
    }.time) else stringResource(R.string.settings_time_not_set)
    SettingsSection(stringResource(R.string.settings_reminders)) {
        SettingItem(stringResource(R.string.settings_default_time), label, onDefaultTimeSettingClicked, icon = Icons.Outlined.Schedule)
        if (hasTime) TextButton(onClick = onClearDefaultTimeClicked, modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)) {
            Text(stringResource(R.string.settings_clear_time))
        }
    }
}
