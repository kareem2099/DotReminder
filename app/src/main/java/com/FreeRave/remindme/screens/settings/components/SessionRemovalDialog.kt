package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.SessionRemoval

@Composable
internal fun SessionRemovalDialog(
    target: SessionRemoval, busy: Boolean, failed: Boolean, canConfirm: Boolean,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    val single = target is SessionRemoval.One
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(if (single) R.string.sessions_remove_one_title else R.string.sessions_remove_others_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (target is SessionRemoval.One) stringResource(R.string.sessions_remove_one_body, sessionDeviceName(target.session))
                    else stringResource(R.string.sessions_remove_others_body))
                if (failed) Text(stringResource(R.string.sessions_remove_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm && !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(if (busy) R.string.sessions_removing else if (single) R.string.sessions_remove else R.string.sessions_clear_others),
                    color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.sessions_cancel)) } })
}
