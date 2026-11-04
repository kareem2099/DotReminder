package com.FreeRave.remindme.screens.home

import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.model.QuickFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HomeReminderFiltersTest {
    private val noon = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 29, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val reminders = listOf(
        Reminder(id = "work", title = "Project catch-up", category = "Work", reminderTime = noon + 3_600_000),
        Reminder(id = "personal", title = "Call home", description = "Ask about the project", category = "Personal", reminderTime = noon + 7_200_000),
        Reminder(id = "late", title = "Send notes", category = "Work", reminderTime = noon - 3_600_000)
    )

    @Test
    fun changingCategoryDuringSearchRestoresMatchingItems() {
        assertEquals(listOf("work"), filter("project", "Work").map { it.id })
        assertEquals(listOf("personal"), filter("project", "Personal").map { it.id })
        assertEquals(listOf("work", "personal"), filter("project").map { it.id })
    }

    @Test
    fun searchMatchesDescriptionsAndCategoriesIgnoringCaseAndOuterSpaces() {
        assertEquals(listOf("work", "personal"), filter(" PROJECT ").map { it.id })
        assertEquals(listOf("personal"), filter("PERSONAL").map { it.id })
    }

    @Test
    fun clearingSearchAndQuickFilterRestoresTheSourceList() {
        assertEquals(emptyList<Reminder>(), filter("missing"))
        assertEquals(listOf("late"), filter(quickFilter = QuickFilter.OVERDUE).map { it.id })
        assertEquals(reminders, filter())
    }

    @Test
    fun todayIncludesMidnightAndExcludesTheNextMidnight() {
        val midnight = Calendar.getInstance().apply {
            timeInMillis = noon
            set(Calendar.HOUR_OF_DAY, 0)
        }
        val tomorrow = (midnight.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val boundaries = listOf(
            Reminder(id = "before", reminderTime = midnight.timeInMillis - 1),
            Reminder(id = "start", reminderTime = midnight.timeInMillis),
            Reminder(id = "end", reminderTime = tomorrow.timeInMillis - 1),
            Reminder(id = "tomorrow", reminderTime = tomorrow.timeInMillis)
        )
        assertEquals(listOf("start", "end"),
            filterHomeReminders(boundaries, "", null, QuickFilter.TODAY, noon).map { it.id })
    }

    @Test
    fun thisWeekUsesLocalWeekBoundaries() {
        val weekStart = Calendar.getInstance().apply {
            timeInMillis = noon
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
        }
        val nextWeek = (weekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 1) }
        val boundaries = listOf(
            Reminder(id = "before", reminderTime = weekStart.timeInMillis - 1),
            Reminder(id = "start", reminderTime = weekStart.timeInMillis),
            Reminder(id = "end", reminderTime = nextWeek.timeInMillis - 1),
            Reminder(id = "next", reminderTime = nextWeek.timeInMillis)
        )
        assertEquals(listOf("start", "end"),
            filterHomeReminders(boundaries, "", null, QuickFilter.THIS_WEEK, noon).map { it.id })
    }

    @Test
    fun categoryAndOverdueFiltersApplyTogether() {
        assertEquals(listOf("late"), filter(category = "Work", quickFilter = QuickFilter.OVERDUE).map { it.id })
        assertEquals(emptyList<Reminder>(), filter(category = "Personal", quickFilter = QuickFilter.OVERDUE))
    }

    private fun filter(query: String = "", category: String? = null, quickFilter: QuickFilter = QuickFilter.ALL) =
        filterHomeReminders(reminders, query, category, quickFilter, noon)
}
