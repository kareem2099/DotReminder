package com.FreeRave.remindme.screens.add

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.templates.TemplatesViewModel
import com.FreeRave.remindme.ui.components.GlassFormDialog
import com.FreeRave.remindme.ui.reminder.ReminderDraft

/** Services are created only for the actual save flow; the form can be reviewed independently. */
@Composable
internal fun SaveReminderTemplateDialog(draft: ReminderDraft, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val viewModel: TemplatesViewModel = hiltViewModel()
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    SaveTemplateNameDialog(saving, failed, onDismiss, onEdit = { failed = false }, onSave = { name ->
        if (!saving) {
            saving = true
            val template = viewModel.createTemplateFromReminder(
                reminderTitle = draft.title, reminderDescription = draft.description,
                reminderCategory = draft.category, isRecurring = draft.isRecurring,
                recurrenceIntervalMillis = draft.recurrenceIntervalMillis,
                isLocationBased = draft.isLocationBased, templateName = name)
            viewModel.addTemplate(template) { success ->
                saving = false; failed = !success
                if (success) onSaved()
            }
        }
    })
}

@Composable
internal fun SaveTemplateNameDialog(busy: Boolean, failed: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val save = { if (!busy && name.isNotBlank()) onSave(name.trim()) }
    GlassFormDialog(stringResource(R.string.reminder_save_template), stringResource(R.string.reminder_confirm), save, onDismiss,
        subtitle = stringResource(R.string.reminder_template_hint), confirmEnabled = name.isNotBlank(), busy = busy,
        error = if (failed) stringResource(R.string.reminder_template_error) else null) {
        OutlinedTextField(name, { name = it; onEdit() }, label = { Text(stringResource(R.string.reminder_template_name)) },
            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }))
    }
}
