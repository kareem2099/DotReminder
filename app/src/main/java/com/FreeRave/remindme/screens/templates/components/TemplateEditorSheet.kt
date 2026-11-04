package com.FreeRave.remindme.screens.templates.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.screens.settings.components.SettingItemWithSwitch
import com.FreeRave.remindme.screens.templates.*
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.reminder.components.ReminderFormSection
import com.FreeRave.remindme.ui.reminder.components.ReminderOptionChip
import com.FreeRave.remindme.utils.CategoryUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TemplateEditorSheet(
    editor: TemplateEditorState, categories: List<Category>, canSave: Boolean,
    onChange: (TemplateDraft) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit
) {
    val draft = editor.draft
    val enabled = !editor.isSaving
    val dismissible by rememberUpdatedState(enabled)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { dismissible || it != SheetValue.Hidden })
    val scroll = rememberScrollState()
    LaunchedEffect(editor.errors) {
        if (TemplateFieldError.NAME in editor.errors || TemplateFieldError.TITLE in editor.errors) scroll.animateScrollTo(0)
    }
    ModalBottomSheet(onDismissRequest = { if (enabled) onDismiss() }, sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(stringResource(if (editor.original == null) R.string.templates_new else R.string.templates_edit),
                    style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.templates_editor_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ReminderFormSection(stringResource(R.string.templates_details), Icons.Outlined.Description) {
                    TemplateField(draft.name, { onChange(draft.copy(name = it)) }, stringResource(R.string.templates_name), enabled,
                        error = if (TemplateFieldError.NAME in editor.errors) stringResource(R.string.templates_error_name) else null,
                        placeholder = stringResource(R.string.templates_name_hint))
                    TemplateField(draft.title, { onChange(draft.copy(title = it)) }, stringResource(R.string.templates_default_title), enabled,
                        error = if (TemplateFieldError.TITLE in editor.errors) stringResource(R.string.templates_error_title) else null)
                    TemplateField(draft.description, { onChange(draft.copy(description = it)) }, stringResource(R.string.templates_description), enabled,
                        multiline = true)
                }
                ReminderFormSection(stringResource(R.string.reminder_category), Icons.AutoMirrored.Outlined.Label) {
                    TemplateField(draft.category, { onChange(draft.copy(category = it)) }, stringResource(R.string.reminder_category), enabled)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderOptionChip(stringResource(R.string.reminder_none), draft.category.isEmpty(), enabled) { onChange(draft.copy(category = "")) }
                        (CategoryUtils.predefinedCategories + categories.map { it.name }).distinct().forEach { name ->
                            ReminderOptionChip(name, draft.category == name, enabled) { onChange(draft.copy(category = name)) }
                        }
                    }
                }
                GlassSurface {
                    Column {
                        SettingItemWithSwitch(stringResource(R.string.templates_repeat), draft.isRecurring,
                            { onChange(draft.copy(isRecurring = it)) }, enabled, subtitle = stringResource(R.string.templates_repeat_hint))
                        if (draft.isRecurring) Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(draft.interval.amount, onValueChange = { onChange(draft.copy(interval = draft.interval.copy(amount = it.take(25)))) },
                                label = { Text(stringResource(R.string.templates_repeat_every)) }, enabled = enabled, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = TemplateFieldError.INTERVAL in editor.errors)
                            Text(stringResource(R.string.templates_interval_unit), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TemplateIntervalUnit.entries.forEach { unit ->
                                    ReminderOptionChip(stringResource(unit.label), unit == draft.interval.unit, enabled) {
                                        onChange(draft.copy(interval = draft.interval.copy(unit = unit)))
                                    }
                                }
                            }
                            if (TemplateFieldError.INTERVAL in editor.errors) Text(stringResource(R.string.templates_error_interval),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                GlassSurface {
                    SettingItemWithSwitch(stringResource(R.string.templates_location_toggle), draft.isLocationBased,
                        { onChange(draft.copy(isLocationBased = it)) }, enabled, subtitle = stringResource(R.string.templates_location_hint))
                }
                if (editor.saveFailed) Text(stringResource(R.string.templates_error_save), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, enabled = enabled) { Text(stringResource(R.string.templates_cancel)) }
                    Button(onClick = onSave, enabled = enabled && canSave && draft.hasChanges(editor.original),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                        if (editor.isSaving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                        Text(stringResource(if (editor.isSaving) R.string.templates_saving else R.string.templates_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateField(
    value: String, onChange: (String) -> Unit, label: String, enabled: Boolean,
    error: String? = null, placeholder: String? = null, multiline: Boolean = false
) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        singleLine = !multiline, minLines = if (multiline) 2 else 1, maxLines = if (multiline) 4 else 1,
        isError = error != null, shape = RoundedCornerShape(16.dp),
        placeholder = placeholder?.let { { Text(it) } }, supportingText = error?.let { { Text(it) } })
}
