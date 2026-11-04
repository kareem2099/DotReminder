package com.FreeRave.remindme.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.calendar.CalendarDay
import com.FreeRave.remindme.screens.calendar.CalendarUiState
import com.FreeRave.remindme.ui.components.GlassSurface
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun CalendarMonthCard(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (Long) -> Unit,
    onChooseDate: () -> Unit
) {
    val locale = Locale.getDefault()
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChangeMonth(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month))
                }
                TextButton(onClick = onChooseDate, modifier = Modifier.weight(1f)) {
                    Text(state.displayedMonth.format(monthFormatter), style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ExpandMore, stringResource(R.string.calendar_choose_date), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onChangeMonth(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val viewportWidth = maxWidth
            val cellWidth = (if (fontScale > 1.3f) 38f * fontScale else 40f).dp
            val gridWidth = maxOf(viewportWidth, cellWidth * 7)
            Column {
            if (gridWidth > viewportWidth) Text(stringResource(R.string.polish_calendar_scroll),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp))
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(gridWidth)) {
            Row(Modifier.fillMaxWidth()) {
                state.days.take(7).forEach { day ->
                    val fullName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                    Text(day.date.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = fullName },
                        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column {
                state.days.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day -> CalendarDayCell(day, Modifier.weight(1f)) { onSelectDate(day.date) } }
                    }
                }
            }
            }
            }
            }
            }
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            } else {
                Text(
                    stringResource(R.string.calendar_month_summary,
                        pluralStringResource(R.plurals.calendar_reminder_count, state.monthReminderCount, state.monthReminderCount),
                        pluralStringResource(R.plurals.calendar_planned_days, state.scheduledDayCount, state.scheduledDayCount)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: CalendarDay, modifier: Modifier, onClick: () -> Unit) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val dateLabel = if (day.isToday) stringResource(R.string.calendar_today_date, day.date.format(dateFormatter))
        else day.date.format(dateFormatter)
    val label = stringResource(R.string.calendar_day_accessibility, dateLabel,
        pluralStringResource(R.plurals.calendar_reminder_count, day.reminderCount, day.reminderCount))
    val primary = MaterialTheme.colorScheme.primary
    val fontScale = LocalDensity.current.fontScale
    val circleSize = maxOf(42.dp, (34f * fontScale).dp)
    val contentColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary
        !day.isInMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        day.isToday -> primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier.height(maxOf(52.dp, circleSize + 10.dp)).clip(RoundedCornerShape(14.dp))
            .selectable(day.isSelected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(circleSize)
                .background(if (day.isSelected) primary else Color.Transparent, CircleShape)
                .then(if (day.isToday && !day.isSelected) Modifier.border(1.dp, primary.copy(alpha = 0.7f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(numbers.format(day.date.dayOfMonth), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isSelected || day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor, modifier = Modifier.clearAndSetSemantics {})
            if (day.reminderCount > 0) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp)
                    .background(if (day.isSelected) contentColor else primary.copy(alpha = if (day.isInMonth) 1f else 0.4f), CircleShape))
            }
        }
    }
}
