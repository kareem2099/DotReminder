package com.FreeRave.remindme.screens.home

import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.model.QuickFilter
import com.FreeRave.remindme.reminders.upcomingTime
import java.util.Calendar

/** Always filter the source list so changing a filter can restore previously hidden items. */
internal fun filterHomeReminders(
    reminders: List<Reminder>,
    query: String,
    category: String?,
    quickFilter: QuickFilter,
    currentTime: Long = System.currentTimeMillis()
): List<Reminder> {
    val start = Calendar.getInstance().apply {
        timeInMillis = currentTime
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (quickFilter == QuickFilter.THIS_WEEK) set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }
    val end = (start.clone() as Calendar).apply {
        if (quickFilter == QuickFilter.THIS_WEEK) add(Calendar.WEEK_OF_YEAR, 1) else add(Calendar.DAY_OF_MONTH, 1)
    }
    val search = query.trim()
    return reminders.filter { reminder ->
        val scheduledTime = reminder.upcomingTime(currentTime)
        val matchesTime = when (quickFilter) {
            QuickFilter.ALL -> true
            QuickFilter.TODAY, QuickFilter.THIS_WEEK ->
                scheduledTime >= start.timeInMillis && scheduledTime < end.timeInMillis
            QuickFilter.OVERDUE, QuickFilter.COMPLETED -> scheduledTime < currentTime
        }
        matchesTime && (category == null || reminder.category == category) &&
            (search.isEmpty() || reminder.title.contains(search, ignoreCase = true) ||
                reminder.description.contains(search, ignoreCase = true) || reminder.category.contains(search, ignoreCase = true))
    }
}
