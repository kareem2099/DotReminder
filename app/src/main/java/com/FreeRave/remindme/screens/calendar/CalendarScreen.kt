package com.FreeRave.remindme.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import java.util.*
import kotlin.collections.ArrayList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) {
    val viewModel: CalendarViewModel = hiltViewModel()
    val reminders by viewModel.selectedDateReminders.collectAsState()
    val selectedDateMillis by viewModel.selectedDateInMillis.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { windowSizeClass, containerModifier ->
            AdaptiveColumn(
                modifier = containerModifier.padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simple month calendar for current month
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SimpleMonthCalendar(viewModel = viewModel, selectedDateMillis = selectedDateMillis)
                }

                AdaptiveSpacer(getResponsiveSpacing(16.dp))

                // Selected date header
                Text(
                    text = formatDateMillis(selectedDateMillis),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                AdaptiveSpacer(getResponsiveSpacing(16.dp))

                // Reminders list for selected date
                if (reminders.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No reminders for this date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp))
                    ) {
                        items(reminders) { reminder ->
                            CalendarReminderItem(reminder) {
                                navController.navigate(Screen.Edit.createRoute(reminder.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleMonthCalendar(viewModel: CalendarViewModel, selectedDateMillis: Long) {
    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH)

    // Create days for the month
    val days = ArrayList<Pair<Int, Calendar>>()
    val monthCalendar = Calendar.getInstance().apply {
        set(currentYear, currentMonth, 1)
    }

    // Add empty days for start of week
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK)
    for (i in 1 until firstDayOfWeek) {
        days.add(Pair(0, Calendar.getInstance())) // Empty
    }

    // Add days of the month
    val daysInMonth = monthCalendar.getMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..daysInMonth) {
        val dayCal = Calendar.getInstance().apply {
            set(currentYear, currentMonth, day)
        }
        days.add(Pair(day, dayCal))
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp))
    ) {
        items(days.chunked(7)) { week ->
            Column(
                modifier = Modifier.width(getResponsiveColumnWidth(50.dp)),
                verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp))
            ) {
                week.forEach { (dayOfMonth, dayCal) ->
                    DayItem(
                        dayOfMonth = dayOfMonth,
                        isSelected = dayCal.timeInMillis == selectedDateMillis,
                        hasReminders = if (dayOfMonth > 0) viewModel.hasRemindersOnDate(currentYear, currentMonth, dayOfMonth) else false,
                        onClick = if (dayOfMonth > 0) { { viewModel.selectDate(currentYear, currentMonth, dayOfMonth) } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun DayItem(dayOfMonth: Int, isSelected: Boolean, hasReminders: Boolean, onClick: (() -> Unit)?) {
    if (dayOfMonth == 0) {
        Spacer(modifier = Modifier.aspectRatio(1f))
        return
    }

    val isToday = run {
        val today = Calendar.getInstance()
        val dayCal = Calendar.getInstance().apply {
            set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), dayOfMonth)
        }
        dayCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) &&
        dayCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
        dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            )
            .padding(getResponsiveSpacing(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hasReminders) {
                Box(
                    modifier = Modifier
                        .size(getResponsiveSpacing(8.dp))
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }
}

@Composable
fun CalendarReminderItem(reminder: Reminder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(getResponsivePadding(16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(getResponsiveSpacing(16.dp))
                    .background(
                        if (reminder.isLocationBased) Color.Blue else Color.Green,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(getResponsiveSpacing(16.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = formatTime(reminder.reminderTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDateMillis(dateMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val year = calendar.get(Calendar.YEAR)
    return "$dayName, $monthName $day, $year"
}

private fun formatTime(timeMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
}
