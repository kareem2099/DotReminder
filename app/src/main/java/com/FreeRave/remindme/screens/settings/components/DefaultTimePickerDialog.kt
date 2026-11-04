package com.FreeRave.remindme.screens.settings.components

import androidx.compose.runtime.Composable
import com.FreeRave.remindme.screens.settings.hasDefaultReminderTime
import com.FreeRave.remindme.ui.reminder.components.ReminderTimeDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun DefaultTimePickerDialog(
    showDialog: Boolean, defaultHour: Int, defaultMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit, onDismiss: () -> Unit
) {
    if (!showDialog) return
    val zone = ZoneId.systemDefault()
    val time = if (hasDefaultReminderTime(defaultHour, defaultMinute)) LocalTime.of(defaultHour, defaultMinute) else LocalTime.now()
    ReminderTimeDialog(LocalDate.now().atTime(time).atZone(zone).toInstant().toEpochMilli(), onDismiss) {
        val selected = Instant.ofEpochMilli(it).atZone(zone)
        onTimeSelected(selected.hour, selected.minute)
    }
}
