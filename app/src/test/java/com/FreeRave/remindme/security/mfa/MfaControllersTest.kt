package com.FreeRave.remindme.security.mfa

import com.FreeRave.remindme.model.User
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class MfaControllersTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val first = MfaFactor("factor-1", "My authenticator")
    private val account = MfaAccount("user-1", "user@example.com", true, listOf(MfaProvider.PASSWORD), emptyList())
    @After fun cleanup() { scope.cancel() }

    private class Challenge(override val factors: List<MfaFactor>, private val uid: String = "user-1") : MfaChallenge {
        var calls = 0
        var wait: CompletableDeferred<Unit>? = null
        override suspend fun verify(factorId: String, code: String): User {
            calls++
            wait?.await()
            if (code != "184392") throw MfaFailure(MfaError.INVALID_CODE)
            check(factors.any { it.id == factorId })
            return User(id = uid)
        }
    }
    private inner class Gateway : MfaAccountGateway {
        var current: MfaAccount? = account
        var starts = 0
        var enrolls = 0
        var removals = 0
        var deadline = 10_000L
        var startGate: CompletableDeferred<Unit>? = null
        var failure: MfaError? = null
        var failAfterEnroll = false
        override suspend fun account(): MfaAccount? {
            failure?.let { throw MfaFailure(it) }
            return current
        }
        override suspend fun beginEnrollment(expectedUid: String): MfaEnrollment {
            check(current?.uid == expectedUid)
            starts++
            startGate?.await()
            return object : MfaEnrollment {
                override val secret = "FAKE-SETUP-KEY"
                override val qrUri = "otpauth://totp/test?secret=FAKE"
                override val expiresAtMillis = deadline
                override suspend fun enroll(code: String, name: String) {
                    if (current?.uid != expectedUid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
                    enrolls++
                    if (code != "184392") throw MfaFailure(MfaError.INVALID_CODE)
                    current = current!!.copy(factors = current!!.factors + MfaFactor("new-factor", name))
                    if (failAfterEnroll) failure = MfaError.NETWORK
                }
            }
        }
        override suspend fun removeFactor(expectedUid: String, factorId: String) {
            if (current?.uid != expectedUid) throw MfaFailure(MfaError.ACCOUNT_CHANGED)
            removals++
            current = current!!.copy(factors = current!!.factors.filterNot { it.id == factorId })
        }
    }
    private fun settings(gateway: Gateway): MfaSettingsController = MfaSettingsController(gateway, scope) { 1_000 }.also { it.refresh() }

    @Test fun enrollmentStartsOnlyAfterReauthentication() {
        val gateway = Gateway()
        val controller = settings(gateway)
        controller.request(MfaAction.Add)
        assertEquals(MfaSettingsStep.REAUTHENTICATE, controller.state.value.step)
        assertEquals(0, gateway.starts)
        controller.reauthenticate { }
        assertEquals(MfaSettingsStep.ENROLL, controller.state.value.step)
        assertEquals(1, gateway.starts)
        assertTrue(controller.state.value.account!!.factors.isEmpty())
    }
    @Test fun aWrongPasswordNeverGeneratesASecret() {
        val gateway = Gateway()
        val controller = settings(gateway)
        controller.request(MfaAction.Add)
        controller.reauthenticate { throw MfaFailure(MfaError.CREDENTIALS) }
        assertEquals(0, gateway.starts)
        assertNull(controller.state.value.setup)
        assertEquals(MfaError.CREDENTIALS, controller.state.value.error)
    }
    @Test fun existingFactorMustBeResolvedBeforeAddingABackup() {
        val gateway = Gateway().apply { current = account.copy(factors = listOf(first)) }
        val controller = settings(gateway)
        val challenge = Challenge(listOf(first))
        controller.request(MfaAction.Add)
        controller.reauthenticate { throw MfaRequired(challenge) }
        assertEquals(MfaSettingsStep.CHALLENGE, controller.state.value.step)
        controller.verifyChallenge("123456")
        assertEquals(MfaError.INVALID_CODE, controller.state.value.challenge?.error)
        assertEquals(0, gateway.starts)
        controller.verifyChallenge("184392")
        assertEquals(MfaSettingsStep.ENROLL, controller.state.value.step)
        assertEquals(1, gateway.starts)
    }
    @Test fun sixDigitsAloneCannotEnableProtection() {
        val gateway = Gateway()
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }
        controller.enroll("123456", "Phone")
        assertEquals(1, gateway.enrolls)
        assertEquals(MfaError.INVALID_CODE, controller.state.value.error)
        assertTrue(controller.state.value.account!!.factors.isEmpty())
        controller.enroll("184392", "  Backup device  ")
        assertEquals("Backup device", controller.state.value.account!!.factors.single().name)
        assertNull(controller.state.value.setup)
        assertEquals(MfaNotice.ADDED, controller.state.value.notice)
    }
    @Test fun shortCodesDoNotReachTheServer() {
        val gateway = Gateway()
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }
        controller.enroll("12345", "Phone")
        assertEquals(0, gateway.enrolls)
        assertEquals(MfaError.INVALID_CODE, controller.state.value.error)
    }
    @Test fun anExpiredSetupCannotEnrollAndDropsItsSecret() {
        val gateway = Gateway().apply { deadline = 999 }
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }; controller.enroll("184392", "Phone")
        assertEquals(0, gateway.enrolls)
        assertEquals(MfaError.EXPIRED, controller.state.value.error)
        assertNull(controller.state.value.setup)
    }
    @Test fun cancelDropsEnrollmentWithoutEnablingIt() {
        val gateway = Gateway()
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }; controller.cancel()
        assertNull(controller.state.value.setup)
        assertEquals(MfaSettingsStep.NONE, controller.state.value.step)
        assertEquals(0, gateway.enrolls)
    }
    @Test fun aCommittedEnrollmentClearsItsSecretEvenWhenReloadFails() {
        val gateway = Gateway().apply { failAfterEnroll = true }
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }; controller.enroll("184392", "Phone")
        assertEquals(1, gateway.enrolls)
        assertNull(controller.state.value.setup)
        assertNull(controller.state.value.account)
        assertEquals(MfaSettingsStep.NONE, controller.state.value.step)
        assertEquals(MfaError.NETWORK, controller.state.value.error)
        assertEquals(MfaNotice.ADDED, controller.state.value.notice)
    }
    @Test fun unverifiedEmailCannotBeginSetup() {
        val gateway = Gateway().apply { current = account.copy(emailVerified = false) }
        val controller = settings(gateway)
        controller.request(MfaAction.Add)
        assertEquals(MfaError.EMAIL_UNVERIFIED, controller.state.value.error)
        assertEquals(MfaSettingsStep.NONE, controller.state.value.step)
    }
    @Test fun enrollmentLimitDoesNotOpenAnotherSetup() {
        val gateway = Gateway().apply { current = account.copy(factors = List(5) { MfaFactor("$it", "Device $it") }) }
        val controller = settings(gateway)
        controller.request(MfaAction.Add)
        assertEquals(MfaError.LIMIT, controller.state.value.error)
        assertEquals(0, gateway.starts)
    }
    @Test fun removingTheLastFactorRequiresItsExistingCode() {
        val gateway = Gateway().apply { current = account.copy(factors = listOf(first)) }
        val controller = settings(gateway)
        controller.request(MfaAction.Remove(first))
        controller.reauthenticate { throw MfaRequired(Challenge(listOf(first))) }
        assertEquals(0, gateway.removals)
        controller.verifyChallenge("123456")
        assertEquals(0, gateway.removals)
        controller.verifyChallenge("184392")
        assertEquals(1, gateway.removals)
        assertTrue(controller.state.value.account!!.factors.isEmpty())
        assertEquals(MfaNotice.REMOVED, controller.state.value.notice)
    }
    @Test fun anotherAccountCannotUseAPendingEnrollment() {
        val gateway = Gateway()
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }
        gateway.current = account.copy(uid = "another-user")
        controller.enroll("184392", "Phone")
        assertEquals(0, gateway.enrolls)
        assertEquals(MfaError.ACCOUNT_CHANGED, controller.state.value.error)
        assertNull(controller.state.value.setup)
    }
    @Test fun duplicateReauthenticationIsIgnoredWhilePending() {
        val gateway = Gateway()
        val controller = settings(gateway)
        val gate = CompletableDeferred<Unit>()
        var requests = 0
        controller.request(MfaAction.Add)
        controller.reauthenticate { requests++; gate.await() }
        controller.reauthenticate { requests++ }
        assertEquals(1, requests)
        gate.complete(Unit)
        assertEquals(1, gateway.starts)
    }
    @Test fun leavingThePageCannotRestoreALateSecret() {
        val gateway = Gateway().apply { startGate = CompletableDeferred() }
        val controller = settings(gateway)
        controller.request(MfaAction.Add); controller.reauthenticate { }
        controller.clearSensitiveState()
        gateway.startGate!!.complete(Unit)
        assertNull(controller.state.value.setup)
        assertEquals(MfaSettingsStep.NONE, controller.state.value.step)
        assertFalse(controller.state.value.busy)
    }
    @Test fun loadErrorsNeverInventAnEnabledAccount() {
        val gateway = Gateway().apply { failure = MfaError.NETWORK }
        val controller = settings(gateway)
        assertEquals(MfaError.NETWORK, controller.state.value.error)
        assertNull(controller.state.value.account)
        assertFalse(controller.state.value.loading)
    }
    @Test fun cancelledOrInvalidChallengesNeverCompleteLogin() {
        var signedIn = 0
        var cancelled = 0
        val controller = MfaChallengeController(scope, { signedIn++ }, { cancelled++ })
        controller.present(Challenge(listOf(first)))
        controller.verify("123456")
        assertEquals(0, signedIn)
        assertEquals(MfaError.INVALID_CODE, controller.state.value?.error)
        controller.cancel()
        assertEquals(0, signedIn)
        assertEquals(1, cancelled)
        assertNull(controller.state.value)
    }
    @Test fun serverVerificationCompletesLoginExactlyOnce() {
        var signedIn = 0
        val controller = MfaChallengeController(scope, { signedIn++ }, { })
        val challenge = Challenge(listOf(first)).apply { wait = CompletableDeferred() }
        controller.present(challenge)
        controller.verify("184392"); controller.verify("184392")
        assertEquals(0, signedIn)
        assertEquals(1, challenge.calls)
        challenge.wait!!.complete(Unit)
        assertEquals(1, signedIn)
        assertNull(controller.state.value)
    }
    @Test fun noSupportedFactorCannotBypassTheChallenge() {
        var signedIn = 0
        val controller = MfaChallengeController(scope, { signedIn++ }, { })
        controller.present(Challenge(emptyList())); controller.verify("184392")
        assertEquals(MfaError.UNSUPPORTED, controller.state.value?.error)
        assertEquals(0, signedIn)
    }
    @Test fun clearedChallengeCannotDeliverALateSuccess() {
        var signedIn = 0
        val controller = MfaChallengeController(scope, { signedIn++ }, { })
        val challenge = Challenge(listOf(first)).apply { wait = CompletableDeferred() }
        controller.present(challenge); controller.verify("184392"); controller.clear()
        challenge.wait!!.complete(Unit)
        assertEquals(0, signedIn)
        assertNull(controller.state.value)
    }
    @Test fun codesNormalizeArabicPersianAndFullWidthDigits() {
        assertEquals("123456", normalizeMfaCode("١٢ ۳۴-５６"))
        assertTrue(validMfaCode(normalizeMfaCode("١٢٣٤٥٦")))
        assertFalse(validMfaCode("12345"))
        assertFalse(validMfaCode("1234567"))
        assertFalse(validMfaCode("12a456"))
    }
    @Test fun setupDiagnosticsDoNotExposeTheSecret() {
        assertFalse(MfaSetup("PRIVATE", "PRIVATE-URI").toString().contains("PRIVATE"))
    }
}
