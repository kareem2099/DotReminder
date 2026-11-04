package com.FreeRave.remindme.ui.reminder.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.reminder.DAY_MILLIS
import com.FreeRave.remindme.ui.reminder.HOUR_MILLIS
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormError
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent
import com.FreeRave.remindme.ui.reminder.WEEK_MILLIS
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderScheduleSection(
    draft: ReminderDraft,
    errors: Set<ReminderFormError>,
    enabled: Boolean,
    onDate: () -> Unit,
    onTime: () -> Unit,
    onEndDate: () -> Unit,
    onEvent: (ReminderFormEvent) -> Unit
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val use24HourTime = DateFormat.is24HourFormat(LocalContext.current)
    val timeFormatter = remember(locale, use24HourTime) {
        DateTimeFormatter.ofPattern(if (use24HourTime) "HH:mm" else "h:mm a", locale)
    }
    val scheduled = draft.reminderTimeMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    ReminderFormSection(stringResource(R.string.reminder_schedule), Icons.Outlined.Schedule) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScheduleValue(stringResource(R.string.reminder_date), scheduled?.format(dateFormatter)
                ?: stringResource(R.string.reminder_time_empty), Icons.Outlined.CalendarToday,
                enabled, Modifier.weight(1f), onDate)
            ScheduleValue(stringResource(R.string.reminder_time), scheduled?.format(timeFormatter)
                ?: stringResource(R.string.reminder_time_empty), Icons.Outlined.Schedule,
                enabled, Modifier.weight(1f), onTime)
        }
        if (ReminderFormError.TIME in errors) ReminderFieldError(stringResource(R.string.reminder_error_time))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onEvent(ReminderFormEvent.Time(System.currentTimeMillis() + HOUR_MILLIS)) }, enabled = enabled) {
                Text(stringResource(R.string.reminder_in_hour))
            }
            TextButton(onClick = {
                val tomorrow = Instant.ofEpochMilli(draft.reminderTimeMillis ?: System.currentTimeMillis())
                    .atZone(ZoneId.systemDefault()).toLocalTime()
                val date = java.time.LocalDate.now().plusDays(1).atTime(tomorrow).atZone(ZoneId.systemDefault())
                onEvent(ReminderFormEvent.Time(date.toInstant().toEpochMilli()))
            }, enabled = enabled) { Text(stringResource(R.string.reminder_tomorrow)) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.reminder_repeat), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.reminder_repeat_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(draft.isRecurring, onCheckedChange = { onEvent(ReminderFormEvent.Recurring(it)) }, enabled = enabled)
        }
        AnimatedVisibility(draft.isRecurring) {
            RepeatOptions(draft, errors, enabled, onEndDate, onEvent)
        }
    }
}

@Composable
private fun ScheduleValue(
    label: String, value: String, icon: ImageVector, enabled: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    Column(modifier.clickable(enabled = enabled, onClickLabel = label, onClick = onClick).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatOptions(
    draft: ReminderDraft, errors: Set<ReminderFormError>, enabled: Boolean,
    onEndDate: () -> Unit, onEvent: (ReminderFormEvent) -> Unit
) {
    val presets = listOf(HOUR_MILLIS, DAY_MILLIS, WEEK_MILLIS)
    var custom by rememberSaveable { mutableStateOf(draft.recurrenceIntervalMillis !in presets) }
    var minutes by rememberSaveable { mutableStateOf(draft.recurrenceIntervalMillis?.div(60_000)?.toString().orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.reminder_hourly, R.string.reminder_daily, R.string.reminder_weekly).zip(presets).forEach { (label, value) ->
                ReminderOptionChip(stringResource(label), !custom && draft.recurrenceIntervalMillis == value, enabled) {
                    custom = false
                    onEvent(ReminderFormEvent.Interval(value))
                }
            }
            ReminderOptionChip(stringResource(R.string.reminder_custom), custom, enabled) {
                custom = true
                onEvent(ReminderFormEvent.Interval(minutes.toLongOrNull()?.takeIf { it in 1..MAX_INTERVAL_MINUTES }?.times(60_000)))
            }
        }
        if (custom) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { text ->
                    minutes = text.filter(Char::isDigit).take(9)
                    onEvent(ReminderFormEvent.Interval(minutes.toLongOrNull()?.takeIf { it in 1..MAX_INTERVAL_MINUTES }?.times(60_000)))
                },
                label = { Text(stringResource(R.string.reminder_interval)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                isError = ReminderFormError.INTERVAL in errors,
                singleLine = true, enabled = enabled
            )
        }
        if (ReminderFormError.INTERVAL in errors) ReminderFieldError(stringResource(R.string.reminder_error_interval))
        Text(stringResource(R.string.reminder_end_date), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onEndDate, enabled = enabled, modifier = Modifier.weight(1f)) {
                val end = draft.recurrenceEndTime?.let {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                }
                Text(end ?: stringResource(R.string.reminder_no_end))
                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
            }
            if (draft.recurrenceEndTime != null) {
                IconButton(onClick = { onEvent(ReminderFormEvent.EndTime(null)) }, enabled = enabled) {
                    Icon(Icons.Default.Close, stringResource(R.string.reminder_remove_end), modifier = Modifier.size(18.dp))
                }
            }
        }
        if (ReminderFormError.END_TIME in errors) ReminderFieldError(stringResource(R.string.reminder_error_end))
    }
}

private const val MAX_INTERVAL_MINUTES = 525_600_000L
