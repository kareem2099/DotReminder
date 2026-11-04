package com.FreeRave.remindme.security.mfa

import com.FreeRave.remindme.model.User

internal enum class MfaProvider { PASSWORD, GOOGLE, FACEBOOK }
internal data class MfaFactor(val id: String, val name: String)
internal data class MfaAccount(
    val uid: String, val email: String, val emailVerified: Boolean,
    val providers: List<MfaProvider>, val factors: List<MfaFactor>
)
internal enum class MfaError {
    NETWORK, RATE_LIMIT, INVALID_CODE, EXPIRED, RECENT_LOGIN, EMAIL_UNVERIFIED,
    UNSUPPORTED, NOT_CONFIGURED, ACCOUNT_CHANGED, SIGN_IN_REQUIRED, CREDENTIALS, LIMIT, GENERIC
}
internal class MfaFailure(val reason: MfaError) : Exception()
internal class MfaCancelled : Exception()
internal class MfaRequired(val challenge: MfaChallenge) : Exception()

internal interface MfaChallenge {
    val factors: List<MfaFactor>
    suspend fun verify(factorId: String, code: String): User
}

/** Enrollment secrets exist only in memory until verified, cancelled or expired. */
internal interface MfaEnrollment {
    val secret: String
    val qrUri: String
    val expiresAtMillis: Long
    suspend fun enroll(code: String, name: String)
}

internal interface MfaAccountGateway {
    suspend fun account(): MfaAccount?
    suspend fun beginEnrollment(expectedUid: String): MfaEnrollment
    suspend fun removeFactor(expectedUid: String, factorId: String)
}

/** Converts Arabic/Persian digits too; it never treats a locally valid code as authenticated. */
internal fun normalizeMfaCode(value: String): String = buildString {
    value.forEach { char -> char.digitToIntOrNull()?.let { append(it) } }
}.take(6)

internal fun validMfaCode(value: String) = value.length == 6 && value.all { it in '0'..'9' }

internal data class MfaChallengeState(
    val factors: List<MfaFactor> = emptyList(), val selectedId: String? = null,
    val busy: Boolean = false, val error: MfaError? = null
)
