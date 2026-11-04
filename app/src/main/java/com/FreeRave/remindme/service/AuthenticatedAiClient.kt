package com.FreeRave.remindme.service

import kotlinx.coroutines.CancellationException
import java.io.IOException

internal data class AiBackendRequest(val operation: String, val text: String, val nowMillis: Long, val timeZone: String)
internal data class AiBackendResponse(val status: Int, val reply: String?)
internal interface AiAccountSession {
    fun currentUid(): String?
    suspend fun idToken(expectedUid: String, forceRefresh: Boolean): String
}
internal fun interface AiBackendTransport {
    suspend fun send(token: String, request: AiBackendRequest): AiBackendResponse
}
class AiServiceUnavailableException : IllegalStateException()
class AiRateLimitException : IllegalStateException()

/** Binds a request to its original account and retries an expired bearer token once, never indefinitely. */
internal class AuthenticatedAiClient(private val session: AiAccountSession, private val transport: AiBackendTransport) {
    suspend fun request(input: AiBackendRequest): String {
        val uid = session.currentUid() ?: throw AiServiceUnavailableException()
        repeat(2) { attempt ->
            val token = try { session.idToken(uid, forceRefresh = attempt == 1) }
            catch (error: CancellationException) { throw error }
            catch (error: IOException) { throw error }
            catch (_: Exception) { throw AiServiceUnavailableException() }
            checkAccount(uid)
            if (token.isBlank()) throw AiServiceUnavailableException()
            val response = transport.send(token, input)
            checkAccount(uid)
            if (response.status == 401 && attempt == 0) return@repeat
            when (response.status) {
                200 -> return response.reply?.takeIf { it.isNotBlank() && it.length <= 16_000 }
                    ?: throw AiServiceUnavailableException()
                429 -> throw AiRateLimitException()
                else -> throw AiServiceUnavailableException()
            }
        }
        throw AiServiceUnavailableException()
    }
    private fun checkAccount(uid: String) {
        if (session.currentUid() != uid) throw AiServiceUnavailableException()
    }
}
