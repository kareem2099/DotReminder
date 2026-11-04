package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.security.mfa.MfaSettingsState

internal data class AccountSecurityUiState(
    val biometricEnabled: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNSUPPORTED,
    val biometricBusy: Boolean = false,
    val passwordBusy: Boolean = false,
    val mfa: MfaSettingsState = MfaSettingsState()
)
