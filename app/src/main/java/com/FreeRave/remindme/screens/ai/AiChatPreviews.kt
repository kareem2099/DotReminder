package com.FreeRave.remindme.screens.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

private val chatPreviewMessages = listOf(ChatMessage(text = "Remind me to call Mum", sender = SenderType.USER),
    ChatMessage(text = "When would you like me to remind you?", sender = SenderType.AI),
    ChatMessage(text = "Tomorrow at 6 pm", sender = SenderType.USER))
@Preview(name = "Assistant · Light", widthDp = 393, heightDp = 852)
@Composable private fun AiLightPreview() = AiPreview(false)
@Preview(name = "Assistant · Dark", widthDp = 393, heightDp = 852)
@Composable private fun AiDarkPreview() = AiPreview(true)
@Preview(name = "Assistant · Compact / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun AiCompactPreview() = AiPreview(false)
@Preview(name = "Assistant · Conversation", widthDp = 393, heightDp = 852)
@Composable private fun AiConversationPreview() = AiPreview(true, AiChatUiState(messages = chatPreviewMessages))
@Preview(name = "Assistant · Draft", widthDp = 393, heightDp = 852)
@Composable private fun AiDraftPreview() = AiPreview(true, AiChatUiState(messages = chatPreviewMessages + ChatMessage(sender = SenderType.AI,
    draft = AiReminderDraft("Call Mum", System.currentTimeMillis() + 86_400_000, category = "Personal"))))
@Preview(name = "Assistant · Loading", widthDp = 393, heightDp = 852)
@Composable private fun AiLoadingPreview() = AiPreview(true, AiChatUiState(messages = chatPreviewMessages, busy = true))
@Preview(name = "Assistant · Error", widthDp = 393, heightDp = 852)
@Composable private fun AiErrorPreview() = AiPreview(false, AiChatUiState(messages = chatPreviewMessages,
    error = AiChatError.NETWORK, failedMessageId = chatPreviewMessages.last().id))

@Composable
private fun AiPreview(dark: Boolean, state: AiChatUiState = AiChatUiState()) {
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            AiChatScreenContent(state, onBack = {}, onInput = {}, onSend = {}, onRetry = {}, onClear = {}, onReview = {}, onManual = {})
        }
    }
}
