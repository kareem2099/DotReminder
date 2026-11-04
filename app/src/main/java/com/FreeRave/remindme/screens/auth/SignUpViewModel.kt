package com.FreeRave.remindme.screens.auth

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.R
import com.FreeRave.remindme.service.FirebaseSignUpGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val gateway: FirebaseSignUpGateway,
    @ApplicationContext context: Context
) : ViewModel() {
    private val controller = SignUpController(gateway, viewModelScope, { error -> context.getString(when (error) {
        RegistrationError.NETWORK -> R.string.signup_network_error
        RegistrationError.RATE_LIMIT -> R.string.signup_rate_limit
        RegistrationError.QUOTA -> R.string.signup_quota_error
        RegistrationError.APP_CONFIG -> R.string.signup_config_error
        RegistrationError.SEND -> R.string.signup_send_error
        RegistrationError.CODE -> R.string.signup_verify_error
        RegistrationError.EXPIRED -> R.string.signup_expired_error
        RegistrationError.EXISTING_PHONE -> R.string.signup_phone_used
        RegistrationError.EMAIL_USED -> R.string.signup_email_used
        RegistrationError.ACCOUNT -> R.string.signup_account_error
        RegistrationError.PROFILE -> R.string.signup_profile_error
    }) }, SystemClock::elapsedRealtime)
    internal val state = controller.state
    internal fun edit(field: SignUpField, value: String) = controller.edit(field, value)
    internal fun sendCode(activity: Activity?) = controller.sendCode({ phone, resend -> gateway.sendCode(phone, resend, activity) })
    internal fun resendCode(activity: Activity?) = controller.sendCode({ phone, resend -> gateway.sendCode(phone, resend, activity) }, resend = true)
    internal fun cancelSend() = controller.cancelSend()
    internal fun editDetails() = controller.editDetails()
    internal fun createAccount() = controller.createAccount()
    internal fun retryProfile() = controller.retryProfile()
    override fun onCleared() = controller.close()
}
