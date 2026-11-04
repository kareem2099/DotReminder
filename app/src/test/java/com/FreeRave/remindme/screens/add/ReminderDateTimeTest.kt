package com.FreeRave.remindme.screens.add

import com.FreeRave.remindme.ui.reminder.applyPickerDate
import com.FreeRave.remindme.ui.reminder.applyPickerTime
import com.FreeRave.remindme.ui.reminder.initialReminderTime
import com.FreeRave.remindme.ui.reminder.pickerDateMillis
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDateTimeTest {
    @Test
    fun confirmingTheSameDateKeepsTheExactOriginalTimestamp() {
        val zone = ZoneId.of("Africa/Cairo")
        val time = timestamp("2026-11-02T18:35:03", zone) + 250
        assertEquals(time, applyPickerDate(pickerDateMillis(time, zone), time, zone))
    }

    @Test
    fun confirmingTheSameHourAndMinuteDoesNotMakeALegacyReminderDirty() {
        val zone = ZoneId.of("Africa/Cairo")
        val time = timestamp("2026-11-02T18:35:03", zone) + 250
        assertEquals(time, applyPickerTime(time, 18, 35, zone))
    }

    @Test
    fun pickerRoundTripKeepsTheLocalDateInPositiveOffsets() {
        val zone = ZoneId.of("Pacific/Kiritimati")
        val time = timestamp("2026-11-02T00:15:00", zone)
        assertEquals(Instant.parse("2026-11-02T00:00:00Z").toEpochMilli(), pickerDateMillis(time, zone))
        assertEquals(time, applyPickerDate(pickerDateMillis(time, zone), time, zone))
    }

    @Test
    fun pickerRoundTripKeepsTheLocalDateInNegativeOffsets() {
        val zone = ZoneId.of("America/Los_Angeles")
        val time = timestamp("2026-11-02T23:45:00", zone)
        assertEquals(Instant.parse("2026-11-02T00:00:00Z").toEpochMilli(), pickerDateMillis(time, zone))
        assertEquals(time, applyPickerDate(pickerDateMillis(time, zone), time, zone))
    }

    @Test
    fun choosingAnotherDateKeepsTheSelectedLocalTime() {
        val zone = ZoneId.of("Africa/Cairo")
        val time = timestamp("2026-11-02T18:35:00", zone)
        val pickedDate = Instant.parse("2026-11-04T00:00:00Z").toEpochMilli()
        assertEquals(timestamp("2026-11-04T18:35:00", zone), applyPickerDate(pickedDate, time, zone))
    }

    @Test
    fun choosingTimeKeepsTheSelectedDateAndClearsSeconds() {
        val zone = ZoneId.of("Asia/Tokyo")
        val time = timestamp("2026-11-02T23:59:30", zone)
        assertEquals(timestamp("2026-11-02T07:10:00", zone), applyPickerTime(time, 7, 10, zone))
    }

    @Test
    fun aDefaultTimeThatAlreadyPassedMovesToTomorrow() {
        val zone = ZoneId.of("Africa/Cairo")
        val now = timestamp("2026-11-02T18:00:00", zone)
        assertEquals(timestamp("2026-11-03T09:30:00", zone), initialReminderTime(now, 9, 30, zone))
    }

    @Test
    fun anUpcomingDefaultTimeStaysOnTheSameDay() {
        val zone = ZoneId.of("Africa/Cairo")
        val now = timestamp("2026-11-02T08:00:00", zone)
        assertEquals(timestamp("2026-11-02T09:30:00", zone), initialReminderTime(now, 9, 30, zone))
    }

    @Test
    fun missingDefaultUsesAFutureTimeAcrossMidnight() {
        val zone = ZoneId.of("Africa/Cairo")
        val now = timestamp("2026-11-02T23:50:35", zone)
        val result = initialReminderTime(now, -1, -1, zone)
        assertEquals(timestamp("2026-11-03T00:50:00", zone), result)
        assertTrue(result > now)
    }

    private fun timestamp(local: String, zone: ZoneId) = LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()
}
