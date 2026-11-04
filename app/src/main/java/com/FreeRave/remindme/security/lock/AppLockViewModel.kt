package com.FreeRave.remindme.security.lock

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.service.BiometricAuthManager
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppLockViewModel @Inject constructor(
    private val session: AppLockSession,
    private val preferences: UserPreferencesRepository,
    private val authService: FirebaseAuthServiceInterface,
    private val auth: FirebaseAuth,
    private val biometrics: BiometricAuthManager
) : ViewModel() {
    val state = session.controller.state
    private var policyJob: Job? = null
    init { retryPolicy() }
    fun retryPolicy() {
        policyJob?.cancel()
        policyJob = viewModelScope.launch {
            observeAppLockPolicy(authService.currentUserIdFlow(), preferences.biometricLockPreference, session.controller)
        }
    }
    fun start() = session.controller.start()
    fun stop() = session.controller.stop()
    fun authenticate(activity: FragmentActivity): BiometricPrompt? {
        val uid = auth.currentUser?.uid ?: return null
        val attempt = session.controller.begin(uid) ?: return null
        if (biometrics.getBiometricAvailability(activity) != BiometricAvailability.AVAILABLE) {
            session.controller.reject(attempt, AppLockError.UNAVAILABLE)
            return null
        }
        return try {
            biometrics.authenticate(activity, activity.getString(R.string.app_lock_title),
                activity.getString(R.string.app_lock_prompt), activity.getString(R.string.security_cancel),
                onSuccess = {
                    if (auth.currentUser?.uid == uid) session.controller.accept(attempt)
                    else session.controller.reject(attempt, AppLockError.FAILED)
                }, onFailedAttempt = { /* The system prompt remains open and enforces biometric retry limits. */ },
                onError = { code, _ -> session.controller.reject(attempt, when (code) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED -> AppLockError.CANCELLED
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AppLockError.LOCKOUT
                    else -> AppLockError.UNAVAILABLE
                }) })
        } catch (_: RuntimeException) { session.controller.reject(attempt, AppLockError.UNAVAILABLE); null }
    }
    fun signInAgain(onSignedOut: () -> Unit) {
        if (state.value.phase == AppLockPhase.SIGNING_OUT) return
        session.controller.beginSignOut()
        viewModelScope.launch {
            try {
                auth.signOut()
                check(auth.currentUser == null)
                onSignedOut()
                session.controller.finishSignOut(true)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { session.controller.finishSignOut(false) }
        }
    }
    override fun onCleared() { session.controller.stop() }
}
