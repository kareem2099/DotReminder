package com.FreeRave.remindme.screens.category.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.automirrored.outlined.Label
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
internal fun CategoriesStatusCard(loading: Boolean, hasError: Boolean, hasQuery: Boolean, onRetry: () -> Unit, onReset: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading && !hasError) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.categories_loading), style = MaterialTheme.typography.bodyMedium)
            } else {
                Icon(if (hasError) Icons.Outlined.CloudOff else Icons.AutoMirrored.Outlined.Label, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text(stringResource(when {
                    hasError -> R.string.categories_load_error
                    hasQuery -> R.string.categories_no_results
                    else -> R.string.categories_empty_custom
                }), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(when {
                    hasError -> R.string.categories_load_error_body
                    hasQuery -> R.string.categories_no_results_body
                    else -> R.string.categories_empty_custom_body
                }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                TextButton(onClick = if (hasError) onRetry else onReset, enabled = !loading) {
                    Text(stringResource(if (hasError) R.string.categories_retry else R.string.categories_reset))
                }
            }
        }
    }
}
