package com.FreeRave.remindme.screens.auth

internal data class VerificationAccount(val id: String, val email: String, val verified: Boolean)
internal enum class EmailVerificationPhase { INITIAL, READY, VERIFIED, SIGNED_OUT, NO_EMAIL, LOAD_ERROR }
internal enum class EmailVerificationOperation { CHECK, SEND, SYNC }
internal enum class EmailVerificationNotice { SENT, PENDING }
internal enum class EmailVerificationError { NETWORK, RATE_LIMIT, CHECK, SEND, SYNC, ACCOUNT_CHANGED }
internal data class EmailVerificationUiState(
    val phase: EmailVerificationPhase = EmailVerificationPhase.INITIAL,
    val account: VerificationAccount? = null,
    val operation: EmailVerificationOperation? = null,
    val notice: EmailVerificationNotice? = null,
    val error: EmailVerificationError? = null,
    val resendSeconds: Int = 0,
    val hasSentLink: Boolean = false
) {
    val busy get() = operation != null || phase == EmailVerificationPhase.INITIAL
    val canSend get() = phase == EmailVerificationPhase.READY && !busy && resendSeconds == 0
    val canCheck get() = phase == EmailVerificationPhase.READY && !busy
}
