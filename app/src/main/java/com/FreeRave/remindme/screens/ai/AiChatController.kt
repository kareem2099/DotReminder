package com.FreeRave.remindme.screens.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

internal fun interface AiChatGateway { suspend fun reply(history: List<ChatMessage>): AiChatReply }

internal class AiChatController(private val gateway: AiChatGateway, private val timeoutMillis: Long = 45_000) {
    private val _state = MutableStateFlow(AiChatUiState())
    val state = _state.asStateFlow()
    fun updateInput(value: String) { _state.value = _state.value.copy(input = value) }
    fun clearConversation() {
        if (!_state.value.busy) _state.value = AiChatUiState()
    }
    fun markSaved(draftId: String, reminderId: String) {
        if (reminderId.isBlank()) return
        _state.value = _state.value.copy(messages = _state.value.messages.map {
            if (it.draft?.id == draftId) it.copy(savedReminderId = reminderId) else it
        })
    }
    suspend fun send() {
        val state = _state.value
        if (!state.canSend) return
        val message = ChatMessage(text = state.input.trim(), sender = SenderType.USER)
        _state.value = state.copy(input = "", messages = state.messages + message, busy = true, error = null, failedMessageId = null)
        requestReply(message.id)
    }
    suspend fun retry() {
        val state = _state.value
        if (!state.canRetry) return
        val id = state.failedMessageId ?: return
        if (state.messages.lastOrNull()?.id != id) return
        _state.value = state.copy(busy = true, error = null)
        requestReply(id)
    }
    private suspend fun requestReply(messageId: String) {
        try {
            val reply = withTimeout(timeoutMillis) { gateway.reply(_state.value.messages) }
            if (reply.text.isBlank() && reply.draft == null) throw AiChatException(AiChatError.RESPONSE)
            _state.value = _state.value.copy(messages = _state.value.messages + ChatMessage(
                text = reply.text, sender = SenderType.AI, draft = reply.draft), failedMessageId = null)
        } catch (_: TimeoutCancellationException) { fail(AiChatError.TIMEOUT, messageId) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { fail((e as? AiChatException)?.reason ?: AiChatError.SERVICE, messageId) }
        finally { _state.value = _state.value.copy(busy = false) }
    }
    private fun fail(error: AiChatError, messageId: String) {
        _state.value = _state.value.copy(error = error, failedMessageId = messageId)
    }
}
