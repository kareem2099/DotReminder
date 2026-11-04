package com.FreeRave.remindme.screens.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@Preview(name = "Calendar · Light", widthDp = 393, heightDp = 852)
@Composable
private fun CalendarLightPreview() = CalendarPreview(false)

@Preview(name = "Calendar · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun CalendarDarkPreview() = CalendarPreview(true)

@Preview(name = "Calendar · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun CalendarCompactPreview() = CalendarPreview(false)

@Preview(name = "Calendar · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun CalendarTabletPreview() = CalendarPreview(false)

@Preview(name = "Calendar · RTL", widthDp = 393, heightDp = 852, locale = "ar")
@Composable
private fun CalendarRtlPreview() = CalendarPreview(true)

@Preview(name = "Calendar · Loading", widthDp = 393, heightDp = 852)
@Composable
private fun CalendarLoadingPreview() = CalendarPreview(false, loading = true)

@Preview(name = "Calendar · Error", widthDp = 393, heightDp = 852)
@Composable
private fun CalendarErrorPreview() = CalendarPreview(true, failed = true)

@Preview(name = "Calendar · Empty", widthDp = 393, heightDp = 852)
@Composable
private fun CalendarEmptyPreview() = CalendarPreview(false, empty = true)

@Composable
private fun CalendarPreview(dark: Boolean, loading: Boolean = false, failed: Boolean = false, empty: Boolean = false) {
    val zone = ZoneId.systemDefault()
    val date = LocalDate.of(2026, 10, 29)
    val reminders = if (loading || failed || empty) emptyList() else listOf(
        Reminder(id = "morning", title = "A little time to reset", category = "Personal",
            reminderTime = date.atTime(9, 30).atZone(zone).toInstant().toEpochMilli()),
        Reminder(id = "daily", title = "Stay hydrated", category = "Health", isRecurring = true,
            recurrenceIntervalMillis = 86_400_000,
            reminderTime = date.minusDays(7).atTime(12, 0).atZone(zone).toInstant().toEpochMilli())
    )
    val state = buildCalendarState(date, reminders, date, zone, DayOfWeek.SUNDAY, loading, failed)
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            CalendarScreenContent(state, onBack = {}, onSelectDate = {}, onChangeMonth = {}, onToday = {},
                onRetry = {}, onReminderClick = {})
        }
    }
}
