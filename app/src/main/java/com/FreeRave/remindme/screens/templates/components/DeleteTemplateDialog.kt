package com.FreeRave.remindme.screens.templates.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.ReminderTemplate

@Composable
internal fun DeleteTemplateDialog(template: ReminderTemplate, busy: Boolean, failed: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.templates_delete)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.templates_delete_body, template.name))
                if (failed) Text(stringResource(R.string.templates_delete_failed), color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(stringResource(if (busy) R.string.templates_deleting else R.string.templates_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.templates_cancel)) } })
}
