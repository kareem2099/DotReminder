package com.FreeRave.remindme.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.ui.theme.AppTypography
import com.FreeRave.remindme.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumReminderCard(
    reminder: Reminder,
    onReminderClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currentTime: Long = System.currentTimeMillis()
) {
    val reminderTime = remember(reminder.reminderTime) {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            .format(Date(reminder.reminderTime))
    }

    // Calculate if reminder is overdue (past due) reactively against currentTime
    val isOverdue = remember(currentTime, reminder.reminderTime) {
        reminder.reminderTime < currentTime
    }

    // Accent color based on overdue status
    val statusColor = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    SwipeToDelete(
        onDelete = onDelete
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onReminderClick() },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            ) {
                // Leading Status Accent Bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(statusColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row: Icon, Title & Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Type Icon (Location or Notification)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (reminder.isLocationBased)
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    else
                                        statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (reminder.isLocationBased)
                                    Icons.Default.LocationOn else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (reminder.isLocationBased)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = reminder.title,
                                style = AppTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status Badge Pill
                        Surface(
                            color = if (isOverdue)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isOverdue) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Overdue",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isOverdue) "Overdue" else "Upcoming",
                                    style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOverdue)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Description
                    if (reminder.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = reminder.description,
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Information & Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reminder Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = reminderTime,
                                style = AppTypography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = statusColor
                            )
                        }

                        // Category Tag
                        if (reminder.category.isNotBlank()) {
                            Surface(
                                color = CategoryUtils.getCategoryColor(reminder.category)?.copy(alpha = 0.2f)
                                    ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = reminder.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = AppTypography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
