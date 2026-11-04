package com.FreeRave.remindme.screens.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.FreeRave.remindme.ui.reminder.ReminderDraft

@Composable
internal fun AddReminderViewModel.collectReminderDraft(): ReminderDraft {
    val prompt by nlpInputText.collectAsStateWithLifecycle()
    val title by this.title.collectAsStateWithLifecycle()
    val description by this.description.collectAsStateWithLifecycle()
    val category by this.category.collectAsStateWithLifecycle()
    val time by reminderTimeMillis.collectAsStateWithLifecycle()
    val recurring by isRecurring.collectAsStateWithLifecycle()
    val interval by recurrenceIntervalMillis.collectAsStateWithLifecycle()
    val endTime by recurrenceEndTime.collectAsStateWithLifecycle()
    val locationBased by isLocationBased.collectAsStateWithLifecycle()
    val latitude by this.latitude.collectAsStateWithLifecycle()
    val longitude by this.longitude.collectAsStateWithLifecycle()
    val radius by geofenceRadius.collectAsStateWithLifecycle()
    val transition by geofenceTransitionType.collectAsStateWithLifecycle()
    val dwellTime by geofenceDwellTimeMillis.collectAsStateWithLifecycle()
    val priority by geofencePriority.collectAsStateWithLifecycle()
    return ReminderDraft(
        prompt = prompt, title = title, description = description, category = category,
        reminderTimeMillis = time, isRecurring = recurring, recurrenceIntervalMillis = interval,
        recurrenceEndTime = endTime, isLocationBased = locationBased, latitude = latitude, longitude = longitude,
        geofenceRadius = radius, geofenceTransitionType = transition, geofenceDwellTimeMillis = dwellTime,
        geofencePriority = priority
    )
}
