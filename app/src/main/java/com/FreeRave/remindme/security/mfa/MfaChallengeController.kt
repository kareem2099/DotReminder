package com.FreeRave.remindme.security.mfa

import com.FreeRave.remindme.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** No success callback is reachable until the server resolves the second factor. */
internal class MfaChallengeController(
    private val scope: CoroutineScope,
    private val onVerified: (User) -> Unit,
    private val onCancelled: () -> Unit
) {
    private var challenge: MfaChallenge? = null
    private var job: Job? = null
    private val mutableState = MutableStateFlow<MfaChallengeState?>(null)
    val state = mutableState.asStateFlow()

    fun present(value: MfaChallenge) {
        clear()
        challenge = value
        mutableState.value = MfaChallengeState(value.factors, value.factors.firstOrNull()?.id,
            error = if (value.factors.isEmpty()) MfaError.UNSUPPORTED else null)
    }

    fun select(id: String) {
        val current = mutableState.value ?: return
        if (!current.busy && current.factors.any { it.id == id }) mutableState.value = current.copy(selectedId = id, error = null)
    }

    fun edit() {
        mutableState.value?.takeUnless { it.busy }?.let { mutableState.value = it.copy(error = null) }
    }

    fun verify(code: String) {
        val current = mutableState.value ?: return
        val pending = challenge ?: return
        val factor = current.selectedId ?: return
        if (current.busy) return
        if (!validMfaCode(code)) { mutableState.value = current.copy(error = MfaError.INVALID_CODE); return }
        mutableState.value = current.copy(busy = true, error = null)
        job = scope.launch {
            try {
                val user = pending.verify(factor, code)
                if (challenge !== pending) return@launch
                challenge = null
                mutableState.value = null
                onVerified(user)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (challenge === pending) mutableState.value = current.copy(error = (e as? MfaFailure)?.reason ?: MfaError.GENERIC)
            }
        }
    }

    fun cancel() {
        if (mutableState.value?.busy == true) return
        clear()
        onCancelled()
    }

    fun clear() {
        job?.cancel()
        job = null
        challenge = null
        mutableState.value = null
    }
}
