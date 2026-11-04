package com.FreeRave.remindme.screens.ai

import com.FreeRave.remindme.model.AiReminderDraft
import java.util.UUID

internal const val AI_MESSAGE_LIMIT = 4_000
internal enum class SenderType { USER, AI }
internal data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val sender: SenderType,
    val draft: AiReminderDraft? = null,
    val savedReminderId: String? = null
)
internal enum class AiChatError { NETWORK, UNAVAILABLE, RESPONSE, SERVICE, TIMEOUT, RATE_LIMIT }
internal data class AiChatUiState(
    val input: String = "", val messages: List<ChatMessage> = emptyList(),
    val busy: Boolean = false, val error: AiChatError? = null, val failedMessageId: String? = null
) {
    val inputTooLong get() = input.length > AI_MESSAGE_LIMIT
    val canSend get() = input.isNotBlank() && !inputTooLong && !busy
    val canRetry get() = !busy && error != null && failedMessageId != null
}
internal data class AiChatReply(val text: String = "", val draft: AiReminderDraft? = null)
internal class AiChatException(val reason: AiChatError) : Exception()
