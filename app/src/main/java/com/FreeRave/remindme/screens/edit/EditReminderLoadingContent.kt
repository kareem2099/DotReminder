package com.FreeRave.remindme.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.reminder.ReminderFormScaffold

@Composable
internal fun EditReminderLoadingContent(
    loading: Boolean, snackbar: SnackbarHostState, onBack: () -> Unit, onRetry: () -> Unit
) {
    ReminderFormScaffold(stringResource(R.string.reminder_edit), snackbar, onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            GlassSurface(Modifier.widthIn(max = 480.dp)) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.reminder_loading), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text(stringResource(R.string.reminder_load_error), style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center)
                        Text(stringResource(R.string.reminder_load_error_body), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = onRetry) { Text(stringResource(R.string.reminder_retry)) }
                    }
                }
            }
        }
    }
}
