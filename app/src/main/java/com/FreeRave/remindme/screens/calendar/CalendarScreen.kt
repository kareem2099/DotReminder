package com.FreeRave.remindme.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.FreeRave.remindme.ui.layout.*
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.theme.OceanBlueBackground
import com.FreeRave.remindme.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import com.FreeRave.remindme.data.ThemeSetting
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) {
    val viewModel: CalendarViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val reminders by viewModel.selectedDateReminders.collectAsState()
    val selectedDateMillis by viewModel.selectedDateInMillis.collectAsState()
    val currentTheme by mainViewModel.currentTheme.collectAsStateWithLifecycle()
    val isSystemDarkTheme = isSystemInDarkTheme()
    val resolvedUseDarkTheme = when (currentTheme) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        else -> isSystemDarkTheme
    }

    // Month navigation state
    var displayedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    OceanBlueBackground(
        themeSetting = currentTheme,
        useDarkTheme = resolvedUseDarkTheme
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Calendar") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            AdaptiveContainer { _, containerModifier ->
                Column(
                    modifier = containerModifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(horizontal = getResponsivePadding(16.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Month navigation header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear--
                            } else displayedMonth--
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                        }

                        val monthName = Calendar.getInstance().apply {
                            set(displayedYear, displayedMonth, 1)
                        }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

                        Text(
                            text = "$monthName $displayedYear",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear++
                            } else displayedMonth++
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                        }
                    }

                    AdaptiveSpacer(getResponsiveSpacing(8.dp))

                    // Day of week headers
                    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AdaptiveSpacer(getResponsiveSpacing(8.dp))

                    // Calendar grid
                    val days = buildCalendarDays(displayedYear, displayedMonth)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        userScrollEnabled = false
                    ) {
                        items(days) { (dayOfMonth, dayCal) ->
                            DayItem(
                                dayOfMonth = dayOfMonth,
                                isSelected = isSameDay(dayCal, selectedDateMillis),
                                hasReminders = if (dayOfMonth > 0)
                                    viewModel.hasRemindersOnDate(displayedYear, displayedMonth, dayOfMonth)
                                else false,
                                onClick = if (dayOfMonth > 0) {
                                    { viewModel.selectDate(displayedYear, displayedMonth, dayOfMonth) }
                                } else null
                            )
                        }
                    }

                    AdaptiveSpacer(getResponsiveSpacing(16.dp))

                    // Selected date header
                    Text(
                        text = formatDateMillis(selectedDateMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    AdaptiveSpacer(getResponsiveSpacing(8.dp))

                    // Reminders list
                    if (reminders.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reminders for this date",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

private fun buildCalendarDays(year: Int, month: Int): List<Pair<Int, Calendar>> {
    val days = mutableListOf<Pair<Int, Calendar>>()
    val firstDayOfMonth = Calendar.getInstance().apply { set(year, month, 1) }
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)

    // Empty cells before first day
    repeat(firstDayOfWeek - 1) {
        days.add(Pair(0, Calendar.getInstance()))
    }

    // Days of month
    val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..daysInMonth) {
        val dayCal = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        days.add(Pair(day, dayCal))
    }

    return days
}

private fun isSameDay(cal: Calendar, millis: Long): Boolean {
    val other = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == other.get(Calendar.MONTH) &&
            cal.get(Calendar.DAY_OF_MONTH) == other.get(Calendar.DAY_OF_MONTH)
}

@Composable
fun DayItem(
    dayOfMonth: Int,
    isSelected: Boolean,
    hasReminders: Boolean,
    onClick: (() -> Unit)?
) {
    if (dayOfMonth == 0) {
        Spacer(modifier = Modifier.aspectRatio(1f))
        return
    }

    val today = Calendar.getInstance()
    val isToday = dayOfMonth == today.get(Calendar.DAY_OF_MONTH)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (hasReminders) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(getResponsivePadding(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (reminder.isLocationBased) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(getResponsiveSpacing(12.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatTime(reminder.reminderTime),
                    style = MaterialTheme.typography.labelSmall,
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
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}