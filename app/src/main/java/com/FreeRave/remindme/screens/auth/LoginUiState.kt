package com.FreeRave.remindme.screens.auth

internal enum class LoginMethod { EMAIL, GOOGLE, FACEBOOK, BIOMETRIC }
internal data class LoginUiState(
    val email: String = "", val password: String = "", val rememberEmail: Boolean = false,
    val emailError: Boolean = false, val passwordError: Boolean = false,
    val busy: Boolean = false, val method: LoginMethod? = null, val error: String? = null,
    val showBiometrics: Boolean = false, val biometricReady: Boolean = false
)

internal sealed interface PasswordResetState {
    data object Idle : PasswordResetState
    data object Loading : PasswordResetState
    data class Sent(val email: String) : PasswordResetState
    data class Error(val message: String) : PasswordResetState
}
