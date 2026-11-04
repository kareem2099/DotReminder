package com.FreeRave.remindme.screens.auth

data class VerifiedPhone(val uid: String, val number: String)
enum class RegistrationError { NETWORK, RATE_LIMIT, QUOTA, APP_CONFIG, SEND, CODE, EXPIRED, EXISTING_PHONE, EMAIL_USED, ACCOUNT, PROFILE }
class RegistrationFailure(val reason: RegistrationError) : Exception()
interface AutoPhoneVerification { suspend fun verify(): VerifiedPhone }
sealed interface PhoneCodeEvent {
    data class Sent(val id: String) : PhoneCodeEvent
    data class Automatic(val confirmation: AutoPhoneVerification) : PhoneCodeEvent
    data class Failed(val error: RegistrationError) : PhoneCodeEvent
}
class RegistrationDetails(val name: String, val email: String, val password: String, val phone: VerifiedPhone) {
    override fun toString() = "RegistrationDetails(redacted)"
}
interface RegistrationGateway {
    suspend fun verifyPhone(id: String, code: String, recipient: String): VerifiedPhone
    suspend fun createAccount(details: RegistrationDetails): String
    suspend fun saveProfile(uid: String, name: String, email: String, phone: String)
    fun clearPhoneSession()
}
