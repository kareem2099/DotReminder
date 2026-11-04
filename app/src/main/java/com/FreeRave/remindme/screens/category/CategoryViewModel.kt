package com.FreeRave.remindme.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.service.CategoryService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryService: CategoryService,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    internal val uiState = _uiState.asStateFlow()
    val allCategories = _uiState.map { it.categories }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), builtInCategories)
    private val _effects = Channel<CategoryEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()
    private var customCategories: List<Category> = emptyList()
    private var activeUserId: String? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            authService.currentUserIdFlow().distinctUntilChanged().collectLatest { loadForUser(it) }
        }
    }

    fun retry() = loadForUser(authService.getCurrentUserId())

    fun setQuery(value: String) { _uiState.update { it.copy(query = value) } }

    internal fun setFilter(filter: CategoryFilter) { _uiState.update { it.copy(filter = filter) } }

    fun resetFilters() { _uiState.update { it.copy(query = "", filter = CategoryFilter.ALL) } }

    fun openEditor(category: Category? = null) {
        _uiState.update { it.copy(editor = CategoryEditorState(original = category)) }
    }

    fun dismissEditor() {
        _uiState.update { if (it.editor?.isSaving == true) it else it.copy(editor = null) }
    }

    internal fun updateDraft(draft: CategoryDraft) {
        _uiState.update { state ->
            val editor = state.editor
            if (editor == null || editor.isSaving) state
            else state.copy(editor = editor.copy(draft = draft, errors = emptySet(), saveFailed = false))
        }
    }

    fun saveCategory() {
        val state = _uiState.value
        val editor = state.editor ?: return
        if (editor.isSaving || !editor.hasChanges || state.isLoading) return
        val errors = editor.draft.validate(state.categories, editor.original)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(editor = editor.copy(errors = errors)) }
            return
        }
        val userId = authService.getCurrentUserId()
        if (userId.isBlank() || (editor.original?.isPredefined == false && editor.original.userId != userId)) {
            _uiState.update { it.copy(editor = editor.copy(saveFailed = true)) }
            return
        }
        val category = editor.draft.toCategory(editor.original, userId, System.currentTimeMillis())
        val creating = editor.original == null || editor.original.isPredefined
        _uiState.update { it.copy(editor = editor.copy(isSaving = true, saveFailed = false)) }
        viewModelScope.launch {
            try {
                val saved = if (creating) categoryService.addCategory(category)
                    else if (categoryService.updateCategory(category)) category else null
                ensureActive()
                if (activeUserId != userId) return@launch
                if (saved == null) {
                    _uiState.update { it.copy(editor = it.editor?.copy(isSaving = false, saveFailed = true)) }
                } else {
                    customCategories = customCategories.filterNot { it.id == saved.id } + saved
                    _uiState.update {
                        it.copy(categories = mergeCategories(customCategories), editor = null, query = "",
                            filter = if (creating) CategoryFilter.CUSTOM else it.filter)
                    }
                    _effects.send(if (creating) CategoryEffect.CREATED else CategoryEffect.UPDATED)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (activeUserId == userId) {
                    _uiState.update { it.copy(editor = it.editor?.copy(isSaving = false, saveFailed = true)) }
                }
            }
        }
    }

    fun requestDelete(category: Category) {
        if (!category.isPredefined && category.id.isNotBlank()) {
            _uiState.update { it.copy(deleteTarget = category, deleteFailed = false) }
        }
    }

    fun dismissDelete() {
        _uiState.update { if (it.isDeleting) it else it.copy(deleteTarget = null, deleteFailed = false) }
    }

    fun deleteCategory() {
        val state = _uiState.value
        val category = state.deleteTarget ?: return
        val userId = authService.getCurrentUserId()
        if (state.isDeleting || category.isPredefined || category.id.isBlank()) return
        if (userId.isBlank() || category.userId != userId) {
            _uiState.update { it.copy(deleteFailed = true) }
            return
        }
        _uiState.update { it.copy(isDeleting = true, deleteFailed = false) }
        viewModelScope.launch {
            try {
                val success = categoryService.deleteCategory(category.id)
                ensureActive()
                if (activeUserId != userId) return@launch
                if (success) {
                    customCategories = customCategories.filterNot { it.id == category.id }
                    _uiState.update { it.copy(categories = mergeCategories(customCategories), deleteTarget = null, isDeleting = false) }
                    _effects.send(CategoryEffect.DELETED)
                } else _uiState.update { it.copy(isDeleting = false, deleteFailed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (activeUserId == userId) _uiState.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }

    private fun loadForUser(userId: String) {
        loadJob?.cancel()
        if (activeUserId != userId) {
            customCategories = emptyList()
            _uiState.value = CategoriesUiState()
        } else _uiState.update { it.copy(isLoading = true, hasError = false) }
        activeUserId = userId
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        loadJob = viewModelScope.launch {
            try {
                categoryService.getCategories(userId).collectLatest { categories ->
                    customCategories = categories
                    _uiState.update { it.copy(categories = mergeCategories(categories), isLoading = false, hasError = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }
}
