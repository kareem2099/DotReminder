package com.FreeRave.remindme.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthenticatedAiClientTest {
    private val input = AiBackendRequest("chat", "A fake review message", 1_793_750_400_000, "Africa/Cairo")
    private class Session : AiAccountSession {
        var uid: String? = "fake-user"
        val refreshes = mutableListOf<Boolean>()
        var onToken: () -> Unit = {}
        override fun currentUid() = uid
        override suspend fun idToken(expectedUid: String, forceRefresh: Boolean): String {
            refreshes += forceRefresh
            onToken()
            return if (forceRefresh) "fake-fresh-token" else "fake-original-token"
        }
    }
    @Test fun signedOutRequestsNeverReachTheBackend() = runBlocking {
        val session = Session().apply { uid = null }
        var calls = 0
        val client = AuthenticatedAiClient(session) { _, _ -> calls++; AiBackendResponse(200, "reply") }
        expect<AiServiceUnavailableException> { client.request(input) }
        assertEquals(0, calls)
    }
    @Test fun expiredTokenRefreshesOnceAndResendsTheSameRequest() = runBlocking {
        val session = Session()
        val tokens = mutableListOf<String>()
        val client = AuthenticatedAiClient(session) { token, request ->
            assertEquals(input, request)
            tokens += token
            AiBackendResponse(if (tokens.size == 1) 401 else 200, "reply")
        }
        assertEquals("reply", client.request(input))
        assertEquals(listOf(false, true), session.refreshes)
        assertEquals(listOf("fake-original-token", "fake-fresh-token"), tokens)
    }
    @Test fun repeatedUnauthorizedResponseDoesNotLoop() = runBlocking {
        val session = Session()
        var calls = 0
        val client = AuthenticatedAiClient(session) { _, _ -> calls++; AiBackendResponse(401, null) }
        expect<AiServiceUnavailableException> { client.request(input) }
        assertEquals(2, calls)
    }
    @Test fun accountChangeDuringTokenRetrievalPreventsSending() = runBlocking {
        val session = Session().apply { onToken = { uid = "different-user" } }
        var calls = 0
        val client = AuthenticatedAiClient(session) { _, _ -> calls++; AiBackendResponse(200, "reply") }
        expect<AiServiceUnavailableException> { client.request(input) }
        assertEquals(0, calls)
    }
    @Test fun accountChangeDuringRequestDiscardsThePreviousAccountsReply() = runBlocking {
        val session = Session()
        val client = AuthenticatedAiClient(session) { _, _ -> session.uid = "different-user"; AiBackendResponse(200, "previous reply") }
        expect<AiServiceUnavailableException> { client.request(input) }
    }
    @Test fun rateLimitIsShownWithoutAutomaticRetries() = runBlocking {
        var calls = 0
        val client = AuthenticatedAiClient(Session()) { _, _ -> calls++; AiBackendResponse(429, null) }
        expect<AiRateLimitException> { client.request(input) }
        assertEquals(1, calls)
    }
    @Test fun cancellationDuringAuthenticationIsPreserved() = runBlocking {
        val session = Session().apply { onToken = { throw CancellationException("Fake cancellation") } }
        var calls = 0
        val client = AuthenticatedAiClient(session) { _, _ -> calls++; AiBackendResponse(200, "reply") }
        expect<CancellationException> { client.request(input) }
        assertEquals(0, calls)
    }
    @Test fun invalidSuccessPayloadIsNotRendered() = runBlocking {
        val client = AuthenticatedAiClient(Session()) { _, _ -> AiBackendResponse(200, " ") }
        expect<AiServiceUnavailableException> { client.request(input) }
    }
    private suspend inline fun <reified T : Throwable> expect(block: suspend () -> Unit) {
        try { block(); fail("Expected ${T::class.java.simpleName}") }
        catch (error: Throwable) { if (error !is T) throw error }
    }
}
