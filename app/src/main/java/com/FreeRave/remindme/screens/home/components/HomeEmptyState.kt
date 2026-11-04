package com.FreeRave.remindme.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.SearchOff
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
internal fun HomeEmptyState(isLoading: Boolean, hasFilters: Boolean, onAction: () -> Unit, hasError: Boolean = false) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.home_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Icon(
                    if (hasError) Icons.Outlined.CloudOff else if (hasFilters) Icons.Outlined.SearchOff else Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
                        .padding(18.dp).size(30.dp)
                )
                Text(
                    stringResource(if (hasError) R.string.polish_home_error_title else if (hasFilters) R.string.home_no_results_title else R.string.home_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(if (hasError) R.string.polish_home_error_body else if (hasFilters) R.string.home_no_results_body else R.string.home_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onAction) {
                    Text(stringResource(if (hasError) R.string.polish_retry else if (hasFilters) R.string.home_reset_filters else R.string.home_empty_action))
                }
            }
        }
    }
}
