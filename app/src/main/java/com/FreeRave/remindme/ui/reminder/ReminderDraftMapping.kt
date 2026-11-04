package com.FreeRave.remindme.ui.reminder

import com.FreeRave.remindme.model.Reminder

internal fun Reminder.toDraft() = ReminderDraft(
    title = title, description = description, category = category, reminderTimeMillis = reminderTime,
    isRecurring = isRecurring, recurrenceIntervalMillis = recurrenceIntervalMillis, recurrenceEndTime = recurrenceEndTime,
    isLocationBased = isLocationBased, latitude = latitude, longitude = longitude, geofenceRadius = geofenceRadius,
    geofenceTransitionType = geofenceTransitionType, geofenceDwellTimeMillis = geofenceDwellTimeMillis,
    geofencePriority = geofencePriority
)

/** Copy onto the original record so identity and account ownership never come from form input. */
internal fun ReminderDraft.applyTo(original: Reminder, updatedAt: Long): Reminder = original.copy(
    title = title.trim(), description = description, category = category,
    reminderTime = requireNotNull(reminderTimeMillis), time = updatedAt,
    isRecurring = isRecurring, recurrenceIntervalMillis = recurrenceIntervalMillis, recurrenceEndTime = recurrenceEndTime,
    isLocationBased = isLocationBased, latitude = latitude, longitude = longitude, geofenceRadius = geofenceRadius,
    geofenceTransitionType = geofenceTransitionType, geofenceDwellTimeMillis = geofenceDwellTimeMillis,
    geofencePriority = geofencePriority
)

internal fun ReminderDraft.update(event: ReminderFormEvent): ReminderDraft = when (event) {
    is ReminderFormEvent.Title -> copy(title = event.value)
    is ReminderFormEvent.Description -> copy(description = event.value)
    is ReminderFormEvent.Category -> copy(category = event.value)
    is ReminderFormEvent.Prompt -> copy(prompt = event.value)
    is ReminderFormEvent.Time -> copy(reminderTimeMillis = event.value)
    is ReminderFormEvent.Interval -> copy(recurrenceIntervalMillis = event.value)
    is ReminderFormEvent.EndTime -> copy(recurrenceEndTime = event.value)
    is ReminderFormEvent.Location -> copy(latitude = event.latitude, longitude = event.longitude, geofenceRadius = event.radius)
    is ReminderFormEvent.Transition -> copy(geofenceTransitionType = event.value)
    is ReminderFormEvent.DwellTime -> copy(geofenceDwellTimeMillis = event.value)
    is ReminderFormEvent.Priority -> copy(geofencePriority = event.value)
    is ReminderFormEvent.Recurring -> copy(
        isRecurring = event.enabled,
        recurrenceIntervalMillis = if (event.enabled) recurrenceIntervalMillis ?: DAY_MILLIS else null,
        recurrenceEndTime = if (event.enabled) recurrenceEndTime else null
    )
    is ReminderFormEvent.LocationEnabled -> if (event.enabled) {
        copy(isLocationBased = true, geofenceTransitionType = geofenceTransitionType.takeIf { it in 1..3 } ?: 1)
    } else {
        copy(isLocationBased = false, latitude = null, longitude = null, geofenceRadius = 200f,
            geofenceTransitionType = 0, geofenceDwellTimeMillis = null, geofencePriority = 1)
    }
    else -> this
}
