package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun SessionsStatusCard(loading: Boolean, failed: Boolean, onRetry: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.sessions_loading), style = MaterialTheme.typography.bodyMedium)
            } else {
                Icon(if (failed) Icons.Outlined.CloudOff else Icons.Outlined.DevicesOther, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text(stringResource(if (failed) R.string.sessions_load_error else R.string.sessions_empty),
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(if (failed) R.string.sessions_load_error_body else R.string.sessions_empty_body),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                if (failed) TextButton(onClick = onRetry) { Text(stringResource(R.string.sessions_retry)) }
            }
        }
    }
}
