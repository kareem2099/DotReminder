package com.FreeRave.remindme.screens.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface EmailVerificationGateway {
    suspend fun readAccount(reload: Boolean): VerificationAccount?
    suspend fun sendLink(account: VerificationAccount)
    suspend fun syncVerifiedAccount(account: VerificationAccount)
}
internal class EmailVerificationException(val reason: EmailVerificationError) : Exception()

/** One request at a time; an email link is sent only after an explicit action. */
internal class EmailVerificationController(
    private val gateway: EmailVerificationGateway,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private val _state = MutableStateFlow(EmailVerificationUiState())
    val state = _state.asStateFlow()
    private var resendDeadline: Long? = null
    private var syncedAccount: Pair<String, String>? = null

    suspend fun refresh(showPending: Boolean = false) {
        if (!begin(EmailVerificationOperation.CHECK)) return
        try {
            val account = gateway.readAccount(reload = true)
            adopt(account)
            if (account?.verified == true && account.email.isNotBlank()) sync(account)
            else if (showPending && _state.value.phase == EmailVerificationPhase.READY) {
                _state.value = _state.value.copy(notice = EmailVerificationNotice.PENDING)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (_state.value.phase == EmailVerificationPhase.INITIAL) {
                _state.value = _state.value.copy(phase = EmailVerificationPhase.LOAD_ERROR)
            }
            fail(e, EmailVerificationError.CHECK)
        } finally { finish() }
    }

    suspend fun sendLink() {
        if (!_state.value.canSend) return
        val expected = _state.value.account ?: return
        if (!begin(EmailVerificationOperation.SEND)) return
        try {
            val current = gateway.readAccount(reload = false)
            if (!sameAccount(expected, current)) { changed(current); return }
            current ?: return
            if (current.verified) { adopt(current); sync(current); return }
            gateway.sendLink(current)
            resendDeadline = clockMillis() + 60_000
            tick()
            _state.value = _state.value.copy(notice = EmailVerificationNotice.SENT, hasSentLink = true)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { fail(e, EmailVerificationError.SEND) }
        finally { finish() }
    }

    suspend fun retrySync() {
        val expected = _state.value.account ?: return
        if (_state.value.phase != EmailVerificationPhase.VERIFIED || !begin(EmailVerificationOperation.SYNC)) return
        try {
            val current = gateway.readAccount(reload = false)
            if (!sameAccount(expected, current)) { changed(current); return }
            adopt(current)
            if (current?.verified == true) sync(current)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { fail(e, EmailVerificationError.SYNC) }
        finally { finish() }
    }

    fun tick() {
        val seconds = resendDeadline?.let { verificationResendSeconds(it, clockMillis()) } ?: 0
        if (_state.value.resendSeconds != seconds) _state.value = _state.value.copy(resendSeconds = seconds)
    }

    private fun begin(operation: EmailVerificationOperation): Boolean {
        if (_state.value.operation != null) return false
        _state.value = _state.value.copy(operation = operation, error = null)
        return true
    }
    private fun finish() { _state.value = _state.value.copy(operation = null) }
    private fun adopt(account: VerificationAccount?) {
        val previous = _state.value.account
        if (!sameAccount(previous, account)) { resendDeadline = null; syncedAccount = null }
        _state.value = _state.value.copy(account = account, error = null,
            notice = if (sameAccount(previous, account)) _state.value.notice else null,
            hasSentLink = sameAccount(previous, account) && _state.value.hasSentLink,
            phase = when {
                account == null -> EmailVerificationPhase.SIGNED_OUT
                account.email.isBlank() -> EmailVerificationPhase.NO_EMAIL
                account.verified -> EmailVerificationPhase.VERIFIED
                else -> EmailVerificationPhase.READY
            })
        tick()
    }
    private fun changed(account: VerificationAccount?) {
        adopt(account)
        _state.value = _state.value.copy(error = EmailVerificationError.ACCOUNT_CHANGED)
    }
    private suspend fun sync(account: VerificationAccount) {
        val identity = account.id to account.email
        if (syncedAccount == identity) return
        try { gateway.syncVerifiedAccount(account); syncedAccount = identity }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if ((e as? EmailVerificationException)?.reason == EmailVerificationError.ACCOUNT_CHANGED) fail(e, EmailVerificationError.SYNC)
            else _state.value = _state.value.copy(error = EmailVerificationError.SYNC)
        }
    }
    private fun fail(e: Exception, fallback: EmailVerificationError) {
        val reason = (e as? EmailVerificationException)?.reason ?: fallback
        if (reason == EmailVerificationError.ACCOUNT_CHANGED) {
            resendDeadline = null
            syncedAccount = null
            _state.value = _state.value.copy(phase = EmailVerificationPhase.LOAD_ERROR, account = null,
                notice = null, resendSeconds = 0, error = reason, hasSentLink = false)
        } else _state.value = _state.value.copy(error = reason)
    }
    private fun sameAccount(a: VerificationAccount?, b: VerificationAccount?) =
        a?.id == b?.id && a?.email == b?.email
}

internal fun verificationResendSeconds(deadline: Long, now: Long): Int {
    val remaining = (deadline - now).coerceAtLeast(0)
    val roundedSeconds = remaining / 1_000 + if (remaining % 1_000 != 0L) 1 else 0
    return roundedSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
