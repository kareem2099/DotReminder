package com.FreeRave.remindme.screens.auth

import org.junit.Assert.*
import org.junit.Test

class LoginRulesTest {
    @Test fun `sign in trims the email for validation`() {
        assertTrue(validateLogin("  user@example.com  ", "password").valid)
    }
    @Test fun `invalid email blocks submission`() {
        listOf("", "user", "user@", "user example.com").forEach { assertTrue(validateLogin(it, "password").emailInvalid) }
    }
    @Test fun `an empty password is required even with a valid email`() {
        assertTrue(validateLogin("user@example.com", "").passwordMissing)
        assertFalse(validateLogin("user@example.com", "").valid)
    }
    @Test fun `existing shorter passwords are not rejected by sign up rules`() {
        assertTrue(validateLogin("user@example.com", "123456").valid)
    }
    @Test fun `sign in does not trim or alter password characters`() {
        assertTrue(validateLogin("user@example.com", " ").valid)
        assertTrue(validateLogin("user@example.com", " pass ").valid)
    }
    @Test fun `remembered email can fill an untouched empty form`() {
        assertEquals("user@example.com", rememberedEmailForForm("", false, true, "user@example.com"))
    }
    @Test fun `a remembered email never overwrites typed input`() {
        assertEquals("typed@example.com", rememberedEmailForForm("typed@example.com", true, true, "other@example.com"))
    }
    @Test fun `clearing the email is respected after preferences load`() {
        assertEquals("", rememberedEmailForForm("", true, true, "other@example.com"))
    }
    @Test fun `disabled remember email does not fill stored data`() {
        assertEquals("", rememberedEmailForForm("", false, false, "other@example.com"))
    }
    @Test fun `biometrics cannot create a session from a remembered email alone`() {
        assertFalse(canUnlockBiometricSession(true, "user@example.com", true, true, null, null))
        assertFalse(canUnlockBiometricSession(true, "user@example.com", true, true, "", "user@example.com"))
    }
    @Test fun `biometrics require a matching authenticated email`() {
        assertFalse(canUnlockBiometricSession(true, "user@example.com", true, true, "uid", "other@example.com"))
        assertTrue(canUnlockBiometricSession(true, "USER@example.com", true, true, "uid", "user@example.com"))
    }
    @Test fun `biometric preference hardware and remembered email remain required`() {
        assertFalse(canUnlockBiometricSession(false, "user@example.com", true, true, "uid", "user@example.com"))
        assertFalse(canUnlockBiometricSession(true, "user@example.com", false, true, "uid", "user@example.com"))
        assertFalse(canUnlockBiometricSession(true, "user@example.com", true, false, "uid", "user@example.com"))
        assertFalse(canUnlockBiometricSession(true, "", true, true, "uid", "user@example.com"))
    }
}
