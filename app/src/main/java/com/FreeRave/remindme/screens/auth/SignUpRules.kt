package com.FreeRave.remindme.screens.auth

import com.FreeRave.remindme.utils.ValidationUtils
import com.google.i18n.phonenumbers.PhoneNumberUtil

internal data class SignUpValidation(
    val nameInvalid: Boolean, val emailInvalid: Boolean, val phoneInvalid: Boolean,
    val passwordInvalid: Boolean, val confirmationInvalid: Boolean
) { val valid get() = !nameInvalid && !emailInvalid && !phoneInvalid && !passwordInvalid && !confirmationInvalid }

internal fun signUpPhone(countryCode: String, phone: String): String? {
    if (!Regex("\\+[1-9][0-9]{0,2}").matches(countryCode.trim()) || phone.isBlank()) return null
    return try {
        val util = PhoneNumberUtil.getInstance()
        val input = phone.trim()
        // An international number must agree with the separately selected calling code.
        val parsed = util.parse(if (input.startsWith('+')) input else countryCode.trim() + input, null)
        if (!util.isValidNumber(parsed) || "+${parsed.countryCode}" != countryCode.trim()) null
        else util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
    } catch (_: Exception) { null }
}

internal fun validateSignUp(state: SignUpUiState) = SignUpValidation(
    !ValidationUtils.isValidName(state.name), !ValidationUtils.isValidEmail(state.email.trim()),
    signUpPhone(state.countryCode, state.phone) == null, !ValidationUtils.isValidPassword(state.password),
    state.confirmation.isEmpty() || state.confirmation != state.password
)
internal fun validSignUpCode(code: String) = Regex("[0-9]{6}").matches(code)
