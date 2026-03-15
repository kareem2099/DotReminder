package com.FreeRave.remindme.service

import kotlinx.coroutines.flow.Flow

interface VerificationService {
    enum class VerificationType {
        EMAIL, PHONE
    }

    data class OTPSendResult(
        val success: Boolean,
        val message: String = "",
        val otpId: String? = null // For tracking the OTP session
    )

    data class OTPVerifyResult(
        val success: Boolean,
        val message: String = ""
    )

    suspend fun sendOTP(
        recipient: String, // Email or phone number
        type: VerificationType,
        activity: android.app.Activity? = null // Required for phone verification reCAPTCHA
    ): Flow<OTPSendResult>

    suspend fun verifyOTP(
        otpId: String?,
        code: String
    ): OTPVerifyResult
}
