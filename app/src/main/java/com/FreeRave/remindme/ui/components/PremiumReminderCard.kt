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
    modifier: Modifier = Modifier
) {
    val createdTime = remember {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            .format(Date(reminder.time))
    }

    val reminderTime = remember {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            .format(Date(reminder.reminderTime))
    }

    // Calculate if reminder is overdue (past due)
    val isOverdue = remember {
        reminder.reminderTime < System.currentTimeMillis()
    }

    // Animate card background based on overdue status
    val cardBackground by animateColorAsState(
        targetValue = when {
            isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300)
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isOverdue -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(300)
    )

    SwipeToDelete(
        onDelete = onDelete
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = cardBorderColor.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground)
                .clickable { onReminderClick() },
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = cardBorderColor
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header Row with Icon and Priority
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Location/Time Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (reminder.isLocationBased)
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (reminder.isLocationBased)
                                Icons.Default.LocationOn else Icons.Default.Notifications,
                            contentDescription = if (reminder.isLocationBased)
                                "Location-based reminder" else "Time-based reminder",
                            tint = if (reminder.isLocationBased)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title and Status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reminder.title,
                                style = AppTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Overdue indicator
                            if (isOverdue) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Overdue",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Status text
                        Text(
                            text = if (isOverdue) "Overdue" else "Upcoming",
                            style = AppTypography.labelSmall,
                            color = if (isOverdue)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = AppTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Time Information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reminder Time
                    Column {
                        Text(
                            text = "Reminder",
                            style = AppTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = reminderTime,
                            style = AppTypography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Created Time
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Created",
                            style = AppTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = createdTime,
                            style = AppTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Category Badge
                if (reminder.category.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = CategoryUtils.getCategoryColor(reminder.category) ?: MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = reminder.category,
                            style = AppTypography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
