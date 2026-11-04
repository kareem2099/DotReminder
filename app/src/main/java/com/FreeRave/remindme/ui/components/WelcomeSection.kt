package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.reminders.upcomingTime
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WelcomeSection(
    reminders: List<Reminder>,
    currentTime: Long,
    userName: String? = null,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, MMMM d", locale) }
    val hour = remember(currentTime) { Calendar.getInstance().apply { timeInMillis = currentTime }.get(Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(when {
        hour < 12 -> R.string.home_greeting_morning
        hour < 17 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    })
    val displayName = userName?.trim()?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.home_greeting_default_name)
    val upcoming = reminders.count { it.upcomingTime(currentTime) >= currentTime }
    val overdue = reminders.size - upcoming

    Column(modifier.fillMaxWidth()) {
        Text(
            dateFormat.format(Date(currentTime)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.home_greeting_name, greeting, displayName),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stats = listOf(
                ReminderStat(stringResource(R.string.home_total), reminders.size,
                    Icons.Outlined.Layers, MaterialTheme.colorScheme.primary),
                ReminderStat(stringResource(R.string.home_upcoming), upcoming,
                    Icons.Outlined.Schedule, MaterialTheme.colorScheme.primary),
                ReminderStat(stringResource(R.string.home_overdue), overdue, Icons.Outlined.Timelapse,
                    if (overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            )
            if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f) {
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        stats.forEach { stat ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(stat.icon, null, tint = stat.color, modifier = Modifier.size(20.dp))
                                Text(stat.title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stat.value.toString(), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.forEach { stat ->
                        StatCard(stat.title, stat.value, stat.icon, stat.color, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

private data class ReminderStat(val title: String, val value: Int, val icon: ImageVector, val color: Color)

@Composable
private fun StatCard(title: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier) {
    val numberFormat = remember(Locale.getDefault()) { NumberFormat.getIntegerInstance() }
    GlassSurface(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier
                .background(color.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
                .padding(7.dp).size(18.dp))
            Text(
                numberFormat.format(value),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
