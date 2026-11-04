package com.FreeRave.remindme.ui.reminder.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormError
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderLocationSection(
    draft: ReminderDraft, errors: Set<ReminderFormError>, enabled: Boolean,
    onPickLocation: () -> Unit, onEvent: (ReminderFormEvent) -> Unit
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    ReminderFormSection(stringResource(R.string.reminder_location), Icons.Outlined.LocationOn) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.reminder_location_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Switch(draft.isLocationBased, onCheckedChange = { onEvent(ReminderFormEvent.LocationEnabled(it)) }, enabled = enabled)
        }
        AnimatedVisibility(draft.isLocationBased) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val latitude = draft.latitude
                val longitude = draft.longitude
                val hasPlace = latitude != null && longitude != null
                OutlinedButton(onClick = onPickLocation, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (hasPlace) R.string.reminder_change_place else R.string.reminder_pick_place))
                }
                if (latitude != null && longitude != null) {
                    Text(stringResource(R.string.reminder_coordinates, latitude, longitude, draft.geofenceRadius.toInt()),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (ReminderFormError.LOCATION in errors) ReminderFieldError(stringResource(R.string.reminder_error_location))
                Text(stringResource(R.string.reminder_trigger), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.reminder_arrive to 1, R.string.reminder_leave to 2, R.string.reminder_both to 3).forEach { (label, value) ->
                        ReminderOptionChip(stringResource(label), draft.geofenceTransitionType == value, enabled) {
                            onEvent(ReminderFormEvent.Transition(value))
                        }
                    }
                }
                if (ReminderFormError.TRIGGER in errors) ReminderFieldError(stringResource(R.string.reminder_error_trigger))
                TextButton(onClick = { advanced = !advanced }, enabled = enabled) {
                    Text(stringResource(R.string.reminder_location_options))
                    Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                }
                AnimatedVisibility(advanced) {
                    LocationOptions(draft, enabled, onEvent)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationOptions(draft: ReminderDraft, enabled: Boolean, onEvent: (ReminderFormEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reminder_wait), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.reminder_immediate to null, R.string.reminder_30_seconds to 30_000L,
                R.string.reminder_1_minute to 60_000L, R.string.reminder_5_minutes to 300_000L).forEach { (label, value) ->
                ReminderOptionChip(stringResource(label), draft.geofenceDwellTimeMillis == value, enabled) {
                    onEvent(ReminderFormEvent.DwellTime(value))
                }
            }
        }
        Text(stringResource(R.string.reminder_priority), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.reminder_low to 1, R.string.reminder_medium to 3, R.string.reminder_high to 5).forEach { (label, value) ->
                ReminderOptionChip(stringResource(label), draft.geofencePriority == value, enabled) {
                    onEvent(ReminderFormEvent.Priority(value))
                }
            }
        }
    }
}
