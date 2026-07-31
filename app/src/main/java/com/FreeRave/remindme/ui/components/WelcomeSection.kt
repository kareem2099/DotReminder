package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.ui.theme.AppTypography
import com.FreeRave.remindme.ui.theme.getOceanEnhancedText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WelcomeSection(
    userName: String? = null,
    reminders: List<Reminder>,
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier,
    currentTime: Long = System.currentTimeMillis()
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val currentDate = dateFormat.format(Date(currentTime))
    val currentTimeString = timeFormat.format(Date(currentTime))

    // Calculate stats
    val totalReminders = reminders.size
    val upcomingReminders = reminders.count { it.reminderTime > currentTime }
    val overdueReminders = reminders.count { it.reminderTime < currentTime }

    val calendar = remember(currentTime) { Calendar.getInstance().apply { timeInMillis = currentTime } }
    val greeting = when {
        calendar.get(Calendar.HOUR_OF_DAY) < 12 -> "Good morning"
        calendar.get(Calendar.HOUR_OF_DAY) < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    val displayName = userName ?: "there"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Greeting and Date/Time
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = getOceanEnhancedText("$greeting, $displayName", themeSetting),
                style = AppTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$currentDate • $currentTimeString",
                style = AppTypography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total",
                value = totalReminders.toString(),
                icon = Icons.AutoMirrored.Filled.List,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Upcoming",
                value = upcomingReminders.toString(),
                icon = Icons.Default.Schedule,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Overdue",
                value = overdueReminders.toString(),
                icon = Icons.Default.Warning,
                color = if (overdueReminders > 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = AppTypography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = color
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = AppTypography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
