package com.FreeRave.remindme.screens.add

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.GeminiService 
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.ui.reminder.DAY_MILLIS
import com.FreeRave.remindme.utils.LocationHistoryEntry
import com.FreeRave.remindme.utils.NLPUtils
import com.FreeRave.remindme.utils.NotificationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject 

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val application: Application,
    private val reminderService: ReminderService,
    private val authService: FirebaseAuthServiceInterface,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

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

    internal fun applyAiDraft(draft: AiReminderDraft) {
        onNlpInputChanged(draft.sourceText)
        onTitleChanged(draft.title)
        onDescriptionChanged("")
        onCategoryChanged(draft.category)
        onReminderTimeChanged(draft.reminderTime)
        onIsRecurringChanged(draft.isRecurring)
        onRecurrenceIntervalChanged(if (draft.isRecurring) draft.recurrenceIntervalMillis else null)
        onRecurrenceEndTimeChanged(if (draft.isRecurring) draft.recurrenceEndTime else null)
        onIsLocationBasedChanged(false)
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

    fun onIsRecurringChanged(isRec: Boolean) {
        _isRecurring.update { isRec }
        if (!isRec) {
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
        if (inputText.isBlank() || _isParsing.value) {
            return
        }

        _isParsing.value = true
        viewModelScope.launch {
            try {
                // Attempt to connect to Gemini 2.5 Flash
                val jsonResponse = geminiService.parseReminderToJSON(inputText)

                if (jsonResponse != null) {
                    val cleanJson = jsonResponse
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()
                    val jsonObject = JSONObject(cleanJson)

                    _title.update { jsonObject.optString("title", inputText) }

                    if (jsonObject.has("reminderTime") && !jsonObject.isNull("reminderTime")) {
                        _reminderTimeMillis.update { jsonObject.getLong("reminderTime") }
                    }

                    val recurring = jsonObject.optBoolean("isRecurring", false)
                    _isRecurring.value = recurring
                    _recurrenceIntervalMillis.value = if (recurring) {
                        jsonObject.optLong("recurrenceIntervalMillis", DAY_MILLIS).takeIf { it > 0 } ?: DAY_MILLIS
                    } else null
                    _recurrenceEndTime.value = null

                    val detectedCategory = jsonObject.optString("category", "")
                    if (detectedCategory.isNotBlank() && detectedCategory != "null") {
                        _category.update { detectedCategory }
                    }
                } else {
                    // Fallback: If AI returns null (e.g., network issue), use the old NLP
                    fallbackToOfflineNLP(inputText)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("AddReminderViewModel", "AI Parsing failed, falling back to NLPUtils", e)
                fallbackToOfflineNLP(inputText)
            } finally {
                _isParsing.value = false
            }

            // If the event is recurring but no start time is set
            if (_isRecurring.value && _reminderTimeMillis.value == null) {
                val defaultStartTime = System.currentTimeMillis() + (_recurrenceIntervalMillis.value ?: 60000L)
                _reminderTimeMillis.update { defaultStartTime }
            }
        }
    }

    // Helper function to avoid duplicating fallback code
    private fun fallbackToOfflineNLP(inputText: String) {
        val parsedInfo = NLPUtils.parseReminderText(inputText)
        _title.update { parsedInfo.title }
        _reminderTimeMillis.update { parsedInfo.reminderTime }
        _isRecurring.update { parsedInfo.isRecurring }
        _recurrenceIntervalMillis.update { parsedInfo.recurrenceIntervalMillis }
        _recurrenceEndTime.update { parsedInfo.recurrenceEndTime }
    }

    fun addReminder(
        customId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (_isSaving.value) return
        val currentTitle = _title.value
        val currentReminderTime = _reminderTimeMillis.value

        if (currentTitle.isBlank()) {
            onComplete(false, null)
            return
        }
        if (currentReminderTime == null || currentReminderTime <= System.currentTimeMillis() || authService.getCurrentUserId().isBlank()) {
            onComplete(false, null)
            return
        }

        if (_isRecurring.value && _recurrenceIntervalMillis.value == null) {
            onComplete(false, null)
            return
        }


        _isSaving.value = true
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
                    val snoozeMinutes = userPreferencesRepository.defaultSnoozeMinutes.first()
                    val soundResourceName = userPreferencesRepository.selectedNotificationSound.first()
                        ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME

                    NotificationUtils.scheduleNotification(
                        context = application.applicationContext,
                        reminder = savedReminder,
                        snoozeMinutes = snoozeMinutes,
                        soundResourceName = soundResourceName,
                        allowDueNow = true
                    )
                    onComplete(true, savedReminder.id)
                } else {
                    onComplete(false, null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onComplete(false, null)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
