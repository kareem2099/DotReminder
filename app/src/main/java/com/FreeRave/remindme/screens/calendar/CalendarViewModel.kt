package com.FreeRave.remindme.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reminderService: ReminderService,
    private val firebaseAuthService: FirebaseAuthServiceInterface
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    private val _selectedDateReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val selectedDateReminders: StateFlow<List<Reminder>> = _selectedDateReminders

    private val _selectedDateInMillis = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDateInMillis: StateFlow<Long> = _selectedDateInMillis

    init { loadReminders() }

    fun loadReminders() {
        viewModelScope.launch {
            val userId = firebaseAuthService.getCurrentUserId()
            if (userId.isNotEmpty()) {
                reminderService.getReminders(userId).collectLatest { reminderList ->
                    _reminders.value = reminderList
                    updateSelectedDateReminders()
                }
            }
        }
    }

    fun selectDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _selectedDateInMillis.value = cal.timeInMillis
        updateSelectedDateReminders()
    }

    private fun updateSelectedDateReminders() {
        val selectedMillis = _selectedDateInMillis.value
        val endOfDay = selectedMillis + (24 * 60 * 60 * 1000) - 1

        _selectedDateReminders.value = _reminders.value
            .filter { reminder ->
                val isExactDay = reminder.reminderTime in selectedMillis..endOfDay
                isExactDay || (reminder.isRecurring && isRecurringOnDate(reminder, selectedMillis))
            }
            .sortedBy { it.reminderTime }
    }

    private fun isRecurringOnDate(reminder: Reminder, targetDateMillis: Long): Boolean {
        val interval = reminder.recurrenceIntervalMillis ?: return false
        if (interval <= 0) return false

        val reminderStartOfDay = getStartOfDay(reminder.reminderTime)
        val targetStartOfDay = getStartOfDay(targetDateMillis)

        if (targetStartOfDay < reminderStartOfDay) return false

        val diff = targetStartOfDay - reminderStartOfDay
        return (diff % interval) == 0L
    }

    fun hasRemindersOnDate(year: Int, month: Int, day: Int): Boolean {
        if (_reminders.value.isEmpty()) return false // ✅ fast exit

        val startOfDay = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1

        return _reminders.value.any { reminder ->
            val isExactDay = reminder.reminderTime in startOfDay..endOfDay
            isExactDay || (reminder.isRecurring && isRecurringOnDate(reminder, startOfDay))
        }
    }

    private fun getStartOfDay(timeInMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}