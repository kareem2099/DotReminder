package com.FreeRave.remindme.utils

import androidx.compose.ui.graphics.Color // Keep this for the return type
import com.FreeRave.remindme.ui.theme.PasswordStrengthTooShort
import com.FreeRave.remindme.ui.theme.PasswordStrengthWeak
import com.FreeRave.remindme.ui.theme.PasswordStrengthMedium
import com.FreeRave.remindme.ui.theme.PasswordStrengthStrong
import com.FreeRave.remindme.ui.theme.PasswordStrengthNone


enum class PasswordStrength {
    TOO_SHORT,
    WEAK,
    MEDIUM,
    STRONG,
    NONE // Initial state or for empty password
}

// Helper to get a color for the strength
fun PasswordStrength.getColor(): Color {
    return when (this) {
        PasswordStrength.TOO_SHORT -> PasswordStrengthTooShort
        PasswordStrength.WEAK -> PasswordStrengthWeak
        PasswordStrength.MEDIUM -> PasswordStrengthMedium
        PasswordStrength.STRONG -> PasswordStrengthStrong
        PasswordStrength.NONE -> PasswordStrengthNone
    }
}

fun PasswordStrength.displayText(): String {
    return when (this) {
        PasswordStrength.TOO_SHORT -> "Too Short"
        PasswordStrength.WEAK -> "Weak"
        PasswordStrength.MEDIUM -> "Medium"
        PasswordStrength.STRONG -> "Strong"
        PasswordStrength.NONE -> ""
    }
}


data class PasswordRequirements(
    val minLength: Boolean,
    val hasLowerCase: Boolean,
    val hasUpperCase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
)

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        return emailRegex.matches(email)
    }

    fun isValidName(name: String): Boolean {
        return name.trim().isNotEmpty()
    }

    fun isValidPassword(password: String): Boolean {
        // A password is valid if its strength is Medium or Strong
        val strength = calculatePasswordStrength(password)
        return strength == PasswordStrength.MEDIUM || strength == PasswordStrength.STRONG
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.NONE
        if (password.length < 8) return PasswordStrength.TOO_SHORT

        var score = 0
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++ // Special character

        return when (score) {
            1 -> PasswordStrength.WEAK
            2, 3 -> PasswordStrength.MEDIUM
            4 -> PasswordStrength.STRONG
            else -> PasswordStrength.WEAK // Should not happen if length >= 8
        }
    }

    fun checkPasswordRequirements(password: String): PasswordRequirements {
        return PasswordRequirements(
            minLength = password.length >= 8,
            hasLowerCase = password.any { it.isLowerCase() },
            hasUpperCase = password.any { it.isUpperCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecialChar = password.any { !it.isLetterOrDigit() }
        )
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        // Simple validation - at least 8 digits
        return phone.length >= 8 && phone.all { it.isDigit() }
    }
}
