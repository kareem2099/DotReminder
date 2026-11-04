package com.FreeRave.remindme.screens.edit

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent
import com.FreeRave.remindme.ui.reminder.applyTo
import com.FreeRave.remindme.ui.reminder.toDraft
import com.FreeRave.remindme.ui.reminder.update
import com.FreeRave.remindme.ui.reminder.validate
import com.FreeRave.remindme.utils.NotificationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditReminderViewModel @Inject constructor(
    private val application: Application,
    private val reminderService: ReminderService,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditReminderUiState())
    internal val uiState = _uiState.asStateFlow()
    private val _effects = Channel<EditReminderEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()
    private var loadJob: Job? = null

    fun loadReminder(id: String) {
        if (_uiState.value.original?.id == id) return
        loadJob?.cancel()
        _uiState.value = EditReminderUiState()
        if (id.isBlank()) {
            _uiState.value = EditReminderUiState(isLoading = false)
            return
        }
        loadJob = viewModelScope.launch {
            try {
                val reminder = reminderService.getReminder(id)
                ensureActive()
                _uiState.value = EditReminderUiState(
                    original = reminder, draft = reminder?.toDraft() ?: ReminderDraft(),
                    isLoading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = EditReminderUiState(isLoading = false)
            }
        }
    }

    internal fun updateDraft(event: ReminderFormEvent) {
        _uiState.update { state ->
            if (state.isLoading || state.isSaving || state.original == null) state
            else state.copy(draft = state.draft.update(event))
        }
    }

    fun saveReminder() {
        val state = _uiState.value
        val original = state.original ?: return
        if (state.isSaving || state.isLoading || !state.hasChanges) return
        val now = System.currentTimeMillis()
        _uiState.update { it.copy(validationRequest = it.validationRequest + 1) }
        if (state.draft.validate(now, original.reminderTime).isNotEmpty()) {
            _effects.trySend(EditReminderEffect.VALIDATION_FAILED)
            return
        }
        val updated = state.draft.applyTo(original, now)
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val success = reminderService.updateReminder(updated)
                ensureActive()
                if (!success) {
                    _effects.send(EditReminderEffect.SAVE_FAILED)
                    return@launch
                }
                if (original.reminderTime != updated.reminderTime) {
                    NotificationUtils.cancelNotification(application.applicationContext, original.id)
                }
                val snoozeMinutes = userPreferencesRepository.defaultSnoozeMinutes.first()
                val sound = userPreferencesRepository.selectedNotificationSound.first()
                    ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
                NotificationUtils.scheduleNotification(application.applicationContext, updated, snoozeMinutes, sound)
                _uiState.update { it.copy(original = updated, draft = updated.toDraft()) }
                _effects.send(EditReminderEffect.SAVED)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _effects.send(EditReminderEffect.SAVE_FAILED)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
