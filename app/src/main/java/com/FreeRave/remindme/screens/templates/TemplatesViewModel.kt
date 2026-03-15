package com.FreeRave.remindme.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.TemplateService
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateService: TemplateService,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {

    private val _templates = MutableStateFlow<List<ReminderTemplate>>(emptyList())
    val templates: StateFlow<List<ReminderTemplate>> = _templates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = authService.getCurrentUserId()
            _isAuthenticated.value = userId.isNotBlank()
            if (userId.isNotBlank()) {
                templateService.getTemplates(userId).collectLatest { templates ->
                    _templates.value = templates
                    _isLoading.value = false
                }
            } else {
                _templates.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun addTemplate(template: ReminderTemplate, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Refresh authentication token to ensure validity
                authService.refreshCurrentUser()
                val userId = authService.getCurrentUserId()
                android.util.Log.d("TemplatesViewModel", "Adding template with userId: $userId")
                if (userId.isNotBlank()) {
                    val templateWithUserId = template.copy(userId = userId)
                    templateService.addTemplate(templateWithUserId)
                    onComplete(true)
                } else {
                    android.util.Log.w("TemplatesViewModel", "Cannot add template: user not authenticated")
                    onComplete(false)
                }
            } catch (e: FirebaseFirestoreException) {
                if (e.code == Code.PERMISSION_DENIED) {
                    android.util.Log.e("TemplatesViewModel", "Permission denied adding template - user may need to re-authenticate", e)
                } else {
                    android.util.Log.e("TemplatesViewModel", "Firestore error adding template: ${e.code}", e)
                }
                onComplete(false)
            } catch (e: Exception) {
                android.util.Log.e("TemplatesViewModel", "Error adding template", e)
                onComplete(false)
            }
        }
    }

    fun updateTemplate(template: ReminderTemplate, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Refresh authentication token to ensure validity
                authService.refreshCurrentUser()
                val success = templateService.updateTemplate(template)
                onComplete(success)
            } catch (e: FirebaseFirestoreException) {
                if (e.code == Code.PERMISSION_DENIED) {
                    android.util.Log.e("TemplatesViewModel", "Permission denied updating template - user may need to re-authenticate", e)
                } else {
                    android.util.Log.e("TemplatesViewModel", "Firestore error updating template: ${e.code}", e)
                }
                onComplete(false)
            } catch (e: Exception) {
                android.util.Log.e("TemplatesViewModel", "Error updating template", e)
                onComplete(false)
            }
        }
    }

    fun deleteTemplate(templateId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Refresh authentication token to ensure validity
                authService.refreshCurrentUser()
                val success = templateService.deleteTemplate(templateId)
                onComplete(success)
            } catch (e: FirebaseFirestoreException) {
                if (e.code == Code.PERMISSION_DENIED) {
                    android.util.Log.e("TemplatesViewModel", "Permission denied deleting template - user may need to re-authenticate", e)
                } else {
                    android.util.Log.e("TemplatesViewModel", "Firestore error deleting template: ${e.code}", e)
                }
                onComplete(false)
            } catch (e: Exception) {
                android.util.Log.e("TemplatesViewModel", "Error deleting template", e)
                onComplete(false)
            }
        }
    }

    fun createTemplateFromReminder(
        reminderTitle: String,
        reminderDescription: String,
        reminderCategory: String,
        isRecurring: Boolean,
        recurrenceIntervalMillis: Long?,
        isLocationBased: Boolean,
        templateName: String
    ): ReminderTemplate {
        return ReminderTemplate(
            userId = authService.getCurrentUserId(),
            name = templateName,
            title = reminderTitle,
            description = reminderDescription,
            category = reminderCategory,
            isRecurring = isRecurring,
            recurrenceIntervalMillis = recurrenceIntervalMillis,
            isLocationBased = isLocationBased
        )
    }
}
