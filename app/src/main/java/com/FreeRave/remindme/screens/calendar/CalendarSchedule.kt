package com.FreeRave.remindme.screens.calendar

import com.FreeRave.remindme.model.Reminder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal data class CalendarEntry(val reminder: Reminder, val occurrenceTime: Long)

internal data class CalendarDay(
    val date: LocalDate,
    val isInMonth: Boolean,
    val isSelected: Boolean,
    val isToday: Boolean,
    val reminderCount: Int
)

internal fun calendarDates(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cellCount = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    return List(cellCount) { first.minusDays(leading.toLong()).plusDays(it.toLong()) }
}

internal fun calendarDays(
    month: YearMonth, selected: LocalDate, today: LocalDate, firstDayOfWeek: DayOfWeek,
    counts: Map<LocalDate, Int>
): List<CalendarDay> = calendarDates(month, firstDayOfWeek).map { date ->
    CalendarDay(date, YearMonth.from(date) == month, date == selected, date == today, counts[date] ?: 0)
}

internal fun adjacentMonthDate(selected: LocalDate, offset: Long): LocalDate {
    val month = YearMonth.from(selected).plusMonths(offset)
    return month.atDay(minOf(selected.dayOfMonth, month.lengthOfMonth()))
}

/** One agenda entry per reminder, using its first actual occurrence within the local day. */
internal fun calendarEntries(reminders: List<Reminder>, date: LocalDate, zone: ZoneId): List<CalendarEntry> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return reminders.mapNotNull { reminder ->
        firstOccurrenceInRange(reminder, start, end)?.let { CalendarEntry(reminder, it) }
    }.sortedWith(compareBy<CalendarEntry> { it.occurrenceTime }.thenBy { it.reminder.id })
}

internal fun firstOccurrenceInRange(reminder: Reminder, start: Long, endExclusive: Long): Long? {
    if (start >= endExclusive || reminder.reminderTime >= endExclusive) return null
    val interval = reminder.recurrenceIntervalMillis?.takeIf { reminder.isRecurring && it > 0 }
    val occurrence = if (reminder.reminderTime >= start) {
        reminder.reminderTime
    } else {
        if (interval == null) return null
        try {
            val elapsed = Math.subtractExact(start, reminder.reminderTime)
            val cycles = elapsed / interval + if (elapsed % interval == 0L) 0 else 1
            Math.addExact(reminder.reminderTime, Math.multiplyExact(cycles, interval))
        } catch (_: ArithmeticException) {
            return null
        }
    }
    if (occurrence >= endExclusive) return null
    if (interval != null && reminder.recurrenceEndTime?.let { occurrence > it } == true) return null
    return occurrence
}
