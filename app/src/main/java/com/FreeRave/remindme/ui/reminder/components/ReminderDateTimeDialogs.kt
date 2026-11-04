package com.FreeRave.remindme.ui.reminder.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassFormDialog
import com.FreeRave.remindme.ui.reminder.*
import java.text.DateFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderDateDialog(timeMillis: Long, onDismiss: () -> Unit, onSelected: (Long) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = pickerDateMillis(timeMillis))
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { onSelected(applyPickerDate(it, timeMillis)) } },
                enabled = picker.selectedDateMillis != null) { Text(stringResource(R.string.reminder_confirm)) }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminder_cancel)) } }) { DatePicker(state = picker) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderTimeDialog(timeMillis: Long, onDismiss: () -> Unit, onSelected: (Long) -> Unit) {
    val initial = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault())
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    var hour by rememberSaveable { mutableStateOf((if (is24) initial.hour else (initial.hour % 12).takeIf { it > 0 } ?: 12).toString()) }
    var minute by rememberSaveable { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
    var afternoon by rememberSaveable { mutableStateOf(initial.hour >= 12) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val selectedHour = selectedTimeHour(hour, is24, afternoon)
    val selectedMinute = selectedTimeMinute(minute)
    val focus = LocalFocusManager.current
    val confirm = {
        submitted = true
        if (selectedHour != null && selectedMinute != null) onSelected(applyPickerTime(timeMillis, selectedHour, selectedMinute))
    }
    GlassFormDialog(stringResource(R.string.reminder_time), stringResource(R.string.reminder_confirm), confirm, onDismiss,
        subtitle = stringResource(R.string.polish_time_hint)) {
        OutlinedTextField(hour, { hour = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            label = { Text(stringResource(R.string.polish_hour)) }, isError = submitted && selectedHour == null,
            supportingText = if (submitted && selectedHour == null) {{ Text(stringResource(if (is24) R.string.polish_hour_24_error else R.string.polish_hour_12_error)) }} else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
        OutlinedTextField(minute, { minute = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            label = { Text(stringResource(R.string.polish_minute)) }, isError = submitted && selectedMinute == null,
            supportingText = if (submitted && selectedMinute == null) {{ Text(stringResource(R.string.polish_minute_error)) }} else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { confirm() }))
        if (!is24) {
            val periods = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(!afternoon, { afternoon = false }, label = { Text(periods[0]) })
                FilterChip(afternoon, { afternoon = true }, label = { Text(periods[1]) })
            }
        }
    }
}
