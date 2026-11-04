package com.FreeRave.remindme.service

import com.FreeRave.remindme.security.AuthNetworkTimeout
import com.FreeRave.remindme.security.awaitAuth
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FirebaseAiAccountSession(private val auth: FirebaseAuth) : AiAccountSession {
    override fun currentUid(): String? = auth.currentUser?.uid
    override suspend fun idToken(expectedUid: String, forceRefresh: Boolean): String {
        val user = auth.currentUser?.takeIf { it.uid == expectedUid } ?: throw AiServiceUnavailableException()
        return try { user.getIdToken(forceRefresh).awaitAuth().token.orEmpty() }
        catch (error: CancellationException) { throw error }
        catch (_: FirebaseNetworkException) { throw IOException("Authentication connection unavailable") }
        catch (_: AuthNetworkTimeout) { throw IOException("Authentication connection timed out") }
    }
}

/** No API key, cookie, provider URL override, or arbitrary prompt destination is accepted from the client. */
internal class DotSuiteAiTransport : AiBackendTransport {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(35, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    override suspend fun send(token: String, request: AiBackendRequest): AiBackendResponse {
        val payload = JSONObject().put("operation", request.operation).put("text", request.text)
            .put("nowMillis", request.nowMillis).put("timeZone", request.timeZone).toString()
        val httpRequest = Request.Builder().url("https://dotsuite.dev/api/dotreminder/ai")
            .header("Authorization", "Bearer $token").header("Accept", "application/json")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val call = client.newCall(httpRequest)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(IOException("AI connection unavailable"))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            if (it.code != 200) return@use AiBackendResponse(it.code, null)
                            val source = it.body.source()
                            if (source.request(65_537) && source.buffer.size > 65_536) throw IOException("AI response too large")
                            val json = JSONObject(source.readUtf8())
                            AiBackendResponse(it.code, json.optString("reply").takeIf(String::isNotBlank))
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(AiServiceUnavailableException())
                    }
                }
            })
        }
    }
}
