package com.FreeRave.remindme.service

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AuthSessionRecorderTest {
    @Test fun aPreferenceFailureDoesNotCrashOrWriteAnUnidentifiableRemoteSession() = runBlocking {
        var remoteCalls = 0
        persistAuthSession("user", { "user" }, {},
            { throw IllegalStateException("storage unavailable") }, { remoteCalls++ })
        assertEquals(0, remoteCalls)
    }

    @Test fun rememberingEmailIsOptionalAndTheSameSessionIdIsSavedLocallyAndRemotely() = runBlocking {
        var localId: String? = null
        var remoteId: String? = null
        persistAuthSession("user", { "user" }, { throw IllegalStateException() },
            { localId = it }, { remoteId = it })
        assertFalse(localId.isNullOrBlank())
        assertEquals(localId, remoteId)
    }

    @Test fun switchingAccountsDuringTheLocalWriteDoesNotRecordTheOldSessionForTheNewUser() = runBlocking {
        var uid = "first"
        var remoteCalls = 0
        persistAuthSession("first", { uid }, {}, { uid = "second" }, { remoteCalls++ })
        assertEquals(0, remoteCalls)
    }

    @Test fun signingOutBeforePersistenceStartsSkipsAllWrites() = runBlocking {
        var calls = 0
        persistAuthSession("user", { null }, { calls++ }, { calls++ }, { calls++ })
        assertEquals(0, calls)
    }

    @Test fun remoteFailureDoesNotUndoSuccessfulAuthenticationMetadata() = runBlocking {
        var localId: String? = null
        persistAuthSession("user", { "user" }, {}, { localId = it }, { throw IllegalStateException() })
        assertNotNull(localId)
    }

    @Test fun cancellationStopsPersistenceBeforeTheRemoteWrite() = runBlocking {
        var remoteCalls = 0
        val cancellation = CancellationException("screen independent work cancelled")
        try {
            persistAuthSession("user", { "user" }, {}, { throw cancellation }, { remoteCalls++ })
            fail("Cancellation must propagate")
        } catch (error: CancellationException) {
            assertEquals(cancellation.message, error.message)
            assertTrue(generateSequence(error as Throwable) { it.cause }.any { it === cancellation })
        }
        assertEquals(0, remoteCalls)
    }
}
