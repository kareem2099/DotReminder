package com.FreeRave.remindme.screens.category

import com.FreeRave.remindme.model.Category

internal data class CategoryEditorState(
    val original: Category? = null,
    val draft: CategoryDraft = original?.toDraft() ?: CategoryDraft(),
    val errors: Set<CategoryFieldError> = emptySet(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false
) {
    val hasChanges: Boolean get() = original == null || draft != original.toDraft()
}

internal data class CategoriesUiState(
    val categories: List<Category> = builtInCategories,
    val query: String = "",
    val filter: CategoryFilter = CategoryFilter.ALL,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val editor: CategoryEditorState? = null,
    val deleteTarget: Category? = null,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false
) {
    val visibleCategories: List<Category> get() = filterCategories(categories, query, filter)
    val hasFilters: Boolean get() = query.isNotBlank() || filter != CategoryFilter.ALL
}

internal enum class CategoryEffect { CREATED, UPDATED, DELETED }
