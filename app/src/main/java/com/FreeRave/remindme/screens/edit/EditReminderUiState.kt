package com.FreeRave.remindme.screens.edit

import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.toDraft

internal data class EditReminderUiState(
    val original: Reminder? = null,
    val draft: ReminderDraft = ReminderDraft(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val validationRequest: Int = 0
) {
    val hasChanges: Boolean get() = original?.let { draft != it.toDraft() } ?: false
}

internal enum class EditReminderEffect { SAVED, SAVE_FAILED, VALIDATION_FAILED }
