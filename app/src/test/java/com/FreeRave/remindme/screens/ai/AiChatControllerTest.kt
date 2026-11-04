package com.FreeRave.remindme.screens.ai

import com.FreeRave.remindme.model.AiReminderDraft
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AiChatControllerTest {
    private class Gateway : AiChatGateway {
        val histories = mutableListOf<List<ChatMessage>>()
        var response = AiChatReply(text = "When would you like to do that?")
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun reply(history: List<ChatMessage>): AiChatReply {
            histories += history.toList()
            gate?.await()
            failure?.let { throw it }
            return response
        }
    }
    @Test fun blankInputDoesNotSendARequest() = runBlocking {
        val gateway = Gateway()
        val controller = AiChatController(gateway)
        controller.updateInput("  \n  "); controller.send()
        assertTrue(gateway.histories.isEmpty())
        assertTrue(controller.state.value.messages.isEmpty())
    }
    @Test fun oversizedInputIsPreservedButCannotBeSent() = runBlocking {
        val gateway = Gateway()
        val controller = AiChatController(gateway)
        val input = "a".repeat(AI_MESSAGE_LIMIT + 1)
        controller.updateInput(input); controller.send()
        assertTrue(gateway.histories.isEmpty())
        assertEquals(input, controller.state.value.input)
        assertTrue(controller.state.value.inputTooLong)
    }
    @Test fun sendsTrimmedMessageAndKeepsChronologicalReply() = runBlocking {
        val gateway = Gateway()
        val controller = AiChatController(gateway)
        controller.updateInput("  Remind me to call Mum  "); controller.send()
        assertEquals("Remind me to call Mum", gateway.histories.single().single().text)
        assertEquals(listOf(SenderType.USER, SenderType.AI), controller.state.value.messages.map { it.sender })
        assertEquals("", controller.state.value.input)
        assertFalse(controller.state.value.busy)
    }
    @Test fun pendingRequestBlocksDuplicateSubmissionsWithoutErasingNextDraft() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val gateway = Gateway().apply { this.gate = gate }
        val controller = AiChatController(gateway)
        controller.updateInput("Call Mum tomorrow")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.send() }
        controller.updateInput("Another thought")
        controller.send()
        assertEquals(1, gateway.histories.size)
        gate.complete(Unit); job.join()
        assertEquals("Another thought", controller.state.value.input)
    }
    @Test fun retryUsesTheFailedTurnWithoutDuplicatingTheUserMessage() = runBlocking {
        val gateway = Gateway().apply { failure = AiChatException(AiChatError.NETWORK) }
        val controller = AiChatController(gateway)
        controller.updateInput("Call Mum tomorrow"); controller.send()
        assertTrue(controller.state.value.canRetry)
        controller.updateInput("Next draft")
        gateway.failure = null
        controller.retry()
        assertEquals(2, gateway.histories.size)
        assertEquals(gateway.histories[0], gateway.histories[1])
        assertEquals(1, controller.state.value.messages.count { it.sender == SenderType.USER })
        assertEquals("Next draft", controller.state.value.input)
        assertNull(controller.state.value.error)
    }
    @Test fun laterTurnsIncludeTheActivityAndAssistantQuestion() = runBlocking {
        val gateway = Gateway()
        val controller = AiChatController(gateway)
        controller.updateInput("Call Mum"); controller.send()
        controller.updateInput("Tomorrow at 6 pm"); controller.send()
        assertEquals(listOf("Call Mum", gateway.response.text, "Tomorrow at 6 pm"), gateway.histories.last().map { it.text })
    }
    @Test fun emptyReplyProducesAnErrorWithoutAddingFakeAssistantText() = runBlocking {
        val gateway = Gateway().apply { response = AiChatReply() }
        val controller = AiChatController(gateway)
        controller.updateInput("Hello"); controller.send()
        assertEquals(AiChatError.RESPONSE, controller.state.value.error)
        assertEquals(1, controller.state.value.messages.size)
    }
    @Test fun typedServiceFailureIsShownWithoutRawExceptionText() = runBlocking {
        val controller = AiChatController(Gateway().apply { failure = AiChatException(AiChatError.UNAVAILABLE) })
        controller.updateInput("Hello"); controller.send()
        assertEquals(AiChatError.UNAVAILABLE, controller.state.value.error)
        assertEquals(1, controller.state.value.messages.size)
    }
    @Test fun requestTimeoutAllowsRetry() = runBlocking {
        val controller = AiChatController(Gateway().apply { gate = CompletableDeferred() }, timeoutMillis = 10)
        controller.updateInput("Call Mum"); controller.send()
        assertEquals(AiChatError.TIMEOUT, controller.state.value.error)
        assertTrue(controller.state.value.canRetry)
    }
    @Test fun cancellationDoesNotAddAnErrorMessage() = runBlocking {
        val controller = AiChatController(Gateway().apply { gate = CompletableDeferred() })
        controller.updateInput("Call Mum")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.send() }
        job.cancelAndJoin()
        assertFalse(controller.state.value.busy)
        assertNull(controller.state.value.error)
        assertEquals(1, controller.state.value.messages.size)
    }
    @Test fun clearingWhileBusyCannotDropThePendingTurn() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val controller = AiChatController(Gateway().apply { this.gate = gate })
        controller.updateInput("Call Mum")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.send() }
        controller.clearConversation()
        assertEquals(1, controller.state.value.messages.size)
        gate.complete(Unit); job.join()
    }
    @Test fun clearingAfterAReplyStartsAnEmptyConversation() = runBlocking {
        val controller = AiChatController(Gateway())
        controller.updateInput("Call Mum"); controller.send()
        controller.clearConversation()
        assertEquals(AiChatUiState(), controller.state.value)
    }
    @Test fun retryWithoutAFailureDoesNotMakeANetworkRequest() = runBlocking {
        val gateway = Gateway()
        val controller = AiChatController(gateway)
        controller.retry()
        assertTrue(gateway.histories.isEmpty())
    }
    @Test fun successfulFormSaveUpdatesOnlyTheMatchingDraftWithoutAnotherRequest() = runBlocking {
        val draft = AiReminderDraft("Call Mum", 2000)
        val gateway = Gateway().apply { response = AiChatReply(draft = draft) }
        val controller = AiChatController(gateway)
        controller.updateInput("Call Mum tomorrow"); controller.send()
        controller.markSaved("another-draft", "reminder-1")
        assertNull(controller.state.value.messages.last().savedReminderId)
        controller.markSaved(draft.id, "reminder-1")
        assertEquals("reminder-1", controller.state.value.messages.last().savedReminderId)
        assertEquals(1, gateway.histories.size)
    }
}
