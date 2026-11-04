package com.FreeRave.remindme.security.mfa

import androidx.activity.ComponentActivity
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.FreeRave.remindme.service.FacebookSignInOnlyService
import com.FreeRave.remindme.service.GoogleCredentialSource
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.FreeRave.remindme.security.awaitAuth
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FirebaseReauthentication @Inject constructor(
    private val auth: FirebaseAuth,
    private val google: GoogleCredentialSource,
    private val facebook: FacebookSignInOnlyService
) {
    suspend fun verify(activity: ComponentActivity, provider: MfaProvider, password: String) = firebaseMfaCall {
        val user = auth.currentUser ?: throw MfaFailure(MfaError.SIGN_IN_REQUIRED)
        val uid = user.uid
        val credential = try {
            when (provider) {
                MfaProvider.PASSWORD -> EmailAuthProvider.getCredential(user.email ?: throw MfaFailure(MfaError.UNSUPPORTED), password)
                MfaProvider.GOOGLE -> google.credential(activity)
                MfaProvider.FACEBOOK -> suspendCancellableCoroutine<AuthCredential> { continuation ->
                    facebook.requestCredential(activity, onCredential = { if (continuation.isActive) continuation.resume(it) },
                        onFailure = { if (continuation.isActive) continuation.resumeWithException(it) })
                }
            }
        } catch (_: GetCredentialCancellationException) { throw MfaCancelled() }
        if (auth.currentUser?.uid != uid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
        try { user.reauthenticate(credential).awaitAuth() }
        catch (e: FirebaseAuthMultiFactorException) { throw MfaRequired(FirebaseMfaChallenge(e.resolver, uid)) }
        if (auth.currentUser?.uid != uid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
    }
}
