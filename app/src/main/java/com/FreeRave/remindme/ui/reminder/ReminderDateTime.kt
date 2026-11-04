package com.FreeRave.remindme.ui.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Material date pickers represent calendar dates at UTC midnight, independent of the device's zone. */
internal fun pickerDateMillis(timeMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun applyPickerDate(dateMillis: Long, timeMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val original = Instant.ofEpochMilli(timeMillis).atZone(zone)
    if (date == original.toLocalDate()) return timeMillis
    val time = original.toLocalTime().withSecond(0).withNano(0)
    return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}

internal fun applyPickerTime(timeMillis: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val original = Instant.ofEpochMilli(timeMillis).atZone(zone)
    if (hour == original.hour && minute == original.minute) return timeMillis
    return original.toLocalDate().atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}

internal fun initialReminderTime(now: Long, defaultHour: Int, defaultMinute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val current = Instant.ofEpochMilli(now).atZone(zone)
    if (defaultHour !in 0..23 || defaultMinute !in 0..59) {
        return current.plusHours(1).withSecond(0).withNano(0).toInstant().toEpochMilli()
    }
    val candidate = current.toLocalDate().atTime(defaultHour, defaultMinute).atZone(zone)
    return (if (candidate.isAfter(current)) candidate else candidate.plusDays(1)).toInstant().toEpochMilli()
}
