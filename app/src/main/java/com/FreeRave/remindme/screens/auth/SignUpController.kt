package com.FreeRave.remindme.screens.auth

import com.FreeRave.remindme.security.mfa.normalizeMfaCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SignUpController(
    private val gateway: RegistrationGateway, private val scope: CoroutineScope,
    private val message: (RegistrationError) -> String,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private val mutableState = MutableStateFlow(SignUpUiState())
    val state = mutableState.asStateFlow()
    private var listener: Job? = null
    private var operation: Job? = null
    private var timer: Job? = null
    private var generation = 0L
    private var verificationId: String? = null
    private var verified: VerifiedPhone? = null
    private var createdUid: String? = null
    private var lastRecipient: String? = null
    private var retryAt = 0L

    fun edit(field: SignUpField, value: String) {
        val current = state.value
        if (current.busy || current.step !in listOf(SignUpStep.DETAILS, SignUpStep.CODE)) return
        if (current.step == SignUpStep.CODE && field != SignUpField.CODE) return
        mutableState.value = when (field) {
            SignUpField.NAME -> current.copy(name = value)
            SignUpField.EMAIL -> current.copy(email = value)
            SignUpField.COUNTRY_CODE -> current.copy(countryCode = value)
            SignUpField.PHONE -> current.copy(phone = value)
            SignUpField.PASSWORD -> current.copy(password = value)
            SignUpField.CONFIRMATION -> current.copy(confirmation = value)
            SignUpField.CODE -> current.copy(code = normalizeMfaCode(value))
        }.copy(error = null)
    }

    fun sendCode(deliver: (String, Boolean) -> Flow<PhoneCodeEvent>, resend: Boolean = false) {
        tick()
        val current = state.value
        if (current.busy || state.value.resendSeconds > 0) return
        if (resend && !current.canResend || !resend && current.step != SignUpStep.DETAILS) return
        if (!resend) {
            mutableState.value = state.value.copy(submitted = true, error = null)
            if (!validateSignUp(current).valid) return
        }
        val recipient = if (resend) current.recipient else signUpPhone(current.countryCode, current.phone) ?: return
        listener?.cancel()
        val token = ++generation
        verified = null
        val forceResend = recipient == lastRecipient
        lastRecipient = recipient
        cooldown()
        mutableState.value = state.value.copy(busy = true, sendingCode = true, recipient = recipient, error = null)
        listener = scope.launch {
            var sent = false
            try {
                withTimeout(90_000) {
                    deliver(recipient, forceResend).collect { event ->
                        currentCoroutineContext().ensureActive()
                        if (token != generation || verified != null) return@collect
                        when (event) {
                            is PhoneCodeEvent.Sent -> {
                                if (event.id.isBlank()) throw RegistrationFailure(RegistrationError.SEND)
                                sent = true
                                verificationId = event.id
                                cooldown()
                                mutableState.value = state.value.copy(step = SignUpStep.CODE, busy = false, sendingCode = false,
                                    code = "", phoneVerified = false, submitted = false, error = null)
                            }
                            is PhoneCodeEvent.Automatic -> {
                                mutableState.value = state.value.copy(busy = true, sendingCode = false)
                                val phone = event.confirmation.verify()
                                currentCoroutineContext().ensureActive()
                                check(phone.number == recipient && phone.uid.isNotBlank())
                                verified = phone
                                mutableState.value = state.value.copy(step = SignUpStep.CODE, busy = false,
                                    phoneVerified = true, code = "", submitted = false, error = null)
                            }
                            is PhoneCodeEvent.Failed -> { generation++; fail(event.error) }
                        }
                    }
                }
                if (token == generation && state.value.busy) fail(RegistrationError.SEND)
            } catch (_: TimeoutCancellationException) {
                if (token == generation && state.value.busy && verified == null)
                    fail(if (sent) RegistrationError.NETWORK else RegistrationError.SEND)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (token == generation) fail((e as? RegistrationFailure)?.reason ?: RegistrationError.SEND) }
        }
    }

    fun cancelSend() {
        if (!state.value.sendingCode) return
        generation++; listener?.cancel()
        mutableState.value = state.value.copy(busy = false, sendingCode = false, error = null)
    }

    fun editDetails() {
        val current = state.value
        if (current.busy || current.step != SignUpStep.CODE) return
        generation++; listener?.cancel(); verificationId = null; verified = null
        gateway.clearPhoneSession()
        mutableState.value = current.copy(step = SignUpStep.DETAILS, code = "", phoneVerified = false, submitted = false, error = null)
    }

    fun createAccount() {
        val current = state.value
        if (current.busy || current.step != SignUpStep.CODE) return
        mutableState.value = current.copy(submitted = true, error = null)
        if (verified == null && !validSignUpCode(current.code)) return
        generation++; listener?.cancel()
        mutableState.value = state.value.copy(busy = true)
        operation = scope.launch {
            try {
                val phone = verified ?: gateway.verifyPhone(verificationId ?: throw RegistrationFailure(RegistrationError.EXPIRED),
                    current.code, current.recipient)
                currentCoroutineContext().ensureActive()
                if (phone.number != current.recipient || phone.uid.isBlank()) throw RegistrationFailure(RegistrationError.CODE)
                verified = phone
                mutableState.value = state.value.copy(phoneVerified = true, code = "")
                val uid = withTimeout(45_000) { gateway.createAccount(RegistrationDetails(current.name.trim(), current.email.trim(), current.password, phone)) }
                currentCoroutineContext().ensureActive()
                check(uid == phone.uid)
                createdUid = uid
                mutableState.value = state.value.copy(step = SignUpStep.PROFILE, password = "", confirmation = "", code = "")
                saveProfile()
            } catch (_: TimeoutCancellationException) { fail(RegistrationError.NETWORK) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { currentCoroutineContext().ensureActive(); fail((e as? RegistrationFailure)?.reason ?: RegistrationError.ACCOUNT) }
        }
    }
    fun retryProfile() {
        if (state.value.busy || state.value.step != SignUpStep.PROFILE) return
        mutableState.value = state.value.copy(busy = true, error = null)
        operation = scope.launch { saveProfile() }
    }
    private suspend fun saveProfile() {
        try {
            val current = state.value
            withTimeout(30_000) { gateway.saveProfile(createdUid ?: throw RegistrationFailure(RegistrationError.PROFILE), current.name.trim(), current.email.trim(), current.recipient) }
            currentCoroutineContext().ensureActive()
            gateway.clearPhoneSession()
            mutableState.value = state.value.copy(step = SignUpStep.COMPLETE, busy = false, error = null)
        } catch (_: TimeoutCancellationException) { fail(RegistrationError.PROFILE) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { currentCoroutineContext().ensureActive(); fail(RegistrationError.PROFILE) }
    }
    fun tick() {
        val remaining = ((retryAt - now()).coerceAtLeast(0) + 999) / 1_000
        mutableState.value = state.value.copy(resendSeconds = remaining.toInt())
    }
    private fun cooldown() {
        retryAt = now() + 60_000
        tick()
        timer?.cancel()
        timer = scope.launch { while (isActive && state.value.resendSeconds > 0) { delay(1_000); tick() } }
    }
    private fun fail(error: RegistrationError) { mutableState.value = state.value.copy(busy = false, sendingCode = false, error = message(error)) }
    fun close() {
        generation++; listener?.cancel(); operation?.cancel(); timer?.cancel()
        verificationId = null; verified = null
        gateway.clearPhoneSession()
        mutableState.value = SignUpUiState()
    }
}
