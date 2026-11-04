package com.FreeRave.remindme.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.FreeRave.remindme.model.Reminder

/** Part of the page's list so headers and reminders scroll together. */
fun LazyListScope.reminderItems(
    reminders: List<Reminder>,
    currentTime: Long,
    onReminderClick: (Reminder) -> Unit,
    onDelete: (String) -> Unit
) {
    items(items = reminders, key = { "reminder:${it.id}" }, contentType = { "reminder" }) { reminder ->
        PremiumReminderCard(
            reminder = reminder,
            currentTime = currentTime,
            onReminderClick = { onReminderClick(reminder) },
            onDelete = { onDelete(reminder.id) },
            modifier = Modifier.animateItem(
                fadeInSpec = tween(220),
                fadeOutSpec = tween(160),
                placementSpec = tween(240)
            )
        )
    }
}
