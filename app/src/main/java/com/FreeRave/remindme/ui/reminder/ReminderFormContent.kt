package com.FreeRave.remindme.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.ui.reminder.components.ReminderAdvancedSection
import com.FreeRave.remindme.ui.reminder.components.ReminderCategorySection
import com.FreeRave.remindme.ui.reminder.components.ReminderDateDialog
import com.FreeRave.remindme.ui.reminder.components.ReminderDetailsSection
import com.FreeRave.remindme.ui.reminder.components.ReminderLocationDialogs
import com.FreeRave.remindme.ui.reminder.components.ReminderLocationPicker
import com.FreeRave.remindme.ui.reminder.components.ReminderLocationSection
import com.FreeRave.remindme.ui.reminder.components.ReminderPromptSection
import com.FreeRave.remindme.ui.reminder.components.ReminderScheduleSection
import com.FreeRave.remindme.ui.reminder.components.ReminderTimeDialog

internal enum class ReminderFormMode { CREATE, EDIT }

@Composable
internal fun ReminderFormContent(
    draft: ReminderDraft,
    categories: List<Category>,
    errors: Set<ReminderFormError>,
    validationRequest: Int,
    isSaving: Boolean,
    isParsing: Boolean,
    snackbar: SnackbarHostState,
    onEvent: (ReminderFormEvent) -> Unit,
    mode: ReminderFormMode = ReminderFormMode.CREATE,
    referenceId: String? = null,
    saveEnabled: Boolean = true
) {
    var customId by rememberSaveable { mutableStateOf("") }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf(false) }
    var endDatePicker by rememberSaveable { mutableStateOf(false) }
    var locationPicker by rememberSaveable { mutableStateOf<ReminderLocationPicker?>(null) }
    val listState = rememberLazyListState()
    val enabled = !isSaving && !isParsing
    val editing = mode == ReminderFormMode.EDIT
    LaunchedEffect(validationRequest) {
        if (validationRequest > 0 && errors.isNotEmpty()) {
            val detailsIndex = if (editing) 1 else 2
            val index = when {
                ReminderFormError.TITLE in errors -> detailsIndex
                errors.any { it == ReminderFormError.TIME || it == ReminderFormError.INTERVAL || it == ReminderFormError.END_TIME } -> detailsIndex + 1
                else -> detailsIndex + 3
            }
            listState.animateScrollToItem(index)
        }
    }
    ReminderFormScaffold(
        title = stringResource(if (editing) R.string.reminder_edit else R.string.reminder_new),
        snackbar = snackbar,
        onBack = { onEvent(ReminderFormEvent.Back) },
        isSaving = isSaving,
        saveLabel = stringResource(if (editing) R.string.reminder_save_changes else R.string.reminder_create),
        saveEnabled = saveEnabled && enabled,
        onSave = { onEvent(ReminderFormEvent.Save(customId)) },
        actions = {
            if (!editing) TextButton(onClick = { onEvent(ReminderFormEvent.Templates) }, enabled = enabled) {
                Text(stringResource(R.string.reminder_templates))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState, modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "intro") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(if (editing) R.string.reminder_edit_intro else R.string.reminder_intro),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        if (editing && !referenceId.isNullOrBlank()) {
                            Text(stringResource(R.string.reminder_reference, referenceId), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (editing && (draft.reminderTimeMillis ?: Long.MAX_VALUE) <= System.currentTimeMillis()) {
                            Text(stringResource(R.string.reminder_edit_overdue_hint), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (!editing) item(key = "prompt") { ReminderPromptSection(draft.prompt, isParsing, !isSaving, onEvent) }
                item(key = "details") { ReminderDetailsSection(draft, errors, enabled, onEvent) }
                item(key = "schedule") {
                    ReminderScheduleSection(draft, errors, enabled, onDate = { datePicker = true },
                        onTime = { timePicker = true }, onEndDate = { endDatePicker = true }, onEvent = onEvent)
                }
                item(key = "category") {
                    ReminderCategorySection(draft.title, draft.description, draft.category, categories, enabled) {
                        onEvent(ReminderFormEvent.Category(it))
                    }
                }
                item(key = "location") {
                    ReminderLocationSection(draft, errors, enabled, { locationPicker = ReminderLocationPicker.CHOICE }, onEvent)
                }
                if (!editing) item(key = "advanced") {
                    ReminderAdvancedSection(customId, enabled, { customId = it }) { onEvent(ReminderFormEvent.SaveTemplate) }
                }
            }
        }
    }
    val scheduled = draft.reminderTimeMillis ?: System.currentTimeMillis()
    if (datePicker) ReminderDateDialog(scheduled, onDismiss = { datePicker = false }) {
        onEvent(ReminderFormEvent.Time(it)); datePicker = false
    }
    if (timePicker) ReminderTimeDialog(scheduled, onDismiss = { timePicker = false }) {
        onEvent(ReminderFormEvent.Time(it)); timePicker = false
    }
    if (endDatePicker) ReminderDateDialog(draft.recurrenceEndTime ?: scheduled + DAY_MILLIS,
        onDismiss = { endDatePicker = false }) {
        onEvent(ReminderFormEvent.EndTime(it)); endDatePicker = false
    }
    ReminderLocationDialogs(locationPicker, draft, { locationPicker = it }, onEvent)
}
