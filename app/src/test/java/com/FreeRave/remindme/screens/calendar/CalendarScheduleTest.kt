package com.FreeRave.remindme.screens.calendar

import com.FreeRave.remindme.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalendarScheduleTest {
    private val utc = ZoneId.of("UTC")
    private fun millis(value: String) = Instant.parse(value).toEpochMilli()
    private fun at(id: String, value: String) = Reminder(id = id, reminderTime = millis(value))

    @Test
    fun oneTimeRemindersIncludeMidnightAndExcludeTheNextMidnight() {
        val date = LocalDate.of(2026, 1, 2)
        val reminders = listOf(at("before", "2026-01-01T23:59:59.999Z"), at("start", "2026-01-02T00:00:00Z"),
            at("last", "2026-01-02T23:59:59.999Z"), at("next", "2026-01-03T00:00:00Z"))
        assertEquals(listOf("start", "last"), calendarEntries(reminders, date, utc).map { it.reminder.id })
    }

    @Test
    fun aThirtySixHourRepeatIsFoundInsideTheDayWithoutNeedingToLandAtMidnight() {
        val reminder = at("repeat", "2026-01-01T18:00:00Z").copy(isRecurring = true, recurrenceIntervalMillis = 36 * 3_600_000L)
        assertTrue(calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 2), utc).isEmpty())
        assertEquals(millis("2026-01-03T06:00:00Z"),
            calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 3), utc).single().occurrenceTime)
    }

    @Test
    fun hourlyRemindersUseTheActualOccurrenceForThatDate() {
        val reminder = at("hourly", "2026-01-01T13:15:00Z").copy(isRecurring = true, recurrenceIntervalMillis = 3_600_000)
        val entry = calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 2), utc).single()
        assertEquals(millis("2026-01-02T00:15:00Z"), entry.occurrenceTime)
        assertSame(reminder, entry.reminder)
        assertEquals(millis("2026-01-01T13:15:00Z"), reminder.reminderTime)
    }

    @Test
    fun recurrenceEndTimeIsInclusiveAndStopsFutureMarkers() {
        val reminder = at("daily", "2026-01-01T09:00:00Z").copy(isRecurring = true,
            recurrenceIntervalMillis = 86_400_000, recurrenceEndTime = millis("2026-01-03T09:00:00Z"))
        assertEquals(1, calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 3), utc).size)
        assertTrue(calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 4), utc).isEmpty())
        val endedEarlier = reminder.copy(recurrenceEndTime = millis("2026-01-03T08:59:59.999Z"))
        assertTrue(calendarEntries(listOf(endedEarlier), LocalDate.of(2026, 1, 3), utc).isEmpty())
    }

    @Test
    fun repeatsNeverAppearBeforeTheOriginalStart() {
        val reminder = at("daily", "2026-01-02T09:00:00Z").copy(isRecurring = true, recurrenceIntervalMillis = 86_400_000)
        assertTrue(calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 1), utc).isEmpty())
    }

    @Test
    fun invalidIntervalsDoNotCreatePhantomRecurrences() {
        val original = at("broken", "2026-01-01T09:00:00Z").copy(isRecurring = true)
        for (interval in listOf(null, 0L, -1L)) {
            val reminder = original.copy(recurrenceIntervalMillis = interval)
            assertEquals(1, calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 1), utc).size)
            assertTrue(calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 2), utc).isEmpty())
        }
    }

    @Test
    fun aTwentyThreeHourLocalDayDoesNotIncludeTheNextDay() {
        val zone = ZoneId.of("America/New_York")
        val reminders = listOf(at("late", "2026-03-09T03:30:00Z"), at("next", "2026-03-09T04:00:00Z"))
        assertEquals(listOf("late"), calendarEntries(reminders, LocalDate.of(2026, 3, 8), zone).map { it.reminder.id })
    }

    @Test
    fun aTwentyFiveHourLocalDayIncludesItsLastHour() {
        val zone = ZoneId.of("America/New_York")
        val reminders = listOf(at("late", "2026-11-02T04:30:00Z"), at("next", "2026-11-02T05:00:00Z"))
        assertEquals(listOf("late"), calendarEntries(reminders, LocalDate.of(2026, 11, 1), zone).map { it.reminder.id })
    }

    @Test
    fun agendaSortingUsesOccurrenceTimeRatherThanTheOriginalStart() {
        val repeat = at("repeat", "2026-01-01T18:00:00Z").copy(isRecurring = true, recurrenceIntervalMillis = 36 * 3_600_000L)
        val early = at("early", "2026-01-03T05:00:00Z")
        assertEquals(listOf("early", "repeat"),
            calendarEntries(listOf(repeat, early), LocalDate.of(2026, 1, 3), utc).map { it.reminder.id })
    }

    @Test
    fun anOverflowingIntervalDoesNotWrapIntoAnUnrelatedDay() {
        val reminder = at("huge", "2026-01-01T09:00:00Z").copy(isRecurring = true, recurrenceIntervalMillis = Long.MAX_VALUE)
        assertTrue(calendarEntries(listOf(reminder), LocalDate.of(2026, 1, 2), utc).isEmpty())
    }

    @Test
    fun monthSummaryCountsUniqueRemindersAndStopsAtTheRepeatEnd() {
        val daily = at("daily", "2026-01-01T09:00:00Z").copy(isRecurring = true, recurrenceIntervalMillis = 86_400_000,
            recurrenceEndTime = millis("2026-01-03T09:00:00Z"))
        val once = at("once", "2026-01-02T10:00:00Z")
        val selected = LocalDate.of(2026, 1, 2)
        val state = buildCalendarState(selected, listOf(daily, once), selected, utc, DayOfWeek.MONDAY)
        assertEquals(2, state.monthReminderCount)
        assertEquals(3, state.scheduledDayCount)
        assertEquals(2, state.entries.size)
        assertEquals(1, state.days.single { it.date == LocalDate.of(2026, 1, 3) }.reminderCount)
        assertEquals(0, state.days.single { it.date == LocalDate.of(2026, 1, 4) }.reminderCount)
    }
}
