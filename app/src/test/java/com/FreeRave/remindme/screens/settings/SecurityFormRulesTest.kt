package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.security.mfa.normalizeMfaCode as normalizeVerificationCode
import org.junit.Assert.*
import org.junit.Test

class SecurityFormRulesTest {
    @Test fun `current password is required`() {
        val result = validatePasswordChange("", "new-pass", "new-pass")
        assertTrue(result.currentMissing)
        assertFalse(result.valid)
    }

    @Test fun `a seven character password fails the existing minimum`() {
        val result = validatePasswordChange("current-pass", "1234567", "1234567")
        assertTrue(result.newTooShort)
        assertFalse(result.valid)
    }

    @Test fun `eight characters can pass when confirmation matches`() {
        assertTrue(validatePasswordChange("current-pass", "new-pass", "new-pass").valid)
    }

    @Test fun `confirmation must match exactly including spaces`() {
        val result = validatePasswordChange("current-pass", "new-pass ", "new-pass")
        assertTrue(result.confirmationMismatch)
        assertFalse(result.valid)
    }

    @Test fun `reusing the current password does not submit a change`() {
        val result = validatePasswordChange("current-pass", "current-pass", "current-pass")
        assertTrue(result.unchanged)
        assertFalse(result.valid)
    }

    @Test fun `whitespace alone is not an acceptable new password`() {
        assertFalse(validatePasswordChange("current-pass", "        ", "        ").valid)
    }

    @Test fun `passwords with intentional surrounding spaces are preserved`() {
        val result = validatePasswordChange("old-pass", " old-pass ", " old-pass ")
        assertFalse(result.unchanged)
        assertTrue(result.valid)
    }

    @Test fun `pasted verification codes accept common separators`() {
        assertEquals("123456", normalizeVerificationCode("12 3-456"))
    }

    @Test fun `verification input normalizes Unicode digits and accepts at most six`() {
        assertEquals("123456", normalizeVerificationCode("123456789"))
        assertEquals("123456", normalizeVerificationCode("abc١٢٣４５６"))
        assertEquals("", normalizeVerificationCode(""))
    }
}
