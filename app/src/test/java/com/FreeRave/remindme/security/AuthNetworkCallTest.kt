package com.FreeRave.remindme.security

import kotlinx.coroutines.*
import com.FreeRave.remindme.security.mfa.MfaFailure
import com.FreeRave.remindme.security.mfa.MfaError
import org.junit.Assert.*
import org.junit.Test

class AuthNetworkCallTest {
    @Test fun aStalledRequestBecomesRetryableAndCanBeRetried() = runBlocking {
        try {
            authNetworkCall(20) { awaitCancellation() }
            fail("A stalled request must finish with a timeout")
        } catch (_: AuthNetworkTimeout) { }
        assertEquals("verified", authNetworkCall { "verified" })
    }

    @Test fun cancellationByTheCallerIsNotConvertedIntoANetworkError() = runBlocking {
        val cancellation = CancellationException("caller cancelled")
        try {
            authNetworkCall { throw cancellation }
            fail("Cancellation must propagate")
        } catch (error: CancellationException) {
            assertEquals(cancellation.message, error.message)
            assertTrue(generateSequence(error as Throwable) { it.cause }.any { it === cancellation })
        }
    }

    @Test fun anOuterDeadlineStillCancelsTheCaller() = runBlocking {
        try {
            withTimeout(20) { authNetworkCall(5_000) { awaitCancellation() } }
            fail("The caller deadline must propagate")
        } catch (_: TimeoutCancellationException) { }
    }

    @Test fun serverErrorsKeepTheirMfaTypeAndDetails() = runBlocking {
        val serverError = MfaFailure(MfaError.EXPIRED)
        try {
            authNetworkCall { throw serverError }
            fail("Server errors must propagate")
        } catch (error: MfaFailure) { assertEquals(MfaError.EXPIRED, error.reason) }
    }
}
