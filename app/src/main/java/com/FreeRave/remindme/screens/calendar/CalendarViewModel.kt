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
import java.util.GregorianCalendar

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reminderService: ReminderService,
    private val firebaseAuthService: FirebaseAuthServiceInterface
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    private val _selectedDateReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val selectedDateReminders: StateFlow<List<Reminder>> = _selectedDateReminders

    private val _selectedDateInMillis = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val selectedDateInMillis: StateFlow<Long> = _selectedDateInMillis

    init {
        loadReminders()
    }

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
        val startOfDay = selectedMillis
        val endOfDay = selectedMillis + (24 * 60 * 60 * 1000) - 1 // End of day

        val daysReminders = _reminders.value.filter { reminder ->
            reminder.reminderTime in startOfDay..endOfDay ||
            (reminder.isRecurring && isRecurringOnDate(reminder, selectedMillis))
        }
        _selectedDateReminders.value = daysReminders
    }

    private fun isRecurringOnDate(reminder: Reminder, dateMillis: Long): Boolean {
        if (reminder.recurrenceIntervalMillis == null || reminder.recurrenceIntervalMillis <= 0) return false
        val reminderTime = reminder.reminderTime

        if (dateMillis < reminderTime) return false

        val diff = dateMillis - reminderTime
        return (diff % reminder.recurrenceIntervalMillis) == 0L
    }

    fun hasRemindersOnDate(year: Int, month: Int, day: Int): Boolean {
        val cal = GregorianCalendar(year, month, day, 0, 0, 0).apply { set(Calendar.MILLISECOND, 0) }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1

        return _reminders.value.any { reminder ->
            val reminderTime = reminder.reminderTime
            (reminderTime in startOfDay..endOfDay) ||
            (reminder.isRecurring && isRecurringOnDate(reminder, startOfDay))
        }
    }

    fun getSelectedDateCalendar(): Calendar {
        return Calendar.getInstance().apply { timeInMillis = _selectedDateInMillis.value }
    }
}
