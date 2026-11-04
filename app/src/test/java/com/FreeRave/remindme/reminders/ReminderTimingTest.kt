package com.FreeRave.remindme.reminders

import org.junit.Assert.*
import org.junit.Test
import com.FreeRave.remindme.model.Reminder

class ReminderTimingTest {
    @Test fun aNewReminderThatBecomesDueDuringItsSaveStillGetsAnAlert() {
        assertEquals(2_000L, initialReminderTrigger(1_000, null, null, 1_500, true))
        assertEquals(2_000L, initialReminderTrigger(1_000, 500, null, 1_500, true))
        assertNull(initialReminderTrigger(1_000, null, null, 1_500, false))
        assertNull(initialReminderTrigger(1_000, 500, 1_400, 1_500, true))
    }
    @Test fun activeRepeatsDisplayTheirUpcomingOccurrenceInsteadOfStayingOverdue() {
        val reminder = Reminder(reminderTime = 1_000, isRecurring = true, recurrenceIntervalMillis = 500)
        assertEquals(2_000L, reminder.upcomingTime(1_700))
        assertEquals(1_000L, reminder.copy(recurrenceEndTime = 1_500).upcomingTime(1_700))
    }
    @Test fun aOneTimeReminderIsScheduledOnlyInTheFuture() {
        assertEquals(1_000L, nextReminderOccurrence(1_000, null, null, 900))
        assertNull(nextReminderOccurrence(1_000, null, null, 1_000))
        assertNull(nextReminderOccurrence(1_000, null, null, 1_500))
    }
    @Test fun repeatingRemindersAdvanceFromTheirAnchorAfterDelivery() {
        assertEquals(1_500L, nextReminderOccurrence(1_000, 500, null, 1_000))
        assertEquals(2_000L, nextReminderOccurrence(1_000, 500, null, 1_700))
    }
    @Test fun rebootSkipsMissedRepeatsWithoutDriftingTheSchedule() {
        assertEquals(6_000L, nextReminderOccurrence(1_000, 500, null, 5_900))
    }
    @Test fun recurrenceStopsAtItsEndAndAllowsTheFinalOccurrence() {
        assertEquals(2_000L, nextReminderOccurrence(1_000, 500, 2_000, 1_500))
        assertNull(nextReminderOccurrence(1_000, 500, 2_000, 2_000))
        assertNull(nextReminderOccurrence(1_000, 500, 900, 0))
    }
    @Test fun invalidIntervalsAndTimestampsNeverCreateAlarmLoops() {
        assertNull(nextReminderOccurrence(0, 500, null, 0))
        assertNull(nextReminderOccurrence(1_000, 0, null, 1_000))
        assertNull(nextReminderOccurrence(1_000, -5, null, 1_000))
    }
    @Test fun overflowingDatesNeverWrapAroundIntoAnImmediateAlarm() {
        assertNull(nextReminderOccurrence(Long.MAX_VALUE - 2, 10, null, Long.MAX_VALUE - 1))
        assertNull(nextReminderOccurrence(1, 1, null, Long.MAX_VALUE))
    }
    @Test fun deliveryPrerequisitesAreCheckedInOrder() {
        assertEquals(DeliveryRequirement.NOTIFICATIONS, deliveryRequirement(false, false, false))
        assertEquals(DeliveryRequirement.CHANNEL, deliveryRequirement(true, false, false))
        assertEquals(DeliveryRequirement.EXACT_ALARM, deliveryRequirement(true, true, false))
        assertEquals(DeliveryRequirement.READY, deliveryRequirement(true, true, true))
    }
    @Test fun channelIdsAreStableAndChangeOnlyForExplicitSoundOrVibrationChoices() {
        assertEquals(reminderChannelId("alarm_sound_1", true), reminderChannelId("alarm_sound_1", true))
        assertNotEquals(reminderChannelId("alarm_sound_1", true), reminderChannelId("alarm_sound_1", false))
        assertNotEquals(reminderChannelId("alarm_sound_1", true), reminderChannelId("", true))
        assertEquals(reminderChannelId("", true), reminderChannelId("invalid/sound", true))
    }
}
