package com.FreeRave.remindme.security

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

internal class AuthNetworkTimeout : Exception()

/** Bound network waits without turning cancellation of the caller into a retryable error. */
internal suspend fun <T> authNetworkCall(timeoutMillis: Long = 30_000, block: suspend () -> T): T =
    try { withTimeout(timeoutMillis) { block() } }
    catch (error: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw AuthNetworkTimeout()
    }

/** Native account pickers remain user controlled; only the Firebase task gets a deadline. */
internal suspend fun <T> Task<T>.awaitAuth(): T = authNetworkCall { await() }
