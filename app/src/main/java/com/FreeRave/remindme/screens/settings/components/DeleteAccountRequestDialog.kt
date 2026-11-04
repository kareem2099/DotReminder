package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassFormDialog

/** Explains the manual request before opening the public request resource. Nothing is deleted here. */
@Composable
internal fun DeleteAccountRequestDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    GlassFormDialog(
        title = stringResource(R.string.deletion_title),
        subtitle = stringResource(R.string.deletion_intro),
        confirmLabel = stringResource(R.string.deletion_continue),
        onConfirm = onContinue, onDismiss = onDismiss
    ) {
        Text(stringResource(R.string.deletion_scope), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.deletion_timing), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.deletion_warning), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error)
    }
}
