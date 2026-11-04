package com.FreeRave.remindme.security.mfa

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MfaSettingsViewModel @Inject constructor(
    gateway: FirebaseMfaGateway,
    private val reauthentication: FirebaseReauthentication,
    auth: FirebaseAuthServiceInterface,
    preferences: UserPreferencesRepository,
    private val appLockSession: com.FreeRave.remindme.security.lock.AppLockSession
) : ViewModel() {
    val controller = MfaSettingsController(gateway, viewModelScope)
    val state = controller.state
    val hiddenByAppLock get() = !appLockSession.controller.state.value.canReveal
    init {
        viewModelScope.launch { preferences.clearLegacyTwoFactorDemo() }
        viewModelScope.launch { auth.currentUserIdFlow().distinctUntilChanged().collect { controller.accountChanged() } }
    }
    fun reauthenticate(activity: ComponentActivity, provider: MfaProvider, password: String) {
        controller.reauthenticate { reauthentication.verify(activity, provider, password) }
    }
    override fun onCleared() { controller.clearSensitiveState() }
}
