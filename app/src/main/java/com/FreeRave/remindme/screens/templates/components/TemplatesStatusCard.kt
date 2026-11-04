package com.FreeRave.remindme.screens.templates.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.templates.TemplateFilter
import com.FreeRave.remindme.screens.templates.TemplatesUiState
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun TemplatesStatusCard(state: TemplatesUiState, onRetry: () -> Unit, onReset: () -> Unit) {
    val filtered = state.query.isNotBlank() || state.filter != TemplateFilter.ALL
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.templates_loading))
            } else {
                Icon(Icons.Outlined.Layers, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Text(stringResource(when {
                    !state.isAuthenticated -> R.string.templates_sign_in
                    state.hasError -> R.string.templates_load_error
                    filtered -> R.string.templates_no_results
                    else -> R.string.templates_empty
                }), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(when {
                    !state.isAuthenticated -> R.string.templates_sign_in_body
                    state.hasError -> R.string.templates_load_error_body
                    filtered -> R.string.templates_no_results_body
                    else -> R.string.templates_empty_body
                }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                if (state.isAuthenticated && (state.hasError || filtered)) TextButton(onClick = if (state.hasError) onRetry else onReset) {
                    Text(stringResource(if (state.hasError) R.string.templates_retry else R.string.templates_reset))
                }
            }
        }
    }
}
