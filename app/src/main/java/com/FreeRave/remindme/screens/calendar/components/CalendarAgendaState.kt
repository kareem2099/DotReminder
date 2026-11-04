package com.FreeRave.remindme.screens.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun CalendarAgendaState(
    loading: Boolean, hasError: Boolean, showTodayAction: Boolean,
    onRetry: () -> Unit, onToday: () -> Unit
) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading && !hasError) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.calendar_loading), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                Icon(if (hasError) Icons.Outlined.CloudOff else Icons.Outlined.CalendarToday, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text(stringResource(if (hasError) R.string.calendar_error_title else R.string.calendar_empty_title),
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(if (hasError) R.string.calendar_error_body else R.string.calendar_empty_body),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                if (hasError || showTodayAction) {
                    TextButton(onClick = if (hasError) onRetry else onToday, enabled = !loading) {
                        Text(stringResource(if (hasError) R.string.calendar_retry else R.string.calendar_back_today))
                    }
                }
            }
        }
    }
}
