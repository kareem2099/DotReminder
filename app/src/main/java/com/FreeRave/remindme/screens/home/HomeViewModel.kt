package com.FreeRave.remindme.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.service.GeofenceService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.ui.components.QuickFilter
import com.FreeRave.remindme.utils.NotificationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application, // Inject Application context
    private val reminderService: ReminderService,
    private val geofenceService: GeofenceService,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {

    private val _unfilteredReminders = MutableStateFlow<List<Reminder>>(emptyList())
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedQuickFilter = MutableStateFlow(QuickFilter.ALL)
    val selectedQuickFilter: StateFlow<QuickFilter> = _selectedQuickFilter

    // This was the version with _isRefreshing for swipe-to-refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showGpsEnableDialog = MutableStateFlow(false)
    val showGpsEnableDialog: StateFlow<Boolean> = _showGpsEnableDialog

    private var geofencesRegistered = false

    init {
        loadReminders()
    }

    private fun loadReminders() {
        viewModelScope.launch {
            _isRefreshing.value = true // Set refreshing true
            val userId = authService.getCurrentUserId()
            if (userId.isNotBlank()) {
                Log.d("HomeViewModel", "Starting to collect reminders for userId: $userId")
                try {
                    reminderService.getReminders(userId).collectLatest { newReminders ->
                        Log.d("HomeViewModel", "Firestore emitted new reminders. Count: ${newReminders.size}. First item ID if any: ${newReminders.firstOrNull()?.id}")
                        _unfilteredReminders.value = newReminders
                        _reminders.value = applyFilters(newReminders)
                        _isRefreshing.value = false // Set refreshing false

                        // Register geofences for location-based reminders on initial app load
                        if (!geofencesRegistered) {
                            registerGeofences(newReminders)
                            geofencesRegistered = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error collecting reminders", e)
                    _isRefreshing.value = false // Set refreshing false on error
                }
            } else {
                _reminders.value = emptyList()
                _isRefreshing.value = false // Set refreshing false if no user
                Log.w("HomeViewModel", "User ID is blank in loadReminders. Reminders list cleared.")
            }
        }
    }
    
    // Added for swipe to refresh
    fun refreshReminders() {
        Log.d("HomeViewModel", "refreshReminders called")
        loadReminders()
    }

    fun searchReminders(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val userId = authService.getCurrentUserId() 
            if (userId.isNotBlank()) {
                if (query.isBlank()) {
                    Log.d("HomeViewModel", "Search query is blank, calling refreshReminders to reload all.")
                    refreshReminders() // Call refresh which handles _isRefreshing state
                } else {
                    _isRefreshing.value = true // Set refreshing true for search
                    Log.d("HomeViewModel", "Searching reminders for userId: $userId, query: $query")
                    try {
                        reminderService.searchReminders(userId, query).collectLatest { searchedReminders ->
                            Log.d("HomeViewModel", "Search returned. Count: ${searchedReminders.size}")
                            _reminders.value = applyFilters(searchedReminders)
                            _isRefreshing.value = false // Set refreshing false
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error searching reminders", e)
                        _isRefreshing.value = false // Set refreshing false on error
                    }
                }
            } else {
                _reminders.value = emptyList()
                _isRefreshing.value = false // Set refreshing false if no user
                Log.w("HomeViewModel", "User ID is blank in searchReminders. Reminders list cleared.")
            }
        }
    }

    private fun applyFilters(allReminders: List<Reminder>): List<Reminder> {
        var filtered = allReminders

        // Apply quick filter first
        val quickFilter = _selectedQuickFilter.value
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        filtered = when (quickFilter) {
            QuickFilter.ALL -> filtered
            QuickFilter.TODAY -> {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val tomorrow = Calendar.getInstance().apply {
                    time = today.time
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                filtered.filter { reminder ->
                    reminder.reminderTime >= today.timeInMillis && reminder.reminderTime < tomorrow.timeInMillis
                }
            }
            QuickFilter.THIS_WEEK -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val weekEnd = Calendar.getInstance().apply {
                    time = weekStart.time
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
                filtered.filter { reminder ->
                    reminder.reminderTime >= weekStart.timeInMillis && reminder.reminderTime < weekEnd.timeInMillis
                }
            }
            QuickFilter.OVERDUE -> {
                filtered.filter { reminder -> reminder.reminderTime < currentTime }
            }
            QuickFilter.COMPLETED -> {
                // For completed, we consider reminders that have passed their time (since we don't have a completion field)
                filtered.filter { reminder -> reminder.reminderTime < currentTime }
            }
        }

        // Apply search query
        val query = _searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }

        // Apply category filter
        val category = _selectedCategory.value
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }

        return filtered
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        // If no search query, reapply filters to unfiltered reminders
        // If there is search, the reminders are already filtered by search service, so apply category to them
        val baseReminders = if (_searchQuery.value.isBlank()) _unfilteredReminders.value else _reminders.value.map { it } // copy to avoid mutation
        _reminders.value = applyFilters(baseReminders)
    }

    fun selectQuickFilter(filter: QuickFilter) {
        _selectedQuickFilter.value = filter
        // Reapply all filters with the new quick filter
        val baseReminders = if (_searchQuery.value.isBlank()) _unfilteredReminders.value else _reminders.value.map { it } // copy to avoid mutation
        _reminders.value = applyFilters(baseReminders)
    }

    fun deleteReminder(reminderId: String) {
        Log.d("HomeViewModel", "Attempting to delete reminder: $reminderId. Current list size: ${_reminders.value.size}")
        val currentList = _reminders.value
        val newList = currentList.filter { it.id != reminderId }

        if (currentList.size != newList.size) {
            _reminders.value = newList
            Log.d("HomeViewModel", "Optimistically updated reminders list. New size: ${newList.size}. Item $reminderId removed locally.")
        } else {
            Log.w("HomeViewModel", "Reminder ID $reminderId not found in current list for optimistic update. List size: ${currentList.size}")
        }

        viewModelScope.launch {
            try {
                reminderService.deleteReminder(reminderId)
                NotificationUtils.cancelNotification(application.applicationContext, reminderId)
                Log.d("HomeViewModel", "Successfully deleted reminder $reminderId from service and cancelled notification.")
                // Firestore listener should update the list from the source of truth.
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting reminder $reminderId from service or cancelling notification", e)
                // If service deletion failed, Firestore listener should eventually correct the state.
                // Consider reverting: _reminders.value = currentList 
            }
        }
    }

    private fun registerGeofences(reminders: List<Reminder>) {
        val locationReminders = reminders.filter { reminder ->
            reminder.isLocationBased && reminder.latitude != null && reminder.longitude != null
        }
        if (locationReminders.isNotEmpty()) {
            val gpsStatus = geofenceService.checkGpsStatus()
            when (gpsStatus) {
                GeofenceService.GpsStatus.ENABLED -> {
                    Log.d("HomeViewModel", "Re-registering ${locationReminders.size} location-based geofences on app restart")
                    viewModelScope.launch {
                        geofenceService.registerGeofences(locationReminders)
                    }
                }
                GeofenceService.GpsStatus.DISABLED -> {
                    Log.w("HomeViewModel", "GPS disabled, showing enable dialog")
                    _showGpsEnableDialog.value = true
                }
                GeofenceService.GpsStatus.NO_PERMISSION -> {
                    Log.w("HomeViewModel", "Location permission not granted, cannot register geofences")
                }
            }
        }
    }

    fun dismissGpsDialog() {
        _showGpsEnableDialog.value = false
    }

    fun enableGps() {
        com.FreeRave.remindme.utils.LocationUtils.openLocationSettings(application.applicationContext)
        _showGpsEnableDialog.value = false
    }
}
