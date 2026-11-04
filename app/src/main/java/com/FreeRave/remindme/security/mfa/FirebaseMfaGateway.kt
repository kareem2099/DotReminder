package com.FreeRave.remindme.security.mfa

import com.FreeRave.remindme.model.AuthProvider
import com.FreeRave.remindme.model.User
import com.FreeRave.remindme.security.AuthNetworkTimeout
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.coroutines.CancellationException
import com.FreeRave.remindme.security.awaitAuth
import javax.inject.Inject

internal fun FirebaseUser.toMfaUser() = User(
    id = uid, email = email.orEmpty(), name = displayName.orEmpty(), photoUrl = photoUrl?.toString(),
    isEmailVerified = isEmailVerified,
    provider = when (providerData.firstOrNull { it.providerId != "firebase" }?.providerId) {
        GoogleAuthProvider.PROVIDER_ID -> AuthProvider.GOOGLE
        FacebookAuthProvider.PROVIDER_ID -> AuthProvider.FACEBOOK
        else -> AuthProvider.EMAIL
    }
)

internal fun mfaError(error: Exception): MfaError = when (error) {
    is MfaFailure -> error.reason
    is AuthNetworkTimeout -> MfaError.NETWORK
    is FirebaseNetworkException -> MfaError.NETWORK
    is FirebaseTooManyRequestsException -> MfaError.RATE_LIMIT
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_INVALID_VERIFICATION_CODE", "ERROR_INVALID_TOTP_CODE", "ERROR_MISSING_TOTP_CODE" -> MfaError.INVALID_CODE
        "ERROR_SESSION_EXPIRED", "ERROR_INVALID_MULTI_FACTOR_SESSION", "ERROR_MISSING_MULTI_FACTOR_SESSION" -> MfaError.EXPIRED
        "ERROR_REQUIRES_RECENT_LOGIN" -> MfaError.RECENT_LOGIN
        "ERROR_UNVERIFIED_EMAIL" -> MfaError.EMAIL_UNVERIFIED
        "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_PROJECT_ID", "ERROR_UNSUPPORTED_FIRST_FACTOR" -> MfaError.NOT_CONFIGURED
        "ERROR_MAXIMUM_SECOND_FACTOR_COUNT_EXCEEDED", "ERROR_SECOND_FACTOR_ALREADY_ENROLLED" -> MfaError.LIMIT
        "ERROR_USER_MISMATCH" -> MfaError.ACCOUNT_CHANGED
        "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN", "ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND" -> MfaError.SIGN_IN_REQUIRED
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> MfaError.CREDENTIALS
        else -> MfaError.GENERIC
    }
    else -> MfaError.GENERIC
}

internal suspend fun <T> firebaseMfaCall(block: suspend () -> T): T = try { block() }
catch (e: CancellationException) { throw e }
catch (e: MfaRequired) { throw e }
catch (e: MfaCancelled) { throw e }
catch (e: Exception) { throw MfaFailure(mfaError(e)) }

internal class FirebaseMfaChallenge(
    private val resolver: MultiFactorResolver,
    private val expectedUid: String? = null,
    private val afterVerified: suspend (FirebaseUser) -> Unit = {}
) : MfaChallenge {
    override val factors = resolver.hints.filter { it.factorId == TotpMultiFactorGenerator.FACTOR_ID }
        .map { MfaFactor(it.uid, it.displayName.orEmpty().ifBlank { "Authenticator" }) }

    override suspend fun verify(factorId: String, code: String): User = firebaseMfaCall {
        if (!validMfaCode(code)) throw MfaFailure(MfaError.INVALID_CODE)
        if (factors.none { it.id == factorId }) throw MfaFailure(MfaError.UNSUPPORTED)
        val assertion = TotpMultiFactorGenerator.getAssertionForSignIn(factorId, code)
        val user = resolver.resolveSignIn(assertion).awaitAuth().user ?: throw MfaFailure(MfaError.SIGN_IN_REQUIRED)
        if (expectedUid != null && user.uid != expectedUid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
        afterVerified(user)
        user.toMfaUser()
    }
}

internal class FirebaseMfaGateway @Inject constructor(private val auth: FirebaseAuth) : MfaAccountGateway {
    override suspend fun account(): MfaAccount? = firebaseMfaCall {
        val user = auth.currentUser ?: return@firebaseMfaCall null
        user.reload().awaitAuth()
        checkUser(user.uid)
        MfaAccount(user.uid, user.email.orEmpty(), user.isEmailVerified,
            user.providerData.mapNotNull { provider -> when (provider.providerId) {
                EmailAuthProvider.PROVIDER_ID -> MfaProvider.PASSWORD
                GoogleAuthProvider.PROVIDER_ID -> MfaProvider.GOOGLE
                FacebookAuthProvider.PROVIDER_ID -> MfaProvider.FACEBOOK
                else -> null
            } }.distinct(),
            user.multiFactor.enrolledFactors.filter { it.factorId == TotpMultiFactorGenerator.FACTOR_ID }
                .map { MfaFactor(it.uid, it.displayName.orEmpty().ifBlank { "Authenticator" }) })
    }

    override suspend fun beginEnrollment(expectedUid: String): MfaEnrollment = firebaseMfaCall {
        val user = checkUser(expectedUid)
        if (!user.isEmailVerified || user.email.isNullOrBlank()) throw MfaFailure(MfaError.EMAIL_UNVERIFIED)
        if (user.multiFactor.enrolledFactors.size >= 5) throw MfaFailure(MfaError.LIMIT)
        val totpSecret = TotpMultiFactorGenerator.generateSecret(user.multiFactor.session.awaitAuth()).awaitAuth()
        checkUser(expectedUid)
        object : MfaEnrollment {
            override val secret: String = totpSecret.sharedSecretKey
            override val qrUri: String = totpSecret.generateQrCodeUrl(user.email!!, "DotReminder")
            override val expiresAtMillis: Long = totpSecret.enrollmentCompletionDeadline * 1_000
            override suspend fun enroll(code: String, name: String) = firebaseMfaCall {
                if (!validMfaCode(code)) throw MfaFailure(MfaError.INVALID_CODE)
                val active = checkUser(expectedUid)
                active.multiFactor.enroll(TotpMultiFactorGenerator.getAssertionForEnrollment(totpSecret, code), name).awaitAuth()
                checkUser(expectedUid)
                Unit
            }
            override fun toString() = "FirebaseMfaEnrollment(redacted)"
        }
    }

    override suspend fun removeFactor(expectedUid: String, factorId: String) = firebaseMfaCall {
        val user = checkUser(expectedUid)
        if (user.multiFactor.enrolledFactors.none { it.uid == factorId && it.factorId == TotpMultiFactorGenerator.FACTOR_ID })
            throw MfaFailure(MfaError.ACCOUNT_CHANGED)
        user.multiFactor.unenroll(factorId).awaitAuth()
        Unit
    }

    private fun checkUser(uid: String): FirebaseUser {
        val user = auth.currentUser ?: throw MfaFailure(MfaError.SIGN_IN_REQUIRED)
        if (user.uid != uid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
        return user
    }
}
