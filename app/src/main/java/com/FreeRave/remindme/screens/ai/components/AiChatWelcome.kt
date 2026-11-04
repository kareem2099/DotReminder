package com.FreeRave.remindme.screens.ai.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun AiChatWelcome(onSuggestion: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        GlassSurface(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(36.dp), MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.ai_chat_empty_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.ai_chat_empty_intro), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(stringResource(R.string.ai_chat_examples), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Suggestion(R.string.ai_chat_example_call, Icons.Outlined.Call, onSuggestion)
        Suggestion(R.string.ai_chat_example_water, Icons.Outlined.WaterDrop, onSuggestion)
        Suggestion(R.string.ai_chat_example_study, Icons.AutoMirrored.Outlined.MenuBook, onSuggestion)
    }
}

@Composable
private fun Suggestion(label: Int, icon: ImageVector, onClick: (String) -> Unit) {
    val text = stringResource(label)
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), onClick = { onClick(text) }) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
