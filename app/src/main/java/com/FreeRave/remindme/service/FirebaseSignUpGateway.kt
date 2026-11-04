package com.FreeRave.remindme.service

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.FreeRave.remindme.model.User
import com.FreeRave.remindme.screens.auth.*
import com.FreeRave.remindme.security.lock.AppLockSession
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** Phone verification cannot replace the app's signed-in Firebase user before registration completes. */
@Singleton
class FirebaseSignUpGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val primary: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val appLock: AppLockSession,
    private val sessionRecorder: AuthSessionRecorder
) : RegistrationGateway {
    private val phoneAuth by lazy {
        val name = "dotreminder-phone-registration"
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == name }
            ?: FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, name)
        FirebaseAuth.getInstance(app).apply { useAppLanguage() }
    }
    private val requestId = AtomicLong(0)
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var resendRecipient: String? = null
    private var registeringUid: String? = null
    private var registeringEmail: String? = null

    fun sendCode(recipient: String, forceResend: Boolean, activity: Activity?): Flow<PhoneCodeEvent> = callbackFlow {
        if (activity == null || primary.currentUser != null) {
            trySend(PhoneCodeEvent.Failed(RegistrationError.APP_CONFIG)); close(); return@callbackFlow
        }
        val id = requestId.incrementAndGet()
        val disposed = AtomicBoolean(false)
        val automaticEmitted = AtomicBoolean(false)
        val codeSent = AtomicBoolean(false)
        val startedAt = SystemClock.elapsedRealtime()
        registeringUid = null; registeringEmail = null
        phoneAuth.signOut()
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            private fun active() = !disposed.get() && requestId.get() == id
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                if (!active() || automaticEmitted.get()) return
                codeSent.set(true)
                resendToken = token; resendRecipient = recipient
                trySend(PhoneCodeEvent.Sent(verificationId))
                // Keep listening: automatic SMS retrieval can complete after onCodeSent.
            }
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (!active() || !automaticEmitted.compareAndSet(false, true)) return
                trySend(PhoneCodeEvent.Automatic(object : AutoPhoneVerification {
                    private val used = AtomicBoolean(false)
                    override suspend fun verify(): VerifiedPhone = registrationCall {
                        if (requestId.get() != id || !used.compareAndSet(false, true)) throw RegistrationFailure(RegistrationError.EXPIRED)
                        val user = withTimeout(30_000) { phoneAuth.signInWithCredential(credential).await().user }
                            ?: throw RegistrationFailure(RegistrationError.CODE)
                        if (requestId.get() != id || user.phoneNumber != recipient) throw RegistrationFailure(RegistrationError.CODE)
                        VerifiedPhone(user.uid, recipient)
                    }
                }))
            }
            override fun onVerificationFailed(error: com.google.firebase.FirebaseException) {
                if (!active()) return
                trySend(PhoneCodeEvent.Failed(registrationError(error)))
                close()
            }
            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                if (!active()) return
                // The manually entered SMS code can still be verified; auto retrieval timing out isn't code expiry.
                close()
            }
        }
        fun request(resend: Boolean) {
            val options = PhoneAuthOptions.newBuilder(phoneAuth).setPhoneNumber(recipient)
                .setActivity(activity).setTimeout(60, TimeUnit.SECONDS).setCallbacks(callbacks)
            if (resend && resendRecipient == recipient) resendToken?.let(options::setForceResendingToken)
            PhoneAuthProvider.verifyPhoneNumber(options.build())
        }
        try {
            request(forceResend)
        } catch (error: Exception) { trySend(PhoneCodeEvent.Failed(registrationError(error))); close() }
        // Firebase detaches Activity-bound callbacks onStop. Reattach an unfinished request on return;
        // never force a resend here and never restart it after its active retrieval window.
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        var stopped = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stopped = true
            if (event == Lifecycle.Event.ON_START && stopped && !disposed.get() && requestId.get() == id &&
                !codeSent.get() && !automaticEmitted.get() && SystemClock.elapsedRealtime() - startedAt < 60_000) {
                stopped = false
                try { request(false) }
                catch (error: Exception) { trySend(PhoneCodeEvent.Failed(registrationError(error))); close() }
            }
        }
        lifecycle?.addObserver(observer)
        awaitClose { disposed.set(true); lifecycle?.removeObserver(observer) }
    }

    override suspend fun verifyPhone(id: String, code: String, recipient: String): VerifiedPhone = registrationCall {
        val user = withTimeout(30_000) { phoneAuth.signInWithCredential(PhoneAuthProvider.getCredential(id, code)).await().user }
            ?: throw RegistrationFailure(RegistrationError.CODE)
        if (user.phoneNumber != recipient) throw RegistrationFailure(RegistrationError.CODE)
        VerifiedPhone(user.uid, recipient)
    }

    override suspend fun createAccount(details: RegistrationDetails): String = registrationCall {
        val initial = phoneAuth.currentUser ?: throw RegistrationFailure(RegistrationError.EXPIRED)
        initial.reload().await()
        val user = phoneAuth.currentUser ?: throw RegistrationFailure(RegistrationError.EXPIRED)
        if (user.uid != details.phone.uid || user.phoneNumber != details.phone.number) throw RegistrationFailure(RegistrationError.CODE)
        if (primary.currentUser != null && primary.currentUser?.uid != user.uid) throw RegistrationFailure(RegistrationError.ACCOUNT)
        if (user.email.isNullOrBlank()) {
            if (user.providerData.any { it.providerId !in listOf("firebase", PhoneAuthProvider.PROVIDER_ID) })
                throw RegistrationFailure(RegistrationError.EXISTING_PHONE)
            registeringUid = user.uid; registeringEmail = details.email
            user.linkWithCredential(EmailAuthProvider.getCredential(details.email, details.password)).await()
        } else if (registeringUid != user.uid || !registeringEmail.equals(details.email, ignoreCase = true) ||
            !user.email.equals(details.email, ignoreCase = true)) throw RegistrationFailure(RegistrationError.EXISTING_PHONE)
        try { user.updateProfile(userProfileChangeRequest { displayName = details.name }).await() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* The profile save below also stores the chosen display name. */ }
        if (primary.currentUser != null && primary.currentUser?.uid != user.uid) throw RegistrationFailure(RegistrationError.ACCOUNT)
        val signedIn = primary.signInWithEmailAndPassword(details.email, details.password).await().user
            ?: throw RegistrationFailure(RegistrationError.ACCOUNT)
        if (signedIn.uid != user.uid) throw RegistrationFailure(RegistrationError.ACCOUNT)
        appLock.controller.verifiedSignIn(signedIn.uid)
        sessionRecorder.record(signedIn.uid, null)
        signedIn.uid
    }
    override suspend fun saveProfile(uid: String, name: String, email: String, phone: String) = registrationCall {
        val user = primary.currentUser ?: throw RegistrationFailure(RegistrationError.PROFILE)
        if (user.uid != uid || !user.email.equals(email, ignoreCase = true) || user.phoneNumber != phone)
            throw RegistrationFailure(RegistrationError.PROFILE)
        val profile = User(id = uid, name = name, email = email, phone = phone, isEmailVerified = user.isEmailVerified)
        firestore.collection("users").document(uid).set(profile.toMap(), SetOptions.merge()).await()
        Unit
    }
    override fun clearPhoneSession() {
        requestId.incrementAndGet(); registeringUid = null; registeringEmail = null; phoneAuth.signOut()
    }
}

