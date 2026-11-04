package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.reminders.upcomingTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PremiumReminderCard(
    reminder: Reminder,
    onReminderClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currentTime: Long = System.currentTimeMillis()
) {
    SwipeToDelete(onDelete = onDelete, modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        ReminderCard(reminder, onReminderClick, currentTime = currentTime)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onReminderClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentTime: Long = System.currentTimeMillis(),
    timeLabel: String? = null,
    showStatus: Boolean = true
) {
    val locale = Locale.getDefault()
    val displayTime = reminder.upcomingTime(currentTime)
    val time = remember(displayTime, locale) {
        SimpleDateFormat("MMM d · h:mm a", locale).format(Date(displayTime))
    }
    val overdue = showStatus && displayTime < currentTime
    val accent = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    GlassSurface(modifier = modifier.fillMaxWidth(), onClick = onReminderClick) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    if (reminder.isLocationBased) Icons.Outlined.LocationOn else Icons.Outlined.NotificationsNone,
                    contentDescription = stringResource(
                        if (reminder.isLocationBased) R.string.home_location else R.string.home_scheduled
                    ),
                    tint = accent,
                    modifier = Modifier.background(accent.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
                        .padding(11.dp).size(20.dp)
                )
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
            if (reminder.description.isNotBlank()) {
                Text(reminder.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.Schedule, null, tint = accent, modifier = Modifier.size(14.dp))
                    Text(timeLabel ?: time, style = MaterialTheme.typography.labelMedium, color = accent)
                }
                if (reminder.category.isNotBlank()) {
                    Text(reminder.category, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (reminder.isRecurring) {
                    Icon(Icons.Outlined.Repeat, stringResource(R.string.home_recurring),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            if (showStatus) StatusLabel(overdue, accent)
        }
    }
}

@Composable
private fun StatusLabel(overdue: Boolean, color: Color) {
    Row(
        modifier = Modifier.background(color.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(if (overdue) R.string.home_overdue else R.string.home_upcoming),
            style = MaterialTheme.typography.labelSmall, color = color)
    }
}
