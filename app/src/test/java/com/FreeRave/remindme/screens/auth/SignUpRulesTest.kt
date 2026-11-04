package com.FreeRave.remindme.screens.auth

import org.junit.Assert.*
import org.junit.Test

class SignUpRulesTest {
    private val valid = SignUpUiState(name = " Kareem ", email = " user@example.com ",
        phone = "01012345678", password = "Password1", confirmation = "Password1")
    @Test fun acceptsValidDetailsAndNormalizesEgyptianLocalNumber() {
        assertTrue(validateSignUp(valid).valid)
        assertEquals("+201012345678", signUpPhone("+20", "01012345678"))
    }
    @Test fun acceptsInternationalPhoneWithMatchingCallingCode() {
        assertEquals("+201012345678", signUpPhone("+20", "+20 101 234 5678"))
    }
    @Test fun rejectsInternationalNumberFromDifferentCallingCode() {
        assertNull(signUpPhone("+44", "+201012345678"))
    }
    @Test fun supportsOtherCallingCodes() {
        assertEquals("+447911123456", signUpPhone("+44", "07911123456"))
    }
    @Test fun rejectsBlankName() { assertTrue(validateSignUp(valid.copy(name = "   ")).nameInvalid) }
    @Test fun rejectsMalformedEmail() { assertTrue(validateSignUp(valid.copy(email = "user@")).emailInvalid) }
    @Test fun rejectsInvalidOrMissingPhone() {
        assertTrue(validateSignUp(valid.copy(phone = "123")).phoneInvalid)
        assertNull(signUpPhone("+20", ""))
    }
    @Test fun requiresExplicitValidCallingCode() {
        listOf("", "+", "20", "+000", "+1234").forEach { assertNull(signUpPhone(it, "01012345678")) }
    }
    @Test fun rejectsShortOrSingleTypePasswords() {
        assertTrue(validateSignUp(valid.copy(password = "Short1")).passwordInvalid)
        assertTrue(validateSignUp(valid.copy(password = "password")).passwordInvalid)
    }
    @Test fun requiresMatchingConfirmation() {
        assertTrue(validateSignUp(valid.copy(confirmation = "")).confirmationInvalid)
        assertTrue(validateSignUp(valid.copy(confirmation = "password1")).confirmationInvalid)
    }
    @Test fun doesNotTrimPasswords() {
        assertFalse(validateSignUp(valid.copy(password = " Password1 ", confirmation = " Password1 ")).confirmationInvalid)
        assertTrue(validateSignUp(valid.copy(password = " Password1 ")).confirmationInvalid)
    }
    @Test fun requiresExactlySixAsciiCodeDigits() {
        assertTrue(validSignUpCode("012345"))
        listOf("", "12345", "1234567", "12a456", "123 56", "١٢٣٤٥٦").forEach { assertFalse(validSignUpCode(it)) }
    }
}
