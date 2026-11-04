package com.FreeRave.remindme.security.lock

import com.FreeRave.remindme.data.BiometricLockPreference
import org.junit.Assert.*
import org.junit.Test

class AppLockControllerTest {
    @Test fun preferencesMustLoadBeforeAnyAccountIsRevealed() {
        assertFalse(AppLockController().state.value.canReveal)
    }
    @Test fun loggedOutAndUnprotectedAccountsCanShowTheirNormalScreens() {
        val lock = AppLockController()
        lock.policy(null, true); assertTrue(lock.state.value.canReveal)
        lock.policy("user", false); assertTrue(lock.state.value.canReveal)
    }
    @Test fun savedProtectedSessionStartsLocked() {
        val lock = AppLockController()
        lock.policy("user", true)
        assertEquals(AppLockPhase.LOCKED, lock.state.value.phase)
    }
    @Test fun onlyTheCurrentSuccessfulAttemptUnlocks() {
        val lock = AppLockController()
        lock.policy("user", true)
        val attempt = lock.begin("user")!!
        assertFalse(lock.state.value.canReveal)
        assertEquals(AppLockPhase.AUTHENTICATING, lock.state.value.phase)
        assertTrue(lock.accept(attempt))
        assertTrue(lock.state.value.canReveal)
        assertFalse(lock.accept(attempt))
    }
    @Test fun cancellationAndLockoutKeepTheAccountLocked() {
        val lock = AppLockController()
        lock.policy("user", true)
        lock.reject(lock.begin("user")!!, AppLockError.CANCELLED)
        assertFalse(lock.state.value.canReveal)
        lock.reject(lock.begin("user")!!, AppLockError.LOCKOUT)
        assertFalse(lock.state.value.canReveal)
        assertEquals(AppLockError.LOCKOUT, lock.state.value.error)
    }
    @Test fun returningFromBackgroundRequiresAnotherBiometric() {
        val lock = AppLockController()
        lock.policy("user", true); lock.accept(lock.begin("user")!!)
        lock.stop(); assertFalse(lock.state.value.canReveal)
        lock.start(); assertFalse(lock.state.value.canReveal)
        lock.accept(lock.begin("user")!!); assertTrue(lock.state.value.canReveal)
    }
    @Test fun aLateBackgroundCallbackCannotUnlock() {
        val lock = AppLockController()
        lock.policy("user", true)
        val attempt = lock.begin("user")!!
        lock.stop(); lock.start()
        assertFalse(lock.accept(attempt))
        assertFalse(lock.state.value.canReveal)
    }
    @Test fun switchingAccountsInvalidatesTheOldPrompt() {
        val lock = AppLockController()
        lock.policy("first", true)
        val attempt = lock.begin("first")!!
        lock.policy("second", true)
        assertFalse(lock.accept(attempt))
        assertNull(lock.begin("first"))
        assertFalse(lock.state.value.canReveal)
    }
    @Test fun aCancelledAttemptCannotApproveANewerOne() {
        val lock = AppLockController()
        lock.policy("user", true)
        val old = lock.begin("user")!!
        lock.reject(old, AppLockError.CANCELLED)
        val fresh = lock.begin("user")!!
        assertFalse(lock.accept(old))
        assertTrue(lock.accept(fresh))
    }
    @Test fun enablingProtectionDoesNotPromptTwiceAfterConfirmedSetup() {
        val lock = AppLockController()
        lock.policy("user", false)
        assertTrue(lock.accept(lock.begin("user")!!))
        lock.policy("user", true)
        assertTrue(lock.state.value.canReveal)
        lock.stop(); lock.start()
        assertFalse(lock.state.value.canReveal)
    }
    @Test fun preferencesFailClosedEvenAfterAnEarlierUnlock() {
        val lock = AppLockController()
        lock.policy("user", true); lock.accept(lock.begin("user")!!)
        lock.preferenceFailure()
        assertFalse(lock.state.value.canReveal)
        assertEquals(AppLockError.PREFERENCES, lock.state.value.error)
    }
    @Test fun signOutFailureCannotRevealTheSavedAccount() {
        val lock = AppLockController()
        lock.policy("user", true); lock.accept(lock.begin("user")!!)
        lock.beginSignOut(); assertEquals(AppLockPhase.SIGNING_OUT, lock.state.value.phase)
        assertFalse(lock.state.value.canReveal)
        lock.finishSignOut(false)
        assertFalse(lock.state.value.canReveal)
    }
    @Test fun confirmedSignOutShowsPublicContentWithoutRetainingAGrant() {
        val lock = AppLockController()
        lock.policy("user", true); lock.accept(lock.begin("user")!!)
        lock.beginSignOut(); lock.finishSignOut(true)
        assertTrue(lock.state.value.canReveal)
        assertNull(lock.state.value.accountId)
        lock.policy("user", true)
        assertFalse(lock.state.value.canReveal)
    }
    @Test fun aFreshCompleteServerSignInIsAValidFallback() {
        val lock = AppLockController()
        lock.policy(null, false)
        lock.verifiedSignIn("user")
        lock.policy("user", true)
        assertTrue(lock.state.value.canReveal)
        lock.stop(); lock.start()
        assertFalse(lock.state.value.canReveal)
    }
    @Test fun completedSocialSignInWhileReturningDoesNotExposeBackgroundContent() {
        val lock = AppLockController()
        lock.policy("user", true); lock.stop()
        lock.verifiedSignIn("user")
        assertFalse(lock.state.value.canReveal)
        lock.start(); assertTrue(lock.state.value.canReveal)
    }
    @Test fun noAttemptIsStartedForAnAnonymousOrDifferentSession() {
        val lock = AppLockController()
        assertNull(lock.begin("user"))
        lock.policy(null, false); assertNull(lock.begin("user"))
        lock.policy("first", true); assertNull(lock.begin("second"))
    }
    @Test fun accountPreferenceIsNotInheritedByAnotherUser() {
        val preference = BiometricLockPreference(true, "owner")
        assertTrue(preference.protects("owner"))
        assertFalse(preference.protects("someone-else"))
        assertFalse(preference.protects(null))
    }
    @Test fun legacyEnabledPreferencesKeepASavedSessionProtected() {
        assertTrue(BiometricLockPreference(true).protects("user"))
        assertFalse(BiometricLockPreference(false).protects("user"))
    }
}
