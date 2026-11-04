package com.FreeRave.remindme.screens.ai.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.ai.AI_MESSAGE_LIMIT
import com.FreeRave.remindme.screens.ai.AiChatUiState
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun AiChatComposer(state: AiChatUiState, onInput: (String) -> Unit, onSend: () -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().testTag("ai-chat-composer"), contentAlignment = Alignment.Center) {
        GlassSurface(Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(state.input, onInput, Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                    label = { Text(stringResource(R.string.ai_chat_input)) },
                    placeholder = { Text(stringResource(R.string.ai_chat_placeholder)) }, maxLines = 4,
                    isError = state.inputTooLong,
                    supportingText = if (state.input.length > AI_MESSAGE_LIMIT * 0.8) {{
                        Text(stringResource(R.string.ai_chat_input_limit, state.input.length, AI_MESSAGE_LIMIT))
                    }} else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (state.canSend) onSend() }))
                FilledIconButton(onClick = onSend, enabled = state.canSend, modifier = Modifier.padding(bottom = 4.dp).size(48.dp),
                    shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.ai_chat_send), Modifier.size(22.dp))
                }
            }
        }
    }
}