private fun registrationError(error: Exception): RegistrationError = when (error) {
    is RegistrationFailure -> error.reason
    is FirebaseNetworkException -> RegistrationError.NETWORK
    is FirebaseTooManyRequestsException -> RegistrationError.RATE_LIMIT
    is FirebaseAuthMultiFactorException -> RegistrationError.EXISTING_PHONE
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> RegistrationError.EMAIL_USED
        "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_PROVIDER_ALREADY_LINKED" -> RegistrationError.EXISTING_PHONE
        "ERROR_SESSION_EXPIRED", "ERROR_INVALID_VERIFICATION_ID" -> RegistrationError.EXPIRED
        "ERROR_QUOTA_EXCEEDED" -> RegistrationError.QUOTA
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_APP_CREDENTIAL", "ERROR_MISSING_CLIENT_IDENTIFIER", "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_CERT_HASH",
        "ERROR_MISSING_ACTIVITY", "ERROR_CAPTCHA_CHECK_FAILED", "ERROR_INVALID_RECAPTCHA_TOKEN", "ERROR_APP_NOT_VERIFIED" -> RegistrationError.APP_CONFIG
        "ERROR_INVALID_PHONE_NUMBER" -> RegistrationError.SEND
        "ERROR_TOO_MANY_REQUESTS" -> RegistrationError.RATE_LIMIT
        "ERROR_INVALID_VERIFICATION_CODE" -> RegistrationError.CODE
        else -> RegistrationError.ACCOUNT
    }
    else -> RegistrationError.SEND
}
private suspend fun <T> registrationCall(block: suspend () -> T): T = try { block() }
catch (e: CancellationException) { throw e }
catch (error: Exception) { throw RegistrationFailure(registrationError(error)) }
