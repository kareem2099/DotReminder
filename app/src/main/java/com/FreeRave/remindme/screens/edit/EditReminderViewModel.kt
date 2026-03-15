package com.FreeRave.remindme.screens.edit

import android.util.Log
import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.utils.NotificationUtils
import com.FreeRave.remindme.service.ReminderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditReminderViewModel @Inject constructor(
    private val application: Application, // Inject Application context
    private val reminderService: ReminderService,
    private val userPreferencesRepository: UserPreferencesRepository // Inject preferences
) : ViewModel() {
    private val _reminder = mutableStateOf<Reminder?>(null)
    private var originalReminderTime: Long? = null
    val reminder: State<Reminder?> get() = _reminder

    fun updateReminderTime(newTime: Long) = viewModelScope.launch {
        _reminder.value = _reminder.value?.copy(reminderTime = newTime)
    }

    fun loadReminder(id: String) {
        Log.d("EditReminderVM", "Loading reminder with id: $id")
        if (id.isBlank()) {
            Log.e("EditReminderVM", "Empty reminder ID provided!")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d("EditReminderVM", "Calling reminderService.getReminder")
                val loadedReminder = reminderService.getReminder(id)
                Log.d("EditReminderVM", "Service returned: ${loadedReminder?.toString()}")
                
                if (loadedReminder == null) {
                    Log.e("EditReminderVM", "No reminder found with ID: $id")
                }
                
                _reminder.value = loadedReminder
                originalReminderTime = loadedReminder?.reminderTime // Store original time
                Log.d("EditReminderVM", "ViewModel reminder updated to: ${_reminder.value?.toString()}")
            } catch (e: Exception) {
                Log.e("EditReminderVM", "Error loading reminder", e)
            }
        }
    }

    fun updateReminder(
        title: String,
        description: String,
        reminderTime: Long,
        category: String,
        onComplete: (Boolean) -> Unit
    ) {
        Log.d("EditReminderVM", "Starting updateReminder")
        viewModelScope.launch {
            try {
                val current = _reminder.value ?: run {
                    Log.e("EditReminderVM", "No current reminder to update")
                    onComplete(false)
                    return@launch
                }
                Log.d("EditReminderVM", "Updating reminder ${current.id} with new data")
                val updated = current.copy(
                    title = title,
                    description = description,
                    reminderTime = reminderTime,
                    category = category,
                    time = System.currentTimeMillis()
                )
                Log.d("EditReminderVM", "Calling reminderService.updateReminder")
                val success = reminderService.updateReminder(updated)
                Log.d("EditReminderVM", "Service call result: $success")
                if (success) {
                    _reminder.value = updated
                    // Cancel old notification if time changed
                    if (originalReminderTime != null && originalReminderTime != updated.reminderTime) {
                        NotificationUtils.cancelNotification(application.applicationContext, current.id)
                    }
                    // Fetch the current preferences
                    val snoozeMinutes = userPreferencesRepository.defaultSnoozeMinutes.first()
                    val soundResourceName = userPreferencesRepository.selectedNotificationSound.first()
                        ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
                    // Schedule new notification
                    NotificationUtils.scheduleNotification(
                        context = application.applicationContext,
                        reminder = updated,
                        snoozeMinutes = snoozeMinutes,
                        soundResourceName = soundResourceName // Pass sound name
                    )
                }
                onComplete(success)
            } catch (e: Exception) {
                Log.e("EditReminderVM", "Update failed", e)
                onComplete(false)
            }
        }
    }
}
