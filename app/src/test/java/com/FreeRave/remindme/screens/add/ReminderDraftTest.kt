package com.FreeRave.remindme.screens.add

import com.FreeRave.remindme.ui.reminder.DAY_MILLIS
import com.FreeRave.remindme.ui.reminder.HOUR_MILLIS
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormError
import com.FreeRave.remindme.ui.reminder.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDraftTest {
    private val now = 1_800_000_000_000L
    private val valid = ReminderDraft(title = "Call home", reminderTimeMillis = now + HOUR_MILLIS)

    @Test
    fun aSimpleFutureReminderIsValid() {
        assertTrue(valid.validate(now).isEmpty())
    }

    @Test
    fun blankTitleAndPastTimeHaveSeparateFieldErrors() {
        assertEquals(setOf(ReminderFormError.TITLE, ReminderFormError.TIME),
            valid.copy(title = "  ", reminderTimeMillis = now - 1).validate(now))
    }

    @Test
    fun recurringReminderNeedsAPositiveInterval() {
        assertEquals(setOf(ReminderFormError.INTERVAL), valid.copy(isRecurring = true).validate(now))
        assertEquals(setOf(ReminderFormError.INTERVAL),
            valid.copy(isRecurring = true, recurrenceIntervalMillis = 0).validate(now))
        assertTrue(valid.copy(isRecurring = true, recurrenceIntervalMillis = DAY_MILLIS).validate(now).isEmpty())
    }

    @Test
    fun repeatEndMustBeAfterTheFirstReminder() {
        assertEquals(setOf(ReminderFormError.END_TIME), valid.copy(
            isRecurring = true, recurrenceIntervalMillis = DAY_MILLIS, recurrenceEndTime = now
        ).validate(now))
    }

    @Test
    fun disabledOptionsDoNotInvalidateASimpleReminder() {
        assertTrue(valid.copy(recurrenceIntervalMillis = -1, recurrenceEndTime = now - 1,
            latitude = 1000.0, geofenceTransitionType = 99).validate(now).isEmpty())
    }

    @Test
    fun locationReminderNeedsBothCoordinatesAndATrigger() {
        assertEquals(setOf(ReminderFormError.LOCATION, ReminderFormError.TRIGGER),
            valid.copy(isLocationBased = true).validate(now))
        assertTrue(valid.copy(isLocationBased = true, latitude = 30.0, longitude = 31.0,
            geofenceTransitionType = 1).validate(now).isEmpty())
    }

    @Test
    fun invalidCoordinateRangesCannotBeSaved() {
        assertEquals(setOf(ReminderFormError.LOCATION), valid.copy(isLocationBased = true,
            latitude = 91.0, longitude = 31.0, geofenceTransitionType = 1).validate(now))
        assertEquals(setOf(ReminderFormError.LOCATION), valid.copy(isLocationBased = true,
            latitude = 30.0, longitude = Double.NaN, geofenceTransitionType = 1).validate(now))
    }
}
