package com.FreeRave.remindme.screens.settings

internal data class PasswordChangeValidation(
    val currentMissing: Boolean,
    val newTooShort: Boolean,
    val unchanged: Boolean,
    val confirmationMismatch: Boolean
) {
    val valid get() = !currentMissing && !newTooShort && !unchanged && !confirmationMismatch
}

/** Passwords are compared as entered; leading and trailing spaces are never trimmed. */
internal fun validatePasswordChange(current: String, new: String, confirmation: String) = PasswordChangeValidation(
    currentMissing = current.isBlank(),
    newTooShort = new.length < 8 || new.isBlank(),
    unchanged = current.isNotEmpty() && current == new,
    confirmationMismatch = new != confirmation
)
