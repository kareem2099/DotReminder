package com.FreeRave.remindme.ui.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.LocationPickerDialog
import com.FreeRave.remindme.ui.components.MapLocationPickerDialog
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent

internal enum class ReminderLocationPicker { CHOICE, MAP, COORDINATES }

@Composable
internal fun ReminderLocationDialogs(
    picker: ReminderLocationPicker?, draft: ReminderDraft,
    onPickerChange: (ReminderLocationPicker?) -> Unit,
    onEvent: (ReminderFormEvent) -> Unit
) {
    val onSelected: (Double?, Double?, Float) -> Unit = { latitude, longitude, radius ->
        onEvent(ReminderFormEvent.Location(latitude, longitude, radius))
        onPickerChange(null)
    }
    when (picker) {
        ReminderLocationPicker.CHOICE -> AlertDialog(
            onDismissRequest = { onPickerChange(null) },
            title = { Text(stringResource(R.string.reminder_location_method)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onPickerChange(ReminderLocationPicker.MAP) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.reminder_map))
                    }
                    OutlinedButton(onClick = { onPickerChange(ReminderLocationPicker.COORDINATES) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.reminder_manual_location))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onPickerChange(null) }) { Text(stringResource(R.string.reminder_cancel)) } }
        )
        ReminderLocationPicker.MAP -> MapLocationPickerDialog(
            onDismiss = { onPickerChange(null) }, onLocationSelected = onSelected,
            initialLatitude = draft.latitude, initialLongitude = draft.longitude, initialRadius = draft.geofenceRadius
        )
        ReminderLocationPicker.COORDINATES -> LocationPickerDialog(
            onDismiss = { onPickerChange(null) }, onLocationSelected = onSelected,
            initialLatitude = draft.latitude, initialLongitude = draft.longitude, initialRadius = draft.geofenceRadius
        )
        null -> Unit
    }
}
