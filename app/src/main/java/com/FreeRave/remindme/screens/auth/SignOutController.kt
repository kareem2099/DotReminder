package com.FreeRave.remindme.screens.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

internal interface SignOutGateway {
    suspend fun endSession()
    fun hasSession(): Boolean
    suspend fun clearProviderSessions()
}

/** A failed provider cleanup must never leave Firebase signed in behind a success screen. */
internal class SignOutController(
    private val gateway: SignOutGateway,
    account: SignOutAccount? = null,
    private val cleanupTimeoutMillis: Long = 5_000
) {
    private val _state = MutableStateFlow(SignOutUiState(account))
    val state = _state.asStateFlow()

    suspend fun signOut() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(status = SignOutStatus.BUSY)
        try {
            gateway.endSession()
            check(!gateway.hasSession())
            try { withTimeout(cleanupTimeoutMillis) { gateway.clearProviderSessions() } }
            catch (_: TimeoutCancellationException) { /* Local sign-out has already completed. */ }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Credential-provider cleanup is best effort. */ }
            _state.value = _state.value.copy(status = if (gateway.hasSession()) SignOutStatus.ERROR else SignOutStatus.COMPLETE)
        } catch (e: CancellationException) {
            _state.value = _state.value.copy(status = if (gateway.hasSession()) SignOutStatus.READY else SignOutStatus.COMPLETE)
            throw e
        } catch (_: Exception) {
            _state.value = _state.value.copy(status = if (gateway.hasSession()) SignOutStatus.ERROR else SignOutStatus.COMPLETE)
        }
    }
}
