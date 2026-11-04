package com.FreeRave.remindme.screens.calendar

import android.text.format.DateFormat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.calendar.components.CalendarAgendaState
import com.FreeRave.remindme.screens.calendar.components.CalendarMonthCard
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.components.ReminderCard
import com.FreeRave.remindme.ui.reminder.components.ReminderDateDialog
import com.FreeRave.remindme.ui.theme.GlassBackground
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreenContent(
    state: CalendarUiState,
    onBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (Long) -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
    onReminderClick: (String) -> Unit
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val use24Hour = DateFormat.is24HourFormat(context)
    val timeFormatter = remember(locale, use24Hour, state.zoneId) { DateFormat.getTimeFormat(context) }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMM d", locale) }
    val jumpToToday: () -> Unit = {
        onToday()
        scope.launch { listState.animateScrollToItem(1) }
    }
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(),
                    title = { Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back))
                            }
                        }
                    },
                    actions = { TextButton(onClick = jumpToToday) { Text(stringResource(R.string.calendar_today)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    state = listState, modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "intro") {
                        Text(stringResource(R.string.calendar_intro), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item(key = "month") {
                        CalendarMonthCard(state, onSelectDate, onChangeMonth, onChooseDate = { pickerOpen = true })
                    }
                    item(key = "day") {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(state.selectedDate.format(dayFormatter), style = MaterialTheme.typography.titleLarge)
                                Text(pluralStringResource(R.plurals.calendar_reminder_count, state.entries.size, state.entries.size),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (state.hasError || state.entries.isEmpty()) {
                        item(key = "agenda_state") {
                            CalendarAgendaState(state.isLoading, state.hasError, state.selectedDate != state.today, onRetry, jumpToToday)
                        }
                    }
                    items(state.entries, key = { "entry:${it.reminder.id}" }, contentType = { "reminder" }) { entry ->
                        ReminderCard(
                            reminder = entry.reminder,
                            onReminderClick = { onReminderClick(entry.reminder.id) },
                            timeLabel = timeFormatter.format(Date(entry.occurrenceTime)), showStatus = false,
                            modifier = Modifier.animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(140), placementSpec = tween(220))
                        )
                    }
                }
            }
        }
    }
    if (pickerOpen) {
        val zone = ZoneId.of(state.zoneId)
        ReminderDateDialog(state.selectedDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            onDismiss = { pickerOpen = false }) {
            onSelectDate(Instant.ofEpochMilli(it).atZone(zone).toLocalDate())
            pickerOpen = false
        }
    }
}
