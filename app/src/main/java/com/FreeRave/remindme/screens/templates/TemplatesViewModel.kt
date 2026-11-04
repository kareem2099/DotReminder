package com.FreeRave.remindme.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.TemplateService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateService: TemplateService,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {
    private val _uiState = MutableStateFlow(TemplatesUiState())
    internal val uiState = _uiState.asStateFlow()
    private val _effects = Channel<TemplateEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()
    private var activeUserId: String? = null
    private var loadJob: Job? = null
    private var writeJob: Job? = null
    private var externalSaveInProgress = false

    init {
        viewModelScope.launch {
            authService.currentUserIdFlow().distinctUntilChanged().collectLatest { loadForUser(it) }
        }
    }

    fun retry() = loadForUser(authService.getCurrentUserId())
    fun setQuery(value: String) { _uiState.update { it.copy(query = value) } }
    internal fun setFilter(value: TemplateFilter) { _uiState.update { it.copy(filter = value) } }
    fun resetFilters() { _uiState.update { it.copy(query = "", filter = TemplateFilter.ALL) } }

    private fun loadForUser(userId: String) {
        loadJob?.cancel()
        if (userId != activeUserId) {
            writeJob?.cancel()
            activeUserId = userId
            _uiState.value = TemplatesUiState(isAuthenticated = userId.isNotBlank())
        }
        if (userId.isBlank()) {
            _uiState.update { it.copy(templates = emptyList(), isAuthenticated = false, isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true, hasError = false, isAuthenticated = true) }
        loadJob = viewModelScope.launch {
            try {
                templateService.getTemplates(userId).collect { list ->
                    _uiState.update { it.copy(templates = orderTemplates(list), isLoading = false, hasError = false) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }

    internal fun openEditor(template: ReminderTemplate? = null) {
        if (_uiState.value.canInteract) _uiState.update { it.copy(editor = TemplateEditorState(template)) }
    }
    fun dismissEditor() { _uiState.update { if (it.editor?.isSaving == true) it else it.copy(editor = null) } }
    internal fun updateDraft(draft: TemplateDraft) {
        _uiState.update { state ->
            val editor = state.editor
            if (editor == null || editor.isSaving) state
            else state.copy(editor = editor.copy(draft = draft, errors = emptySet(), saveFailed = false))
        }
    }

    fun saveEditor() {
        val state = _uiState.value
        val editor = state.editor ?: return
        if (!state.canInteract || !editor.draft.hasChanges(editor.original)) return
        val errors = editor.draft.validate()
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(editor = editor.copy(errors = errors)) }
            return
        }
        val userId = authService.getCurrentUserId()
        if (userId.isBlank() || userId != activeUserId || (editor.original != null && editor.original.userId != userId)) return
        val template = editor.draft.toTemplate(editor.original, userId, System.currentTimeMillis())
        _uiState.update { it.copy(editor = editor.copy(isSaving = true, saveFailed = false)) }
        writeJob = viewModelScope.launch {
            try {
                authService.refreshCurrentUser()
                check(authService.getCurrentUserId() == userId)
                val saved = if (editor.original == null) templateService.addTemplate(template)
                    else if (templateService.updateTemplate(template)) template else null
                ensureActive()
                if (activeUserId != userId) return@launch
                if (saved == null) _uiState.update { it.copy(editor = it.editor?.copy(isSaving = false, saveFailed = true)) }
                else {
                    _uiState.update { it.copy(templates = orderTemplates(it.templates.filterNot { item -> item.id == saved.id } + saved),
                        editor = null, query = "", filter = TemplateFilter.ALL) }
                    _effects.send(if (editor.original == null) TemplateEffect.CREATED else TemplateEffect.UPDATED)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (activeUserId == userId) _uiState.update { it.copy(editor = it.editor?.copy(isSaving = false, saveFailed = true)) }
            }
        }
    }

    internal fun requestDelete(template: ReminderTemplate) {
        if (_uiState.value.canInteract && template.id.isNotBlank() && template.userId == authService.getCurrentUserId()) {
            _uiState.update { it.copy(deleteTarget = template, deleteFailed = false) }
        }
    }
    fun dismissDelete() { _uiState.update { if (it.isDeleting) it else it.copy(deleteTarget = null, deleteFailed = false) } }
    fun deleteTemplate() {
        val state = _uiState.value
        val template = state.deleteTarget ?: return
        val userId = authService.getCurrentUserId()
        if (!state.canInteract || userId.isBlank() || userId != activeUserId || template.userId != userId) return
        _uiState.update { it.copy(isDeleting = true, deleteFailed = false) }
        writeJob = viewModelScope.launch {
            try {
                authService.refreshCurrentUser()
                check(authService.getCurrentUserId() == userId)
                val success = templateService.deleteTemplate(template.id)
                ensureActive()
                if (activeUserId != userId) return@launch
                if (success) {
                    _uiState.update { it.copy(templates = it.templates.filterNot { item -> item.id == template.id }, deleteTarget = null, isDeleting = false) }
                    _effects.send(TemplateEffect.DELETED)
                } else _uiState.update { it.copy(isDeleting = false, deleteFailed = true) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (activeUserId == userId) _uiState.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }

    /** Kept for saving the current reminder as a template from the add screen. */
    fun addTemplate(template: ReminderTemplate, onComplete: (Boolean) -> Unit) {
        if (externalSaveInProgress) return
        val userId = authService.getCurrentUserId()
        if (userId.isBlank() || template.toDraft().validate().isNotEmpty()) { onComplete(false); return }
        externalSaveInProgress = true
        viewModelScope.launch {
            val success = try {
                authService.refreshCurrentUser()
                if (authService.getCurrentUserId() != userId) false else {
                    val now = System.currentTimeMillis()
                    templateService.addTemplate(template.toDraft().toTemplate(null, userId, now))
                    true
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { false }
            finally { externalSaveInProgress = false }
            onComplete(success)
        }
    }

    fun createTemplateFromReminder(
        reminderTitle: String, reminderDescription: String, reminderCategory: String, isRecurring: Boolean,
        recurrenceIntervalMillis: Long?, isLocationBased: Boolean, templateName: String
    ) = ReminderTemplate(userId = authService.getCurrentUserId(), name = templateName, title = reminderTitle,
        description = reminderDescription, category = reminderCategory, isRecurring = isRecurring,
        recurrenceIntervalMillis = recurrenceIntervalMillis, isLocationBased = isLocationBased)
}
