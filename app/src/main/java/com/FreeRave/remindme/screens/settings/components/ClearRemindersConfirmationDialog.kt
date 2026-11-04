package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R

@Composable
fun ClearRemindersConfirmationDialog(showDialog: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (showDialog) AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
        text = { Text(stringResource(R.string.settings_clear_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.settings_delete_all), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } }
    )
}
