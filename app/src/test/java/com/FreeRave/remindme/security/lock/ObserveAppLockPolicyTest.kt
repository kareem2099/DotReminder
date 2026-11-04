package com.FreeRave.remindme.security.lock

import com.FreeRave.remindme.data.BiometricLockPreference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ObserveAppLockPolicyTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    @After fun close() { scope.cancel() }

    @Test fun aPreferenceFailureDoesNotLoseTheNextAccountOrItsProtection() {
        val userIds = MutableStateFlow("first")
        var reads = 0
        val preferences = flow {
            reads++
            if (reads == 1) throw IOException("storage unavailable")
            emit(BiometricLockPreference(true, "second"))
        }
        val lock = AppLockController()
        scope.launch { observeAppLockPolicy(userIds, preferences, lock) }
        assertEquals(AppLockError.PREFERENCES, lock.state.value.error)
        userIds.value = ""
        assertTrue(lock.state.value.canReveal)
        userIds.value = "second"
        assertEquals(2, reads)
        assertEquals("second", lock.state.value.accountId)
        assertFalse(lock.state.value.canReveal)
        assertTrue(lock.state.value.required)
        lock.accept(lock.begin("second")!!)
        assertTrue(lock.state.value.canReveal)
        lock.stop(); lock.start()
        assertFalse(lock.state.value.canReveal)
    }

    @Test fun aPendingPolicyCannotExposeAnIncomingAccountEvenAfterRealSignIn() {
        val lock = AppLockController()
        val prefs = MutableSharedFlow<BiometricLockPreference>(extraBufferCapacity = 1)
        val ids = MutableStateFlow("")
        scope.launch { observeAppLockPolicy(ids, prefs, lock) }
        lock.verifiedSignIn("incoming")
        ids.value = "incoming"
        assertEquals(AppLockPhase.CHECKING, lock.state.value.phase)
        assertFalse(lock.state.value.canReveal)
        prefs.tryEmit(BiometricLockPreference(true, "incoming"))
        assertTrue(lock.state.value.canReveal)
        lock.stop(); lock.start()
        assertFalse(lock.state.value.canReveal)
    }

    @Test fun aBrokenPolicyAfterSignOutStillFailsClosedForANewLogin() {
        val ids = MutableStateFlow("first")
        val lock = AppLockController()
        scope.launch { observeAppLockPolicy(ids, flow { throw IOException() }, lock) }
        lock.beginSignOut(); ids.value = ""; lock.finishSignOut(true)
        assertTrue(lock.state.value.canReveal)
        ids.value = "second"
        lock.verifiedSignIn("second")
        assertFalse(lock.state.value.canReveal)
        assertEquals(AppLockError.PREFERENCES, lock.state.value.error)
        lock.stop(); lock.start()
        assertFalse(lock.state.value.canReveal)
        assertEquals(AppLockError.PREFERENCES, lock.state.value.error)
    }
}
