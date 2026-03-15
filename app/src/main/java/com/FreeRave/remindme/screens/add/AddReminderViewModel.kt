package com.FreeRave.remindme.screens.add

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.utils.NotificationUtils
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.utils.NLPUtils
import com.FreeRave.remindme.utils.ParsedReminderInfo
import com.FreeRave.remindme.utils.LocationHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val application: Application,
    private val reminderService: ReminderService,
    private val authService: FirebaseAuthServiceInterface,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _nlpInputText = MutableStateFlow("")
    val nlpInputText: StateFlow<String> = _nlpInputText.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _reminderTimeMillis = MutableStateFlow<Long?>(null)
    val reminderTimeMillis: StateFlow<Long?> = _reminderTimeMillis.asStateFlow()

    // Recurrence states
    private val _isRecurring = MutableStateFlow(false)
    val isRecurring: StateFlow<Boolean> = _isRecurring.asStateFlow()

    private val _recurrenceIntervalMillis = MutableStateFlow<Long?>(null)
    val recurrenceIntervalMillis: StateFlow<Long?> = _recurrenceIntervalMillis.asStateFlow()

    private val _recurrenceEndTime = MutableStateFlow<Long?>(null)
    val recurrenceEndTime: StateFlow<Long?> = _recurrenceEndTime.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    // Location-based states
    private val _isLocationBased = MutableStateFlow(false)
    val isLocationBased: StateFlow<Boolean> = _isLocationBased.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude.asStateFlow()

    private val _geofenceRadius = MutableStateFlow(200f)
    val geofenceRadius: StateFlow<Float> = _geofenceRadius.asStateFlow()

    private val _geofenceTransitionType = MutableStateFlow(0) // Default to none
    val geofenceTransitionType: StateFlow<Int> = _geofenceTransitionType.asStateFlow()

    private val _geofenceDwellTimeMillis = MutableStateFlow<Long?>(null)
    val geofenceDwellTimeMillis: StateFlow<Long?> = _geofenceDwellTimeMillis.asStateFlow()

    private val _geofencePriority = MutableStateFlow(1)
    val geofencePriority: StateFlow<Int> = _geofencePriority.asStateFlow()


    fun onNlpInputChanged(text: String) {
        _nlpInputText.update { text }
    }

    fun onTitleChanged(text: String) {
        _title.update { text }
    }

    fun onDescriptionChanged(text: String) {
        _description.update { text }
    }

    fun onReminderTimeChanged(millis: Long?) {
        _reminderTimeMillis.update { millis }
    }

    // Functions to manually update recurrence if UI controls are added later
    fun onIsRecurringChanged(isRec: Boolean) {
        _isRecurring.update { isRec }
        if (!isRec) { // If not recurring, clear interval and end time
            _recurrenceIntervalMillis.update { null }
            _recurrenceEndTime.update { null }
        }
    }

    fun onRecurrenceIntervalChanged(interval: Long?) {
        _recurrenceIntervalMillis.update { interval }
    }

    fun onRecurrenceEndTimeChanged(endTime: Long?) {
        _recurrenceEndTime.update { endTime }
    }

    fun onCategoryChanged(category: String) {
        _category.update { category }
    }

    fun onIsLocationBasedChanged(isLocationBased: Boolean) {
        _isLocationBased.update { isLocationBased }
        if (!isLocationBased) {
            _latitude.update { null }
            _longitude.update { null }
            _geofenceRadius.update { 200f }
            _geofenceTransitionType.update { 0 }
        }
    }

    fun onLocationChanged(latitude: Double?, longitude: Double?, radius: Float) {
        _latitude.update { latitude }
        _longitude.update { longitude }
        _geofenceRadius.update { radius }
    }

    fun onGeofenceTransitionTypeChanged(transitionType: Int) {
        _geofenceTransitionType.update { transitionType }
    }

    fun onGeofenceDwellTimeChanged(dwellTimeMillis: Long?) {
        _geofenceDwellTimeMillis.update { dwellTimeMillis }
    }

    fun onGeofencePriorityChanged(priority: Int) {
        _geofencePriority.update { priority }
    }

    // Location history methods
    suspend fun getLocationHistory(): List<LocationHistoryEntry> {
        return try {
            val locationUtils = com.FreeRave.remindme.utils.LocationUtils(application.applicationContext)
            locationUtils.getLocationHistory()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCurrentLocationToHistory() {
        val lat = _latitude.value
        val lng = _longitude.value
        if (lat != null && lng != null) {
            try {
                val locationUtils = com.FreeRave.remindme.utils.LocationUtils(application.applicationContext)
                locationUtils.addLocationToHistory(lat, lng, null, _title.value.takeIf { it.isNotBlank() })
            } catch (e: Exception) {
                // Silently fail for location history
            }
        }
    }


    fun parseNaturalLanguageInput() {
        val inputText = _nlpInputText.value
        if (inputText.isBlank()) {
            return
        }
        viewModelScope.launch {
            val parsedInfo: ParsedReminderInfo = NLPUtils.parseReminderText(inputText)
            _title.update { parsedInfo.title }
            _reminderTimeMillis.update { parsedInfo.reminderTime }
            _isRecurring.update { parsedInfo.isRecurring }
            _recurrenceIntervalMillis.update { parsedInfo.recurrenceIntervalMillis }
            _recurrenceEndTime.update { parsedInfo.recurrenceEndTime }

            // If it's recurring but no specific start time was parsed,
            // and current reminderTimeMillis is null, set a default start time (e.g., now or soon).
            if (parsedInfo.isRecurring && parsedInfo.reminderTime == null && _reminderTimeMillis.value == null) {
                val defaultStartTime = System.currentTimeMillis() + (parsedInfo.recurrenceIntervalMillis ?: 60000L) // Default to interval or 1 min from now
                 _reminderTimeMillis.update { defaultStartTime }
            }
        }
    }

    fun addReminder(
        customId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentTitle = _title.value
        val currentReminderTime = _reminderTimeMillis.value

        if (currentTitle.isBlank()) {
            onComplete(false, null)
            return
        }
        if (currentReminderTime == null) {
            onComplete(false, null)
            return
        }
        // If recurring, interval must be set
        if (_isRecurring.value && _recurrenceIntervalMillis.value == null) {
            // Or handle this by setting a default interval, or showing an error
            onComplete(false, null) // Cannot save recurring reminder without interval
            return
        }


        viewModelScope.launch {
            try {
                val reminder = Reminder(
                    id = "",
                    customId = customId,
                    title = currentTitle,
                    description = _description.value,
                    time = System.currentTimeMillis(),
                    reminderTime = currentReminderTime,
                    userId = authService.getCurrentUserId(),
                    category = _category.value,
                    isRecurring = _isRecurring.value,
                    recurrenceIntervalMillis = _recurrenceIntervalMillis.value,
                    recurrenceEndTime = _recurrenceEndTime.value,
                    isLocationBased = _isLocationBased.value,
                    latitude = _latitude.value,
                    longitude = _longitude.value,
                    geofenceRadius = _geofenceRadius.value,
                    geofenceTransitionType = _geofenceTransitionType.value,
                    geofenceDwellTimeMillis = _geofenceDwellTimeMillis.value,
                    geofencePriority = _geofencePriority.value
                )
                val savedReminder = reminderService.addReminder(reminder)
                if (savedReminder.id.isNotBlank()) {
                    // Notification scheduling needs to be aware of recurrence
                    // This will be handled in a subsequent step. For now, it schedules the first one.
                    val snoozeMinutes = userPreferencesRepository.defaultSnoozeMinutes.first()
                    val soundResourceName = userPreferencesRepository.selectedNotificationSound.first()
                        ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME

                    NotificationUtils.scheduleNotification(
                        context = application.applicationContext,
                        reminder = savedReminder, // savedReminder now contains recurrence info
                        snoozeMinutes = snoozeMinutes,
                        soundResourceName = soundResourceName
                    )
                    onComplete(true, savedReminder.id)
                } else {
                    onComplete(false, null)
                }
            } catch (e: Exception) {
                onComplete(false, null)
            }
        }
    }
}
