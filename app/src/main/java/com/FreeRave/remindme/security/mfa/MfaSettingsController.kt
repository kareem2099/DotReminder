package com.FreeRave.remindme.security.mfa

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal enum class MfaSettingsStep { NONE, REAUTHENTICATE, CHALLENGE, ENROLL }
internal sealed interface MfaAction {
    data object Add : MfaAction
    data class Remove(val factor: MfaFactor) : MfaAction
}
internal enum class MfaNotice { ADDED, REMOVED }
internal class MfaSetup(val secret: String, val qrUri: String) {
    override fun toString() = "MfaSetup(redacted)"
}
internal data class MfaSettingsState(
    val account: MfaAccount? = null, val loading: Boolean = true, val busy: Boolean = false,
    val step: MfaSettingsStep = MfaSettingsStep.NONE, val action: MfaAction? = null,
    val setup: MfaSetup? = null, val challenge: MfaChallengeState? = null,
    val error: MfaError? = null, val notice: MfaNotice? = null
)

/** The remote enrolled-factor list is the only source of truth. No local "enabled" flag. */
internal class MfaSettingsController(
    private val gateway: MfaAccountGateway,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val mutableState = MutableStateFlow(MfaSettingsState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var enrollment: MfaEnrollment? = null
    private var challenge: MfaChallenge? = null
    private var expectedUid: String? = null
    private var generation = 0L

    fun refresh() {
        if (state.value.busy || state.value.step != MfaSettingsStep.NONE) return
        launchOperation {
            mutableState.value = state.value.copy(loading = true, error = null)
            val account = gateway.account()
            currentCoroutineContext().ensureActive()
            mutableState.value = MfaSettingsState(account, loading = false,
                error = if (account == null) MfaError.SIGN_IN_REQUIRED else null)
        }
    }

    fun accountChanged() {
        clearSensitiveState()
        mutableState.value = MfaSettingsState()
        refresh()
    }

    fun request(action: MfaAction) {
        val current = state.value
        if (current.busy || current.loading || current.step != MfaSettingsStep.NONE) return
        val account = current.account ?: return
        val issue = when {
            !account.emailVerified || account.email.isBlank() -> MfaError.EMAIL_UNVERIFIED
            account.providers.isEmpty() -> MfaError.UNSUPPORTED
            action is MfaAction.Add && account.factors.size >= 5 -> MfaError.LIMIT
            action is MfaAction.Remove && account.factors.none { it.id == action.factor.id } -> MfaError.ACCOUNT_CHANGED
            else -> null
        }
        if (issue != null) { mutableState.value = current.copy(error = issue); return }
        expectedUid = account.uid
        mutableState.value = current.copy(action = action, step = MfaSettingsStep.REAUTHENTICATE, error = null, notice = null)
    }

    /** The provider credential stays inside the reauthentication service, never in UI state. */
    fun reauthenticate(verifyIdentity: suspend () -> Unit) {
        if (state.value.busy || state.value.step != MfaSettingsStep.REAUTHENTICATE) return
        launchOperation {
            try {
                verifyIdentity()
                continueAction()
            } catch (required: MfaRequired) {
                challenge = required.challenge
                val factors = required.challenge.factors
                mutableState.value = state.value.copy(step = MfaSettingsStep.CHALLENGE,
                    challenge = MfaChallengeState(factors, factors.firstOrNull()?.id,
                        error = if (factors.isEmpty()) MfaError.UNSUPPORTED else null))
            }
        }
    }

    fun selectFactor(id: String) {
        val current = state.value
        val pending = current.challenge ?: return
        if (!current.busy && pending.factors.any { it.id == id })
            mutableState.value = current.copy(challenge = pending.copy(selectedId = id, error = null))
    }

    fun verifyChallenge(code: String) {
        val pending = challenge ?: return
        val id = state.value.challenge?.selectedId ?: return
        if (state.value.busy || state.value.step != MfaSettingsStep.CHALLENGE) return
        if (!validMfaCode(code)) { setError(MfaError.INVALID_CODE); return }
        launchOperation {
            val user = pending.verify(id, code)
            if (user.id != expectedUid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
            challenge = null
            continueAction()
        }
    }

    private suspend fun continueAction() {
        val uid = expectedUid ?: throw MfaFailure(MfaError.ACCOUNT_CHANGED)
        val account = gateway.account() ?: throw MfaFailure(MfaError.SIGN_IN_REQUIRED)
        currentCoroutineContext().ensureActive()
        if (account.uid != uid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
        when (val action = state.value.action) {
            MfaAction.Add -> {
                val pending = gateway.beginEnrollment(uid)
                currentCoroutineContext().ensureActive()
                enrollment = pending
                mutableState.value = state.value.copy(account = account, step = MfaSettingsStep.ENROLL,
                    setup = MfaSetup(pending.secret, pending.qrUri), challenge = null)
            }
            is MfaAction.Remove -> {
                gateway.removeFactor(uid, action.factor.id)
                currentCoroutineContext().ensureActive()
                clearHandles()
                mutableState.value = state.value.copy(account = null, step = MfaSettingsStep.NONE, action = null,
                    setup = null, challenge = null, notice = MfaNotice.REMOVED)
                val updated = gateway.account() ?: throw MfaFailure(MfaError.SIGN_IN_REQUIRED)
                currentCoroutineContext().ensureActive()
                if (updated.uid != uid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
                clearHandles()
                mutableState.value = MfaSettingsState(updated, loading = false, notice = MfaNotice.REMOVED)
            }
            null -> throw MfaFailure(MfaError.GENERIC)
        }
    }

    fun enroll(code: String, name: String) {
        val pending = enrollment ?: return
        if (state.value.busy || state.value.step != MfaSettingsStep.ENROLL) return
        if (!validMfaCode(code)) { setError(MfaError.INVALID_CODE); return }
        if (pending.expiresAtMillis <= now()) {
            clearHandles()
            mutableState.value = state.value.copy(step = MfaSettingsStep.NONE, action = null, setup = null, error = MfaError.EXPIRED)
            return
        }
        launchOperation {
            pending.enroll(code, name.trim().take(40).ifBlank { "Authenticator" })
            currentCoroutineContext().ensureActive()
            val uid = expectedUid
            // A committed enrollment must never leave a reusable setup key on screen, even if reload fails.
            clearHandles()
            mutableState.value = state.value.copy(account = null, step = MfaSettingsStep.NONE, action = null,
                setup = null, challenge = null, notice = MfaNotice.ADDED)
            val account = gateway.account() ?: throw MfaFailure(MfaError.SIGN_IN_REQUIRED)
            currentCoroutineContext().ensureActive()
            if (account.uid != uid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
            clearHandles()
            mutableState.value = MfaSettingsState(account, loading = false, notice = MfaNotice.ADDED)
        }
    }

    fun edit() {
        if (!state.value.busy) mutableState.value = state.value.copy(error = null,
            challenge = state.value.challenge?.copy(error = null))
    }

    fun cancel() {
        if (state.value.busy) return
        clearSensitiveState()
    }

    fun clearSensitiveState() {
        generation++
        job?.cancel()
        job = null
        clearHandles()
        mutableState.value = state.value.copy(busy = false, loading = false, step = MfaSettingsStep.NONE,
            action = null, setup = null, challenge = null, error = null, notice = null)
    }

    fun consumeNotice() { mutableState.value = state.value.copy(notice = null) }
    private fun clearHandles() { enrollment = null; challenge = null; expectedUid = null }
    private fun setError(reason: MfaError) {
        if (state.value.step == MfaSettingsStep.CHALLENGE) mutableState.value = state.value.copy(
            challenge = state.value.challenge?.copy(error = reason))
        else mutableState.value = state.value.copy(error = reason)
    }
    private fun launchOperation(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true, error = null,
            challenge = state.value.challenge?.copy(error = null))
        val token = ++generation
        job = scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: MfaCancelled) { /* The user can choose their provider again. */ }
            catch (e: Exception) {
                if (token != generation) return@launch
                val error = (e as? MfaFailure)?.reason ?: MfaError.GENERIC
                if (error in listOf(MfaError.ACCOUNT_CHANGED, MfaError.SIGN_IN_REQUIRED, MfaError.EXPIRED)) {
                    clearHandles()
                    mutableState.value = state.value.copy(step = MfaSettingsStep.NONE, action = null, setup = null, challenge = null,
                        account = if (error == MfaError.SIGN_IN_REQUIRED || error == MfaError.ACCOUNT_CHANGED) null else state.value.account)
                }
                setError(error)
            } finally { if (token == generation) mutableState.value = state.value.copy(busy = false, loading = false) }
        }
    }
}
