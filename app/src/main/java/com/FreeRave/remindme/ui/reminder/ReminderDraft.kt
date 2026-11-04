package com.FreeRave.remindme.ui.reminder

internal data class ReminderDraft(
    val prompt: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val reminderTimeMillis: Long? = null,
    val isRecurring: Boolean = false,
    val recurrenceIntervalMillis: Long? = null,
    val recurrenceEndTime: Long? = null,
    val isLocationBased: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceRadius: Float = 200f,
    val geofenceTransitionType: Int = 0,
    val geofenceDwellTimeMillis: Long? = null,
    val geofencePriority: Int = 1
)

internal enum class ReminderFormError {
    TITLE, TIME, INTERVAL, END_TIME, LOCATION, TRIGGER
}

internal fun ReminderDraft.validate(now: Long, originalTimeMillis: Long? = null): Set<ReminderFormError> = buildSet {
    if (title.isBlank()) add(ReminderFormError.TITLE)
    if (reminderTimeMillis == null || (reminderTimeMillis <= now && reminderTimeMillis != originalTimeMillis)) {
        add(ReminderFormError.TIME)
    }
    if (isRecurring) {
        if (recurrenceIntervalMillis == null || recurrenceIntervalMillis <= 0) add(ReminderFormError.INTERVAL)
        if (recurrenceEndTime != null && reminderTimeMillis != null && recurrenceEndTime <= reminderTimeMillis) {
            add(ReminderFormError.END_TIME)
        }
    }
    if (isLocationBased) {
        if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            add(ReminderFormError.LOCATION)
        }
        if (geofenceTransitionType !in 1..3) add(ReminderFormError.TRIGGER)
    }
}

internal const val HOUR_MILLIS = 3_600_000L
internal const val DAY_MILLIS = 24 * HOUR_MILLIS
internal const val WEEK_MILLIS = 7 * DAY_MILLIS
