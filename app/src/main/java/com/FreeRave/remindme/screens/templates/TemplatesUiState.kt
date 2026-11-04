package com.FreeRave.remindme.screens.templates

import com.FreeRave.remindme.model.ReminderTemplate

internal enum class TemplateFilter { ALL, RECURRING, LOCATION }
internal enum class TemplateEffect { CREATED, UPDATED, DELETED }
internal data class TemplateEditorState(
    val original: ReminderTemplate? = null,
    val draft: TemplateDraft = original?.toDraft() ?: TemplateDraft(),
    val errors: Set<TemplateFieldError> = emptySet(), val isSaving: Boolean = false, val saveFailed: Boolean = false
)
internal data class TemplatesUiState(
    val templates: List<ReminderTemplate> = emptyList(), val isLoading: Boolean = true,
    val hasError: Boolean = false, val isAuthenticated: Boolean = false,
    val query: String = "", val filter: TemplateFilter = TemplateFilter.ALL,
    val editor: TemplateEditorState? = null, val deleteTarget: ReminderTemplate? = null,
    val isDeleting: Boolean = false, val deleteFailed: Boolean = false
) {
    val canInteract get() = isAuthenticated && !isLoading && !hasError && !isDeleting && editor?.isSaving != true
    val visibleTemplates get() = templates.filter { template ->
        val term = query.trim()
        (term.isEmpty() || listOf(template.name, template.title, template.description, template.category).any { it.contains(term, true) }) &&
            when (filter) { TemplateFilter.ALL -> true; TemplateFilter.RECURRING -> template.isRecurring; TemplateFilter.LOCATION -> template.isLocationBased }
    }
}
