package com.FreeRave.remindme.screens.ai.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.ai.AiChatError
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun AiChatThinking() {
    GlassSurface {
        Row(Modifier.padding(18.dp).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.ai_chat_thinking), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun AiChatErrorCard(error: AiChatError, canRetry: Boolean, onRetry: () -> Unit, onManual: () -> Unit) {
    val label = when (error) {
        AiChatError.NETWORK -> R.string.ai_chat_error_network
        AiChatError.UNAVAILABLE -> R.string.ai_chat_error_unavailable
        AiChatError.RESPONSE -> R.string.ai_chat_error_response
        AiChatError.SERVICE -> R.string.ai_chat_error_service
        AiChatError.TIMEOUT -> R.string.ai_chat_error_timeout
        AiChatError.RATE_LIMIT -> R.string.ai_chat_error_rate_limit
    }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(label), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (error != AiChatError.UNAVAILABLE) TextButton(onClick = onRetry, enabled = canRetry) { Text(stringResource(R.string.ai_chat_retry)) }
            TextButton(onClick = onManual) { Text(stringResource(R.string.ai_chat_manual)) }
        }
    }
}
