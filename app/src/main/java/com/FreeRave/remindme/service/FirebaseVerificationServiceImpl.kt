package com.FreeRave.remindme.service

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseVerificationServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : VerificationService {

    override suspend fun sendOTP(
        recipient: String,
        type: VerificationService.VerificationType,
        activity: Activity?
    ): Flow<VerificationService.OTPSendResult> = callbackFlow {
        if (type != VerificationService.VerificationType.PHONE) {
            trySend(VerificationService.OTPSendResult(
                success = false,
                message = "Only phone number verification is supported"
            ))
            close()
            return@callbackFlow
        }
        if (activity == null) {
            trySend(VerificationService.OTPSendResult(
                success = false,
                message = "Activity is required for phone verification"
            ))
            close()
            return@callbackFlow
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(recipient)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto verification - sign in immediately
                    firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            trySend(VerificationService.OTPSendResult(
                                success = true,
                                message = "Phone verified automatically",
                                otpId = null
                            ))
                        } else {
                            trySend(VerificationService.OTPSendResult(
                                success = false,
                                message = "Auto verification failed: ${task.exception?.localizedMessage}"
                            ))
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    trySend(VerificationService.OTPSendResult(
                        success = false,
                        message = "Verification failed: ${e.localizedMessage}"
                    ))
                    close(e)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    trySend(VerificationService.OTPSendResult(
                        success = true,
                        message = "OTP sent to $recipient",
                        otpId = verificationId
                    ))
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        awaitClose()
    }

    override suspend fun verifyOTP(
        otpId: String?,
        code: String
    ): VerificationService.OTPVerifyResult {
        return try {
            if (otpId == null) {
                // Auto verification case - check if user is signed in
                if (firebaseAuth.currentUser != null) {
                    VerificationService.OTPVerifyResult(true, "Phone verified automatically")
                } else {
                    VerificationService.OTPVerifyResult(false, "Verification not completed")
                }
            } else {
                val credential = PhoneAuthProvider.getCredential(otpId, code)
                firebaseAuth.signInWithCredential(credential).await()
                VerificationService.OTPVerifyResult(true, "OTP verified successfully")
            }
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException ->
                    VerificationService.OTPVerifyResult(false, "Invalid OTP code")
                else ->
                    VerificationService.OTPVerifyResult(false, "Verification failed: ${e.localizedMessage}")
            }
        }
    }
}