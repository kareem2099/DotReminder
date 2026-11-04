package com.FreeRave.remindme.ui.reminder

internal fun normalizeTimeDigits(value: String) = buildString {
    value.trim().forEach { character ->
        val digit = Character.digit(character, 10)
        append(if (digit >= 0) ('0'.code + digit).toChar() else character)
    }
}
internal fun selectedTimeHour(value: String, is24Hour: Boolean, afternoon: Boolean): Int? {
    val hour = normalizeTimeDigits(value).toIntOrNull() ?: return null
    return if (is24Hour) hour.takeIf { it in 0..23 }
    else hour.takeIf { it in 1..12 }?.let { it % 12 + if (afternoon) 12 else 0 }
}
internal fun selectedTimeMinute(value: String): Int? = normalizeTimeDigits(value).toIntOrNull()?.takeIf { it in 0..59 }
