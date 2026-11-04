package com.FreeRave.remindme.screens.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.ReminderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reminderService: ReminderService,
    private val authService: FirebaseAuthServiceInterface,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val selectedDate = MutableStateFlow(
        runCatching { LocalDate.parse(savedState.get<String>("calendar_selected_date")) }.getOrNull() ?: LocalDate.now()
    )
    private val remindersData = MutableStateFlow(CalendarRemindersData())
    private val clock = MutableStateFlow(System.currentTimeMillis())
    private var remindersJob: Job? = null
    private var activeUserId: String? = null

    internal val uiState = combine(selectedDate, remindersData, clock) { date, data, now ->
        val zone = ZoneId.systemDefault()
        buildCalendarState(date, data.reminders, Instant.ofEpochMilli(now).atZone(zone).toLocalDate(), zone,
            WeekFields.of(Locale.getDefault()).firstDayOfWeek, data.isLoading, data.hasError)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildCalendarState(
        selectedDate.value, emptyList(), LocalDate.now(), ZoneId.systemDefault(),
        WeekFields.of(Locale.getDefault()).firstDayOfWeek, isLoading = true
    ))

    init {
        viewModelScope.launch {
            authService.currentUserIdFlow().distinctUntilChanged().collectLatest { loadForUser(it) }
        }
    }

    fun selectDate(date: LocalDate) {
        savedState["calendar_selected_date"] = date.toString()
        selectedDate.value = date
    }

    fun changeMonth(offset: Long) = selectDate(adjacentMonthDate(selectedDate.value, offset))

    fun goToToday() = selectDate(LocalDate.now())

    fun updateClock() { clock.value = System.currentTimeMillis() }

    fun retry() = loadForUser(authService.getCurrentUserId())

    private fun loadForUser(userId: String) {
        remindersJob?.cancel()
        if (userId.isBlank()) {
            activeUserId = null
            remindersData.value = CalendarRemindersData(isLoading = false)
            return
        }
        remindersData.value = if (activeUserId == userId) remindersData.value.copy(isLoading = true, hasError = false)
            else CalendarRemindersData()
        activeUserId = userId
        remindersJob = viewModelScope.launch {
            try {
                reminderService.getReminders(userId).collectLatest { reminders ->
                    remindersData.value = CalendarRemindersData(reminders = reminders, isLoading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                remindersData.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }
}

private data class CalendarRemindersData(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false
)
