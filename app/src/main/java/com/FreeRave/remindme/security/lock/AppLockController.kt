package com.FreeRave.remindme.security.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class AppLockPhase { CHECKING, OPEN, LOCKED, AUTHENTICATING, SIGNING_OUT }
internal enum class AppLockError { CANCELLED, UNAVAILABLE, LOCKOUT, FAILED, PREFERENCES, SIGN_OUT }
internal data class AppLockState(
    val phase: AppLockPhase = AppLockPhase.CHECKING, val accountId: String? = null,
    val required: Boolean = false, val revision: Long = 0, val error: AppLockError? = null
) { val canReveal get() = phase == AppLockPhase.OPEN }
internal class BiometricAttempt internal constructor(val id: Long, val uid: String, val epoch: Long)

/** In-memory grants never survive a background transition, account change or process death. */
internal class AppLockController {
    private val mutableState = MutableStateFlow(AppLockState())
    val state = mutableState.asStateFlow()
    private var foreground = true
    private var epoch = 0L
    private var sequence = 0L
    private var grantUid: String? = null
    private var pending: BiometricAttempt? = null
    private var ready = false
    private var signingOut = false

    fun checkingPolicy(uid: String) {
        if (uid != state.value.accountId) {
            epoch++; pending = null
            if (grantUid != uid) grantUid = null
        }
        ready = false
        mutableState.value = state.value.copy(accountId = uid, required = true, error = null)
        publish()
    }

    fun policy(uid: String?, required: Boolean) {
        if (uid != state.value.accountId) {
            epoch++
            pending = null
            if (grantUid != uid) grantUid = null
        }
        ready = true
        mutableState.value = state.value.copy(accountId = uid, required = uid != null && required, error = null)
        publish()
    }
    fun preferenceFailure() {
        ready = false
        invalidate()
        mutableState.value = state.value.copy(phase = AppLockPhase.CHECKING, error = AppLockError.PREFERENCES)
    }
    fun start() { foreground = true; publish() }
    fun stop() {
        foreground = false
        invalidate()
        mutableState.value = state.value.copy(error = state.value.error.takeUnless { ready })
        publish()
    }
    private fun invalidate() { epoch++; pending = null; grantUid = null }

    fun begin(uid: String): BiometricAttempt? {
        if (!ready || !foreground || signingOut || state.value.accountId != uid || pending != null) return null
        return BiometricAttempt(++sequence, uid, epoch).also { pending = it; publish() }
    }
    fun accept(attempt: BiometricAttempt): Boolean {
        if (attempt != pending || attempt.epoch != epoch || attempt.uid != state.value.accountId || !foreground) return false
        pending = null
        grantUid = attempt.uid
        mutableState.value = state.value.copy(error = null)
        publish()
        return true
    }
    fun reject(attempt: BiometricAttempt, error: AppLockError) {
        if (attempt != pending) return
        pending = null
        mutableState.value = state.value.copy(error = error)
        publish()
    }

    /** Called only after a complete Firebase sign-in, including any required MFA. */
    fun verifiedSignIn(uid: String) {
        if (signingOut) return
        grantUid = uid
        pending = null
        mutableState.value = state.value.copy(error = state.value.error.takeUnless { ready })
        publish()
    }
    fun beginSignOut() { signingOut = true; invalidate(); publish() }
    fun finishSignOut(success: Boolean) {
        signingOut = false
        if (success) { ready = true; mutableState.value = state.value.copy(accountId = null, required = false, error = null) }
        else mutableState.value = state.value.copy(error = AppLockError.SIGN_OUT)
        publish()
    }
    private fun publish() {
        val current = state.value
        val phase = when {
            signingOut -> AppLockPhase.SIGNING_OUT
            !ready -> AppLockPhase.CHECKING
            current.accountId == null || !current.required -> AppLockPhase.OPEN
            foreground && grantUid == current.accountId -> AppLockPhase.OPEN
            pending != null -> AppLockPhase.AUTHENTICATING
            else -> AppLockPhase.LOCKED
        }
        val revision = current.revision + if (phase == AppLockPhase.LOCKED && current.phase != phase) 1 else 0
        mutableState.value = current.copy(phase = phase, revision = revision)
    }
}
