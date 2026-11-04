package com.FreeRave.remindme.screens.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SignUpControllerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private var time = 0L
    private val phone = VerifiedPhone("phone-user", "+201012345678")
    @After fun closeScope() { scope.cancel() }
    private inner class Gateway : RegistrationGateway {
        var verifies = 0; var creates = 0; var profiles = 0; var clears = 0
        var lastId: String? = null
        var createFailure: RegistrationError? = null
        var profileFailure = false
        var createGate: CompletableDeferred<Unit>? = null
        var returnedPhone = phone
        override suspend fun verifyPhone(id: String, code: String, recipient: String): VerifiedPhone {
            verifies++; lastId = id
            if (code != "184392") throw RegistrationFailure(RegistrationError.CODE)
            return returnedPhone
        }
        override suspend fun createAccount(details: RegistrationDetails): String {
            creates++; createGate?.await(); createFailure?.let { throw RegistrationFailure(it) }
            assertEquals(phone, details.phone)
            return phone.uid
        }
        override suspend fun saveProfile(uid: String, name: String, email: String, phone: String) {
            profiles++; assertEquals("phone-user", uid)
            if (profileFailure) throw RegistrationFailure(RegistrationError.PROFILE)
        }
        override fun clearPhoneSession() { clears++ }
    }
    private fun controller(gateway: Gateway) = SignUpController(gateway, scope, { it.name }) { time }.also {
        it.edit(SignUpField.NAME, "Kareem")
        it.edit(SignUpField.EMAIL, "user@example.com")
        it.edit(SignUpField.PHONE, "01012345678")
        it.edit(SignUpField.PASSWORD, "Strong-pass1")
        it.edit(SignUpField.CONFIRMATION, "Strong-pass1")
    }
    private fun code(controller: SignUpController) { controller.edit(SignUpField.CODE, "184392") }

    @Test fun invalidDetailsCannotSendAnSms() {
        val controller = controller(Gateway())
        controller.edit(SignUpField.PHONE, "123")
        var sends = 0
        controller.sendCode({ _, _ -> sends++; emptyFlow() })
        assertEquals(0, sends)
    }
    @Test fun sentCodeIsNotTreatedAsAVerifiedPhoneOrAccount() {
        val gateway = Gateway(); val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session-1")) })
        assertEquals(SignUpStep.CODE, controller.state.value.step)
        assertFalse(controller.state.value.phoneVerified)
        assertEquals(0, gateway.creates)
        assertEquals(60, controller.state.value.resendSeconds)
    }
    @Test fun automaticVerificationCanArriveAfterCodeSentAndStillRequiresCreate() {
        val gateway = Gateway(); val controller = controller(gateway)
        var autoCalls = 0
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session"), PhoneCodeEvent.Automatic(object : AutoPhoneVerification {
            override suspend fun verify(): VerifiedPhone { autoCalls++; return phone }
        })) })
        assertEquals(1, autoCalls)
        assertTrue(controller.state.value.phoneVerified)
        assertEquals(0, gateway.creates)
        controller.createAccount()
        assertEquals(0, gateway.verifies)
        assertEquals(1, gateway.creates)
        assertEquals(SignUpStep.COMPLETE, controller.state.value.step)
    }
    @Test fun resendingHasACooldownAndUsesANewSessionOnlyWhenSent() {
        val gateway = Gateway(); val controller = controller(gateway)
        val flags = mutableListOf<Boolean>()
        val send: (String, Boolean) -> Flow<PhoneCodeEvent> = { _, resend -> flags += resend; flowOf(PhoneCodeEvent.Sent("session-${flags.size}")) }
        controller.sendCode(send); controller.sendCode(send, true)
        assertEquals(listOf(false), flags)
        time = 60_001; controller.tick(); controller.sendCode(send, true)
        assertEquals(listOf(false, true), flags)
        code(controller); controller.createAccount()
        assertEquals("session-2", gateway.lastId)
    }
    @Test fun failedResendDoesNotDiscardThePreviouslySentCode() {
        val gateway = Gateway(); val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("previous")) })
        time = 60_000; controller.tick()
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Failed(RegistrationError.NETWORK)) }, true)
        assertFalse(controller.state.value.busy)
        code(controller); controller.createAccount()
        assertEquals("previous", gateway.lastId)
        assertEquals(SignUpStep.COMPLETE, controller.state.value.step)
    }
    @Test fun editingDetailsCannotBypassTheSendCooldown() {
        val controller = controller(Gateway()); var sends = 0
        val send: (String, Boolean) -> Flow<PhoneCodeEvent> = { _, _ -> sends++; flowOf(PhoneCodeEvent.Sent("session")) }
        controller.sendCode(send); controller.editDetails(); controller.sendCode(send)
        assertEquals(1, sends)
        assertEquals(60, controller.state.value.resendSeconds)
    }
    @Test fun invalidSixDigitsNeverCreateAnAccount() {
        val gateway = Gateway(); val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session")) })
        controller.edit(SignUpField.CODE, "123456"); controller.createAccount()
        assertEquals(RegistrationError.CODE.name, controller.state.value.error)
        assertEquals(0, gateway.creates)
        assertFalse(controller.state.value.phoneVerified)
    }
    @Test fun ArabicCodeDigitsAreNormalizedBeforeServerVerification() {
        val gateway = Gateway(); val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session")) })
        controller.edit(SignUpField.CODE, "١٨٤٣٩٢"); controller.createAccount()
        assertEquals(SignUpStep.COMPLETE, controller.state.value.step)
    }
    @Test fun verificationForADifferentPhoneCannotCreateAnAccount() {
        val gateway = Gateway().apply { returnedPhone = phone.copy(number = "+201099999999") }
        val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session")) }); code(controller); controller.createAccount()
        assertEquals(0, gateway.creates)
        assertEquals(RegistrationError.CODE.name, controller.state.value.error)
    }
    @Test fun accountCreationErrorsDoNotLoseAnAlreadyVerifiedPhone() {
        val gateway = Gateway().apply { createFailure = RegistrationError.EMAIL_USED }
        val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session")) }); code(controller); controller.createAccount()
        assertTrue(controller.state.value.phoneVerified)
        assertEquals(0, gateway.profiles)
        gateway.createFailure = null; controller.createAccount()
        assertEquals(1, gateway.verifies)
        assertEquals(SignUpStep.COMPLETE, controller.state.value.step)
    }
    @Test fun retryingProfileDoesNotCreateAnotherAccountAndPasswordsAreCleared() {
        val gateway = Gateway().apply { profileFailure = true }
        val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session")) }); code(controller); controller.createAccount()
        assertEquals(SignUpStep.PROFILE, controller.state.value.step)
        assertEquals("", controller.state.value.password)
        assertEquals("", controller.state.value.confirmation)
        gateway.profileFailure = false; controller.retryProfile()
        assertEquals(1, gateway.creates)
        assertEquals(2, gateway.profiles)
        assertEquals(SignUpStep.COMPLETE, controller.state.value.step)
    }
    @Test fun duplicateCreationIsIgnoredWhileTheServerIsPending() {
        val gateway = Gateway().apply { createGate = CompletableDeferred() }
        val controller = controller(gateway)
        controller.sendCode({ _, _ -> flowOf(PhoneCodeEvent.Sent("session")) }); code(controller)
        controller.createAccount(); controller.createAccount()
        assertEquals(1, gateway.creates)
        gateway.createGate!!.complete(Unit)
        assertEquals(1, gateway.profiles)
    }
    @Test fun aCancelledSendCannotApplyALateAutomaticVerification() {
        val gateway = Gateway(); val controller = controller(gateway)
        val events = MutableSharedFlow<PhoneCodeEvent>(extraBufferCapacity = 2)
        controller.sendCode({ _, _ -> events }); assertTrue(controller.state.value.sendingCode)
        controller.cancelSend()
        var autoCalls = 0
        events.tryEmit(PhoneCodeEvent.Automatic(object : AutoPhoneVerification {
            override suspend fun verify(): VerifiedPhone { autoCalls++; return phone }
        }))
        assertEquals(0, autoCalls)
        assertFalse(controller.state.value.busy)
        assertFalse(controller.state.value.phoneVerified)
    }
    @Test fun closingTheFormClearsItsCredentialsAndPhoneSession() {
        val gateway = Gateway(); val controller = controller(gateway)
        controller.close()
        assertEquals("", controller.state.value.password)
        assertEquals("", controller.state.value.confirmation)
        assertEquals(1, gateway.clears)
    }
    @Test fun registrationDiagnosticsDoNotExposePasswords() {
        assertFalse(RegistrationDetails("Name", "user@example.com", "PRIVATE-PASSWORD", phone).toString().contains("PRIVATE-PASSWORD"))
    }
}
