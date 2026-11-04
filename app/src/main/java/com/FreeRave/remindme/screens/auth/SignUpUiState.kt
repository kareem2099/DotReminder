package com.FreeRave.remindme.screens.auth

internal enum class SignUpStep { DETAILS, CODE, PROFILE, COMPLETE }
internal data class SignUpUiState(
    val name: String = "", val email: String = "", val countryCode: String = "+20",
    val phone: String = "", val password: String = "", val confirmation: String = "",
    val code: String = "", val step: SignUpStep = SignUpStep.DETAILS,
    val submitted: Boolean = false, val busy: Boolean = false,
    val error: String? = null, val recipient: String = "", val phoneVerified: Boolean = false,
    val resendSeconds: Int = 0, val sendingCode: Boolean = false
) {
    val canResend get() = step == SignUpStep.CODE && !busy && !phoneVerified && resendSeconds == 0
}
internal enum class SignUpField { NAME, EMAIL, COUNTRY_CODE, PHONE, PASSWORD, CONFIRMATION, CODE }
