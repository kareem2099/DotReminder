package com.FreeRave.remindme.ui.reminder.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R

@Composable
internal fun ReminderAdvancedSection(customId: String, enabled: Boolean, onCustomIdChange: (String) -> Unit, onSaveTemplate: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { expanded = !expanded }, enabled = enabled) {
            Text(stringResource(R.string.reminder_more))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = customId, onValueChange = onCustomIdChange,
                    label = { Text(stringResource(R.string.reminder_custom_id)) },
                    supportingText = { Text(stringResource(R.string.reminder_custom_id_hint)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    singleLine = true, enabled = enabled
                )
                TextButton(onClick = onSaveTemplate, enabled = enabled) {
                    Text(stringResource(R.string.reminder_save_template), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
