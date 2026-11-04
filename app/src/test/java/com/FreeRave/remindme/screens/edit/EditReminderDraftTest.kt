package com.FreeRave.remindme.screens.edit

import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.ui.reminder.DAY_MILLIS
import com.FreeRave.remindme.ui.reminder.HOUR_MILLIS
import com.FreeRave.remindme.ui.reminder.ReminderFormError
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent
import com.FreeRave.remindme.ui.reminder.applyTo
import com.FreeRave.remindme.ui.reminder.toDraft
import com.FreeRave.remindme.ui.reminder.update
import com.FreeRave.remindme.ui.reminder.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditReminderDraftTest {
    private val now = 1_800_000_000_000L
    private val original = Reminder(
        id = "reminder-123", userId = "owner-456", customId = "my-reference",
        title = "Call home", description = "Ask about the appointment", category = "Personal", time = now - DAY_MILLIS,
        reminderTime = now + HOUR_MILLIS, isRecurring = true, recurrenceIntervalMillis = DAY_MILLIS,
        recurrenceEndTime = now + 7 * DAY_MILLIS, isLocationBased = true, latitude = 30.0, longitude = 31.0,
        geofenceRadius = 350f, geofenceTransitionType = 3, geofenceDwellTimeMillis = 60_000L, geofencePriority = 5
    )

    @Test
    fun loadingAndSavingWithoutEditingPreservesEveryReminderField() {
        assertEquals(original.copy(time = now), original.toDraft().applyTo(original, now))
    }

    @Test
    fun changingTheTimeDoesNotResetUnsavedTextEdits() {
        val draft = original.toDraft()
            .update(ReminderFormEvent.Title("Call Dad"))
            .update(ReminderFormEvent.Description("Bring the appointment notes"))
            .update(ReminderFormEvent.Time(now + 2 * HOUR_MILLIS))
        assertEquals("Call Dad", draft.title)
        assertEquals("Bring the appointment notes", draft.description)
        assertEquals(now + 2 * HOUR_MILLIS, draft.reminderTimeMillis)
    }

    @Test
    fun savingEditsKeepsTheExistingIdOwnerAndReference() {
        val updated = original.toDraft().update(ReminderFormEvent.Title("New title")).applyTo(original, now)
        assertEquals(original.id, updated.id)
        assertEquals(original.userId, updated.userId)
        assertEquals(original.customId, updated.customId)
        assertEquals("New title", updated.title)
    }

    @Test
    fun editingTheTitlePreservesLocationAndRepeatSettings() {
        val updated = original.toDraft().update(ReminderFormEvent.Title("New title")).applyTo(original, now)
        assertEquals(original.copy(title = "New title", time = now), updated)
        assertEquals(60_000L, updated.geofenceDwellTimeMillis)
        assertEquals(5, updated.geofencePriority)
    }

    @Test
    fun unchangedOverdueTimeDoesNotBlockTextEdits() {
        val past = original.copy(reminderTime = now - HOUR_MILLIS)
        val draft = past.toDraft().update(ReminderFormEvent.Description("Updated notes"))
        assertTrue(draft.validate(now, past.reminderTime).isEmpty())
    }

    @Test
    fun movingToADifferentPastTimeIsRejected() {
        val past = original.copy(reminderTime = now - HOUR_MILLIS)
        val draft = past.toDraft().update(ReminderFormEvent.Time(now - 2 * HOUR_MILLIS))
        assertEquals(setOf(ReminderFormError.TIME), draft.validate(now, past.reminderTime))
    }

    @Test
    fun anOverdueReminderCanBeRescheduledIntoTheFuture() {
        val past = original.copy(reminderTime = now - HOUR_MILLIS)
        val draft = past.toDraft().update(ReminderFormEvent.Time(now + HOUR_MILLIS))
        assertTrue(draft.validate(now, past.reminderTime).isEmpty())
    }

    @Test
    fun turningRepeatOffClearsItsIntervalAndEndDate() {
        val updated = original.toDraft().update(ReminderFormEvent.Recurring(false)).applyTo(original, now)
        assertFalse(updated.isRecurring)
        assertNull(updated.recurrenceIntervalMillis)
        assertNull(updated.recurrenceEndTime)
        assertEquals(original.latitude, updated.latitude)
    }

    @Test
    fun turningLocationOffClearsLocationSpecificSettingsOnly() {
        val updated = original.toDraft().update(ReminderFormEvent.LocationEnabled(false)).applyTo(original, now)
        assertFalse(updated.isLocationBased)
        assertNull(updated.latitude)
        assertNull(updated.longitude)
        assertNull(updated.geofenceDwellTimeMillis)
        assertEquals(0, updated.geofenceTransitionType)
        assertEquals(1, updated.geofencePriority)
        assertEquals(original.recurrenceIntervalMillis, updated.recurrenceIntervalMillis)
    }

    @Test
    fun enablingLocationStartsWithAnArrivalTrigger() {
        val draft = original.toDraft().update(ReminderFormEvent.LocationEnabled(false))
            .update(ReminderFormEvent.LocationEnabled(true))
        assertEquals(1, draft.geofenceTransitionType)
        assertTrue(draft.isLocationBased)
        assertTrue(ReminderFormError.LOCATION in draft.validate(now, original.reminderTime))
    }

    @Test
    fun undoingAnEditRestoresTheUnchangedState() {
        val state = EditReminderUiState(original = original, draft = original.toDraft(), isLoading = false)
        assertFalse(state.hasChanges)
        val edited = state.copy(draft = state.draft.update(ReminderFormEvent.Title("Different title")))
        assertTrue(edited.hasChanges)
        assertFalse(edited.copy(draft = edited.draft.update(ReminderFormEvent.Title(original.title))).hasChanges)
    }
}
