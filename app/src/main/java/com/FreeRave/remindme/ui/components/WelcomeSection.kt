package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    modifier: Modifier = Modifier
) {
    val currentTime = remember { System.currentTimeMillis() }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val currentDate = remember { dateFormat.format(Date(currentTime)) }
    val currentTimeString = remember { timeFormat.format(Date(currentTime)) }

    // Calculate stats
    val totalReminders = reminders.size
    val upcomingReminders = reminders.count { it.reminderTime > currentTime }
    val overdueReminders = reminders.count { it.reminderTime < currentTime }
    val completedToday = reminders.count {
        val reminderDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.reminderTime))
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(currentTime))
        reminderDate == today && it.reminderTime < currentTime
    }

    val greeting = when {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12 -> "Good morning"
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 17 -> "Good afternoon"
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
                text = getOceanEnhancedText("$greeting, $displayName!", themeSetting),
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
                icon = Icons.Default.List,
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

        // Completed Today Badge (only show if > 0)
        if (completedToday > 0) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Great job! You completed $completedToday reminder${if (completedToday != 1) "s" else ""} today",
                        style = AppTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = AppTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = AppTypography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}
