package com.FreeRave.remindme.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.service.GeofenceService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.model.QuickFilter
import com.FreeRave.remindme.utils.NotificationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application, // Inject Application context
    private val reminderService: ReminderService,
    private val geofenceService: GeofenceService,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {

    private val _unfilteredReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val allReminders: StateFlow<List<Reminder>> = _unfilteredReminders.asStateFlow()
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedQuickFilter = MutableStateFlow(QuickFilter.ALL)
    val selectedQuickFilter: StateFlow<QuickFilter> = _selectedQuickFilter

    // This was the version with _isRefreshing for swipe-to-refresh
    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _hasError = MutableStateFlow(false)
    val hasError = _hasError.asStateFlow()
    private var remindersJob: Job? = null

    private val _showGpsEnableDialog = MutableStateFlow(false)
    val showGpsEnableDialog: StateFlow<Boolean> = _showGpsEnableDialog

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private var geofencesRegistered = false

    init {
        viewModelScope.launch {
            authService.currentUserIdFlow().distinctUntilChanged().collectLatest { userId ->
                remindersJob?.cancel()
                _unfilteredReminders.value = emptyList()
                _reminders.value = emptyList()
                _hasError.value = false
                geofencesRegistered = false
                if (userId.isNotBlank()) {
                    _isRefreshing.value = true
                    try {
                        authService.refreshCurrentUser()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        // Ignore reload error if offline
                    }
                    _userName.value = authService.getUserDisplayName()
                    loadRemindersForUser(userId)
                } else {
                    _userName.value = null
                    _unfilteredReminders.value = emptyList()
                    _reminders.value = emptyList()
                    _isRefreshing.value = false
                }
            }
        }
    }

    private fun loadRemindersForUser(userId: String) {
        remindersJob?.cancel()
        remindersJob = viewModelScope.launch {
            _isRefreshing.value = true
            _hasError.value = false
            Log.d("HomeViewModel", "Starting to collect reminders for userId: $userId")
            try {
                reminderService.getReminders(userId).collectLatest { newReminders ->
                    if (authService.getCurrentUserId() != userId) return@collectLatest
                    Log.d("HomeViewModel", "Firestore emitted new reminders. Count: ${newReminders.size}.")
                    _unfilteredReminders.value = newReminders
                    _reminders.value = applyFilters(newReminders)
                    _isRefreshing.value = false
                    try {
                        withContext(Dispatchers.IO) {
                            NotificationUtils.reconcileNotifications(application.applicationContext, newReminders, userId)
                        }
                    } catch (error: CancellationException) { throw error }
                    catch (_: Exception) { /* The list remains usable when local notification storage is unavailable. */ }

                    if (!geofencesRegistered) {
                        registerGeofences(newReminders)
                        geofencesRegistered = true
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e("HomeViewModel", "Error collecting reminders for user: $userId", e)
                _isRefreshing.value = false
                _hasError.value = true
            }
        }
    }
    
    // Added for swipe to refresh
    fun refreshReminders() {
        Log.d("HomeViewModel", "refreshReminders called")
        val userId = authService.getCurrentUserId()
        if (userId.isNotBlank()) {
            loadRemindersForUser(userId)
        }
    }

    fun searchReminders(query: String) {
        _searchQuery.value = query
        _reminders.value = applyFilters(_unfilteredReminders.value)
    }

    private fun applyFilters(reminders: List<Reminder>): List<Reminder> = filterHomeReminders(
        reminders = reminders,
        query = _searchQuery.value,
        category = _selectedCategory.value,
        quickFilter = _selectedQuickFilter.value
    )

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _reminders.value = applyFilters(_unfilteredReminders.value)
    }

    fun selectQuickFilter(filter: QuickFilter) {
        _selectedQuickFilter.value = filter
        _reminders.value = applyFilters(_unfilteredReminders.value)
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
