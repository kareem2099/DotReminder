package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.UserPreferencesRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale as JavaLocale

@Composable
fun ReminderSettingsSection(
    defaultHour: Int,
    defaultMinute: Int,
    onDefaultTimeSettingClicked: () -> Unit,
    onClearDefaultTimeClicked: () -> Unit
) {
    Column {
        Text(
            text = "Reminders",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        val timeFormatter = remember { SimpleDateFormat("hh:mm a", JavaLocale.getDefault()) }
        val defaultTimeDisplay = remember(defaultHour, defaultMinute) {
            if (defaultHour != UserPreferencesRepository.NO_DEFAULT_TIME_SET && defaultMinute != UserPreferencesRepository.NO_DEFAULT_TIME_SET) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, defaultHour)
                    set(Calendar.MINUTE, defaultMinute)
                }
                timeFormatter.format(cal.time)
            } else {
                "Not Set"
            }
        }
        SettingItem(
            title = "Default Time",
            currentValue = defaultTimeDisplay,
            onClick = onDefaultTimeSettingClicked
        )
        if (defaultHour != UserPreferencesRepository.NO_DEFAULT_TIME_SET) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onClearDefaultTimeClicked) {
                Text("Clear Default Time")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
