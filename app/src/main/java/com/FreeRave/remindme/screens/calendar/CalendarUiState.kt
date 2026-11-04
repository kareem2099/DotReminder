package com.FreeRave.remindme.screens.calendar

import com.FreeRave.remindme.model.Reminder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.from(selectedDate),
    val today: LocalDate = LocalDate.now(),
    val zoneId: String = ZoneId.systemDefault().id,
    val days: List<CalendarDay> = emptyList(),
    val entries: List<CalendarEntry> = emptyList(),
    val monthReminderCount: Int = 0,
    val scheduledDayCount: Int = 0,
    val isLoading: Boolean = true,
    val hasError: Boolean = false
)

internal fun buildCalendarState(
    selectedDate: LocalDate,
    reminders: List<Reminder>,
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
    isLoading: Boolean = false,
    hasError: Boolean = false
): CalendarUiState {
    val month = YearMonth.from(selectedDate)
    val byDate = calendarDates(month, firstDayOfWeek).associateWith { calendarEntries(reminders, it, zone) }
    val inMonth = byDate.filterKeys { YearMonth.from(it) == month }
    return CalendarUiState(
        selectedDate = selectedDate,
        displayedMonth = month,
        today = today,
        zoneId = zone.id,
        days = calendarDays(month, selectedDate, today, firstDayOfWeek, byDate.mapValues { it.value.size }),
        entries = byDate[selectedDate].orEmpty(),
        monthReminderCount = inMonth.values.flatten().map { it.reminder.id }.distinct().size,
        scheduledDayCount = inMonth.count { it.value.isNotEmpty() },
        isLoading = isLoading,
        hasError = hasError
    )
}
