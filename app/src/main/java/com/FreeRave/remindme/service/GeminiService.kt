package com.FreeRave.remindme.service

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Gemini is called only by DotSuite. The Android app supplies a Firebase ID token, never a Gemini key. */
@Singleton
class GeminiService @Inject constructor(auth: FirebaseAuth) {
    private val backend = AuthenticatedAiClient(FirebaseAiAccountSession(auth), DotSuiteAiTransport())

    suspend fun generateChatReply(conversation: String): String = backend.request(request("chat", conversation))

    /** The reminder form retains its existing offline parsing fallback when extraction is unavailable. */
    suspend fun parseReminderToJSON(userInput: String): String? = try {
        backend.request(request("extract", userInput))
    } catch (error: CancellationException) { throw error }
    catch (_: Exception) { null }

    private fun request(operation: String, text: String): AiBackendRequest {
        val now = ZonedDateTime.now()
        return AiBackendRequest(operation, text, now.toInstant().toEpochMilli(), now.zone.id)
    }
}
