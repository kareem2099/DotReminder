package com.FreeRave.remindme.reminders

import com.FreeRave.remindme.model.Reminder

internal fun Reminder.upcomingTime(now: Long): Long = if (isRecurring)
    nextReminderOccurrence(reminderTime, recurrenceIntervalMillis, recurrenceEndTime, now) ?: reminderTime
    else reminderTime

/** Return the first occurrence strictly after [after], preserving the original repetition anchor. */
internal fun nextReminderOccurrence(start: Long, interval: Long?, end: Long?, after: Long): Long? {
    if (start <= 0 || end != null && start > end) return null
    if (start > after) return start
    if (interval == null || interval <= 0) return null
    return try {
        val steps = Math.addExact(Math.subtractExact(after, start) / interval, 1)
        Math.addExact(start, Math.multiplyExact(steps, interval)).takeIf { end == null || it <= end }
    } catch (_: ArithmeticException) { null }
}

/** A valid new reminder can become due while its server write is completing. */
internal fun initialReminderTrigger(start: Long, interval: Long?, end: Long?, now: Long, allowDueNow: Boolean): Long? {
    if (allowDueNow && start > 0 && start <= now && (end == null || now <= end)) {
        return try { Math.addExact(now, 500) } catch (_: ArithmeticException) { null }
    }
    return nextReminderOccurrence(start, interval, end, now)
}

internal fun reminderChannelId(sound: String, vibration: Boolean): String {
    val name = sound.takeIf { it.matches(Regex("[a-z][a-z0-9_]{0,60}")) } ?: "default"
    return "reminder_alarm_v2_${name}_${if (vibration) "vibrate" else "quiet"}"
}
