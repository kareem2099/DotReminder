package com.FreeRave.remindme.screens.auth

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EmailVerificationControllerTest {
    private val account = VerificationAccount("user-1", "user@example.com", false)
    private class Gateway(var account: VerificationAccount?) : EmailVerificationGateway {
        var reads = 0
        var sends = 0
        var syncs = 0
        var readFailure: Exception? = null
        var sendFailure: Exception? = null
        var syncFailure: Exception? = null
        var sendGate: CompletableDeferred<Unit>? = null
        override suspend fun readAccount(reload: Boolean): VerificationAccount? {
            reads++
            readFailure?.let { throw it }
            return account
        }
        override suspend fun sendLink(account: VerificationAccount) {
            sends++
            sendGate?.await()
            sendFailure?.let { throw it }
        }
        override suspend fun syncVerifiedAccount(account: VerificationAccount) {
            check(account.verified)
            syncs++
            syncFailure?.let { throw it }
        }
    }
    @Test fun openingOnlyChecksAndDoesNotSendALink() = runBlocking {
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        assertEquals(EmailVerificationPhase.READY, controller.state.value.phase)
        assertNull(controller.state.value.notice)
        assertEquals(0, gateway.sends)
        assertEquals(0, gateway.syncs)
    }
    @Test fun explicitSendStartsSixtySecondCooldownAfterSuccess() = runBlocking {
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway) { 1_000 }
        controller.refresh(); controller.sendLink()
        assertEquals(1, gateway.sends)
        assertEquals(EmailVerificationNotice.SENT, controller.state.value.notice)
        assertTrue(controller.state.value.hasSentLink)
        assertEquals(60, controller.state.value.resendSeconds)
        assertFalse(controller.state.value.canSend)
        assertTrue(controller.state.value.canCheck)
    }
    @Test fun cooldownBlocksDuplicateSends() = runBlocking {
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway) { 0 }
        controller.refresh(); controller.sendLink(); controller.sendLink()
        assertEquals(1, gateway.sends)
    }
    @Test fun cooldownCountsDownAndAllowsResendingWhenItExpires() = runBlocking {
        var now = 0L
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway) { now }
        controller.refresh(); controller.sendLink()
        now = 59_001; controller.tick()
        assertEquals(1, controller.state.value.resendSeconds)
        now = 60_000; controller.tick()
        assertTrue(controller.state.value.canSend)
        controller.sendLink()
        assertEquals(2, gateway.sends)
    }
    @Test fun failedSendDoesNotClaimSuccessOrStartCooldown() = runBlocking {
        val gateway = Gateway(account).apply { sendFailure = Exception() }
        val controller = EmailVerificationController(gateway)
        controller.refresh(); controller.sendLink()
        assertEquals(EmailVerificationError.SEND, controller.state.value.error)
        assertNull(controller.state.value.notice)
        assertFalse(controller.state.value.hasSentLink)
        assertEquals(0, controller.state.value.resendSeconds)
        assertTrue(controller.state.value.canSend)
    }
    @Test fun pendingSendBlocksOtherRequestsAndWaitsBeforeSuccess() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val gateway = Gateway(account).apply { sendGate = gate }
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { controller.sendLink() }
        assertTrue(controller.state.value.busy)
        assertNull(controller.state.value.notice)
        val readsBefore = gateway.reads
        controller.sendLink(); controller.refresh()
        assertEquals(1, gateway.sends)
        assertEquals(readsBefore, gateway.reads)
        gate.complete(Unit); pending.join()
        assertEquals(EmailVerificationNotice.SENT, controller.state.value.notice)
        assertFalse(controller.state.value.busy)
    }
    @Test fun unconfirmedCheckShowsPendingWithoutClaimingALinkWasSent() = runBlocking {
        val controller = EmailVerificationController(Gateway(account))
        controller.refresh(showPending = true)
        assertEquals(EmailVerificationPhase.READY, controller.state.value.phase)
        assertEquals(EmailVerificationNotice.PENDING, controller.state.value.notice)
        assertFalse(controller.state.value.hasSentLink)
    }
    @Test fun verifiedFirebaseStatusIsSyncedWithoutSendingAnotherEmail() = runBlocking {
        val gateway = Gateway(account.copy(verified = true))
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        assertEquals(EmailVerificationPhase.VERIFIED, controller.state.value.phase)
        assertEquals(1, gateway.syncs)
        controller.sendLink()
        assertEquals(0, gateway.sends)
    }
    @Test fun profileSyncFailureKeepsRealVerificationAndCanRetryIndependently() = runBlocking {
        val gateway = Gateway(account.copy(verified = true)).apply { syncFailure = Exception() }
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        assertEquals(EmailVerificationPhase.VERIFIED, controller.state.value.phase)
        assertEquals(EmailVerificationError.SYNC, controller.state.value.error)
        gateway.syncFailure = null
        controller.retrySync()
        assertNull(controller.state.value.error)
        assertEquals(2, gateway.syncs)
        assertEquals(0, gateway.sends)
    }
    @Test fun repeatedResumeChecksDoNotRepeatASuccessfulProfileSync() = runBlocking {
        val gateway = Gateway(account.copy(verified = true))
        val controller = EmailVerificationController(gateway)
        controller.refresh(); controller.refresh()
        assertEquals(1, gateway.syncs)
    }
    @Test fun signedOutStateCannotSendOrSync() = runBlocking {
        val gateway = Gateway(null)
        val controller = EmailVerificationController(gateway)
        controller.refresh(); controller.sendLink(); controller.retrySync()
        assertEquals(EmailVerificationPhase.SIGNED_OUT, controller.state.value.phase)
        assertEquals(0, gateway.sends)
        assertEquals(0, gateway.syncs)
    }
    @Test fun phoneOnlyAccountsDoNotGetAnEmailSendAction() = runBlocking {
        val gateway = Gateway(account.copy(email = ""))
        val controller = EmailVerificationController(gateway)
        controller.refresh(); controller.sendLink()
        assertEquals(EmailVerificationPhase.NO_EMAIL, controller.state.value.phase)
        assertFalse(controller.state.value.canSend)
        assertEquals(0, gateway.sends)
    }
    @Test fun changedUserCannotReceiveThePreviousUsersSendRequest() = runBlocking {
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        gateway.account = account.copy(id = "user-2", email = "other@example.com")
        controller.sendLink()
        assertEquals(EmailVerificationError.ACCOUNT_CHANGED, controller.state.value.error)
        assertEquals("other@example.com", controller.state.value.account?.email)
        assertEquals(0, gateway.sends)
    }
    @Test fun changedEmailOnTheSameUserAlsoRequiresAnotherExplicitAction() = runBlocking {
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        gateway.account = account.copy(email = "new@example.com")
        controller.sendLink()
        assertEquals(EmailVerificationError.ACCOUNT_CHANGED, controller.state.value.error)
        assertEquals(0, gateway.sends)
    }
    @Test fun initialLoadFailureOffersRetryAndNeverLooksSignedOut() = runBlocking {
        val gateway = Gateway(account).apply { readFailure = EmailVerificationException(EmailVerificationError.NETWORK) }
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        assertEquals(EmailVerificationPhase.LOAD_ERROR, controller.state.value.phase)
        assertEquals(EmailVerificationError.NETWORK, controller.state.value.error)
        gateway.readFailure = null
        controller.refresh()
        assertEquals(EmailVerificationPhase.READY, controller.state.value.phase)
        assertNull(controller.state.value.error)
    }
    @Test fun cancellationClearsBusyWithoutShowingSendSuccess() = runBlocking {
        val gateway = Gateway(account).apply { sendGate = CompletableDeferred() }
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.sendLink() }
        job.cancelAndJoin()
        assertFalse(controller.state.value.busy)
        assertNull(controller.state.value.notice)
        assertFalse(controller.state.value.hasSentLink)
    }
    @Test fun accountSwitchResetsThePreviousAccountsCooldownAndSentNotice() = runBlocking {
        val gateway = Gateway(account)
        val controller = EmailVerificationController(gateway) { 0 }
        controller.refresh(); controller.sendLink()
        gateway.account = account.copy(id = "user-2")
        controller.refresh()
        assertEquals(0, controller.state.value.resendSeconds)
        assertNull(controller.state.value.notice)
        assertFalse(controller.state.value.hasSentLink)
    }
    @Test fun accountChangedDuringReloadDoesNotKeepAnOldVerifiedScreen() = runBlocking {
        val gateway = Gateway(account.copy(verified = true))
        val controller = EmailVerificationController(gateway)
        controller.refresh()
        gateway.readFailure = EmailVerificationException(EmailVerificationError.ACCOUNT_CHANGED)
        controller.refresh()
        assertEquals(EmailVerificationPhase.LOAD_ERROR, controller.state.value.phase)
        assertNull(controller.state.value.account)
    }
    @Test fun serverRateLimitIsShownWithoutFalseSendSuccess() = runBlocking {
        val gateway = Gateway(account).apply { sendFailure = EmailVerificationException(EmailVerificationError.RATE_LIMIT) }
        val controller = EmailVerificationController(gateway)
        controller.refresh(); controller.sendLink()
        assertEquals(EmailVerificationError.RATE_LIMIT, controller.state.value.error)
        assertNull(controller.state.value.notice)
    }
    @Test fun resendClockRoundsUpAndNeverBecomesNegative() {
        assertEquals(60, verificationResendSeconds(60_000, 0))
        assertEquals(60, verificationResendSeconds(60_000, 1))
        assertEquals(1, verificationResendSeconds(60_000, 59_999))
        assertEquals(0, verificationResendSeconds(60_000, 60_000))
        assertEquals(0, verificationResendSeconds(60_000, 70_000))
    }
}
