package com.FreeRave.remindme.screens.auth

import com.FreeRave.remindme.utils.ValidationUtils

internal data class LoginValidation(val emailInvalid: Boolean, val passwordMissing: Boolean) {
    val valid get() = !emailInvalid && !passwordMissing
}

/** Sign-in accepts existing credentials; password creation rules belong to sign-up. */
internal fun validateLogin(email: String, password: String) = LoginValidation(
    emailInvalid = !ValidationUtils.isValidEmail(email.trim()), passwordMissing = password.isEmpty())

internal fun rememberedEmailForForm(current: String, edited: Boolean, remember: Boolean, stored: String?): String =
    if (!edited && current.isEmpty() && remember) stored.orEmpty() else current

internal fun canUnlockBiometricSession(
    remember: Boolean, rememberedEmail: String?, biometricEnabled: Boolean, available: Boolean,
    sessionId: String?, sessionEmail: String?
): Boolean = remember && biometricEnabled && available && !sessionId.isNullOrBlank() &&
    !rememberedEmail.isNullOrBlank() && !sessionEmail.isNullOrBlank() && rememberedEmail.equals(sessionEmail, ignoreCase = true)
