package com.FreeRave.remindme.screens.auth

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SignOutControllerTest {
    private class Gateway : SignOutGateway {
        var signedIn = true
        var endCalls = 0
        var cleanupCalls = 0
        var endFailure: Exception? = null
        var cleanupFailure: Exception? = null
        var endGate: CompletableDeferred<Unit>? = null
        var cleanupGate: CompletableDeferred<Unit>? = null
        var leaveSessionOpen = false
        var restoreSessionDuringCleanup = false
        val calls = mutableListOf<String>()
        override suspend fun endSession() {
            endCalls++
            calls += "firebase"
            endGate?.await()
            endFailure?.let { throw it }
            if (!leaveSessionOpen) signedIn = false
        }
        override fun hasSession() = signedIn
        override suspend fun clearProviderSessions() {
            cleanupCalls++
            calls += "providers"
            cleanupGate?.await()
            cleanupFailure?.let { throw it }
            if (restoreSessionDuringCleanup) signedIn = true
        }
    }
    @Test fun openingConfirmationDoesNotSignOut() {
        val gateway = Gateway()
        val controller = SignOutController(gateway)
        assertEquals(SignOutStatus.READY, controller.state.value.status)
        assertEquals(0, gateway.endCalls)
        assertEquals(0, gateway.cleanupCalls)
    }
    @Test fun localSessionEndsBeforeCredentialProvidersAreCleared() = runBlocking {
        val gateway = Gateway()
        val controller = SignOutController(gateway)
        controller.signOut()
        assertEquals(listOf("firebase", "providers"), gateway.calls)
        assertFalse(gateway.signedIn)
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
    }
    @Test fun duplicateClicksCannotStartAnotherSignOut() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val gateway = Gateway().apply { endGate = gate }
        val controller = SignOutController(gateway)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.signOut() }
        assertTrue(controller.state.value.busy)
        controller.signOut()
        assertEquals(1, gateway.endCalls)
        gate.complete(Unit); job.join()
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
    }
    @Test fun credentialProviderFailureDoesNotKeepTheAppSignedIn() = runBlocking {
        val gateway = Gateway().apply { cleanupFailure = Exception() }
        val controller = SignOutController(gateway)
        controller.signOut()
        assertFalse(gateway.signedIn)
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
    }
    @Test fun localSignOutFailureCannotReportSuccess() = runBlocking {
        val gateway = Gateway().apply { endFailure = Exception() }
        val controller = SignOutController(gateway)
        controller.signOut()
        assertEquals(SignOutStatus.ERROR, controller.state.value.status)
        assertTrue(gateway.signedIn)
        assertEquals(0, gateway.cleanupCalls)
    }
    @Test fun failedRequestCanBeRetried() = runBlocking {
        val gateway = Gateway().apply { endFailure = Exception() }
        val controller = SignOutController(gateway)
        controller.signOut()
        gateway.endFailure = null
        controller.signOut()
        assertEquals(2, gateway.endCalls)
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
    }
    @Test fun returningWithoutEndingTheSessionCannotReportSuccess() = runBlocking {
        val gateway = Gateway().apply { leaveSessionOpen = true }
        val controller = SignOutController(gateway)
        controller.signOut()
        assertEquals(SignOutStatus.ERROR, controller.state.value.status)
        assertEquals(0, gateway.cleanupCalls)
    }
    @Test fun aNewSessionDuringCleanupPreventsSuccessNavigation() = runBlocking {
        val gateway = Gateway().apply { restoreSessionDuringCleanup = true }
        val controller = SignOutController(gateway)
        controller.signOut()
        assertEquals(SignOutStatus.ERROR, controller.state.value.status)
    }
    @Test fun stalledProviderCleanupDoesNotLeaveThePageLoadingForever() = runBlocking {
        val gateway = Gateway().apply { cleanupGate = CompletableDeferred() }
        val controller = SignOutController(gateway, cleanupTimeoutMillis = 10)
        controller.signOut()
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
        assertFalse(gateway.signedIn)
    }
    @Test fun cancellationBeforeEndingSessionAllowsRetry() = runBlocking {
        val gateway = Gateway().apply { endGate = CompletableDeferred() }
        val controller = SignOutController(gateway)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.signOut() }
        job.cancelAndJoin()
        assertEquals(SignOutStatus.READY, controller.state.value.status)
        assertTrue(gateway.signedIn)
    }
    @Test fun cancellationAfterEndingSessionDoesNotPretendItIsStillSignedIn() = runBlocking {
        val gateway = Gateway().apply { cleanupGate = CompletableDeferred() }
        val controller = SignOutController(gateway)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.signOut() }
        job.cancelAndJoin()
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
        assertFalse(gateway.signedIn)
    }
    @Test fun completedSignOutCannotBeSubmittedAgain() = runBlocking {
        val gateway = Gateway()
        val controller = SignOutController(gateway)
        controller.signOut(); controller.signOut()
        assertEquals(1, gateway.endCalls)
        assertEquals(1, gateway.cleanupCalls)
    }
    @Test fun alreadySignedOutSessionIsHandledSafely() = runBlocking {
        val gateway = Gateway().apply { signedIn = false }
        val controller = SignOutController(gateway)
        controller.signOut()
        assertEquals(SignOutStatus.COMPLETE, controller.state.value.status)
    }
}
