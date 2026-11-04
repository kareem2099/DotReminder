package com.FreeRave.remindme.ui.reminder.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.SmartTextField
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormError
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent

@Composable
internal fun ReminderDetailsSection(
    draft: ReminderDraft,
    errors: Set<ReminderFormError>,
    enabled: Boolean,
    onEvent: (ReminderFormEvent) -> Unit
) {
    ReminderFormSection(stringResource(R.string.reminder_details), Icons.Outlined.EditNote) {
        SmartTextField(
            value = draft.title,
            onValueChange = { onEvent(ReminderFormEvent.Title(it)) },
            label = stringResource(R.string.reminder_title),
            placeholder = stringResource(R.string.reminder_title_hint),
            enabled = enabled,
            isError = ReminderFormError.TITLE in errors,
            errorMessage = if (ReminderFormError.TITLE in errors) stringResource(R.string.reminder_error_title) else null
        )
        SmartTextField(
            value = draft.description,
            onValueChange = { onEvent(ReminderFormEvent.Description(it)) },
            label = stringResource(R.string.reminder_notes),
            placeholder = stringResource(R.string.reminder_notes_hint),
            singleLine = false,
            enabled = enabled
        )
    }
}
