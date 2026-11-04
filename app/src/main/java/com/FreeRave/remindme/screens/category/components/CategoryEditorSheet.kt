package com.FreeRave.remindme.screens.category.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.category.CategoryDraft
import com.FreeRave.remindme.screens.category.CategoryEditorState
import com.FreeRave.remindme.screens.category.CategoryFieldError
import com.FreeRave.remindme.ui.components.GlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryEditorSheet(
    editor: CategoryEditorState, canSave: Boolean,
    onChange: (CategoryDraft) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()
    var advanced by rememberSaveable { mutableStateOf(false) }
    val enabled = !editor.isSaving
    val allowDismiss by rememberUpdatedState(enabled)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { allowDismiss || it != SheetValue.Hidden })
    val draft = editor.draft
    val nameError = when {
        CategoryFieldError.NAME_REQUIRED in editor.errors -> stringResource(R.string.category_error_name_required)
        CategoryFieldError.NAME_DUPLICATE in editor.errors -> stringResource(R.string.category_error_name_duplicate)
        else -> null
    }
    LaunchedEffect(editor.errors) {
        if (CategoryFieldError.COLOR in editor.errors) advanced = true
        if (CategoryFieldError.NAME_REQUIRED in editor.errors || CategoryFieldError.NAME_DUPLICATE in editor.errors) {
            scroll.animateScrollTo(0)
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 24.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(when {
                    editor.original == null -> R.string.categories_new
                    editor.original.isPredefined -> R.string.category_customize
                    else -> R.string.category_edit
                }), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(if (editor.original?.isPredefined == true) R.string.category_customize_hint else R.string.category_edit_hint),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GlassSurface(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CategoryGlyph(draft.icon, draft.colorHex)
                        Text(draft.name.ifBlank { stringResource(R.string.category_preview) },
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(
                    value = draft.name, onValueChange = { onChange(draft.copy(name = it)) },
                    label = { Text(stringResource(R.string.category_name)) },
                    placeholder = { Text(stringResource(R.string.category_name_hint)) },
                    singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp), isError = nameError != null,
                    supportingText = nameError?.let { message -> { Text(message) } }
                )
                OutlinedTextField(
                    value = draft.description, onValueChange = { onChange(draft.copy(description = it)) },
                    label = { Text(stringResource(R.string.category_description)) },
                    placeholder = { Text(stringResource(R.string.category_description_hint)) },
                    minLines = 2, maxLines = 4, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Text(stringResource(R.string.category_color), style = MaterialTheme.typography.titleSmall)
                CategoryColorPicker(draft.colorHex, enabled) { onChange(draft.copy(colorHex = it)) }
                if (CategoryFieldError.COLOR in editor.errors) {
                    Text(stringResource(R.string.category_error_color), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.titleSmall)
                CategoryIconPicker(draft.icon, enabled) { onChange(draft.copy(icon = it)) }
                TextButton(onClick = { advanced = !advanced }, enabled = enabled) {
                    Text(stringResource(R.string.category_custom_appearance))
                    Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                AnimatedVisibility(advanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = draft.colorHex, onValueChange = { onChange(draft.copy(colorHex = it)) },
                            label = { Text(stringResource(R.string.category_hex)) },
                            placeholder = { Text(stringResource(R.string.category_hex_hint)) },
                            singleLine = true, enabled = enabled, isError = CategoryFieldError.COLOR in editor.errors,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = draft.icon, onValueChange = { onChange(draft.copy(icon = it)) },
                            label = { Text(stringResource(R.string.category_custom_icon)) },
                            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
                if (editor.saveFailed) {
                    Text(stringResource(R.string.category_error_save), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, enabled = enabled) { Text(stringResource(R.string.category_cancel)) }
                    Button(onClick = onSave, enabled = enabled && canSave && editor.hasChanges,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                        if (editor.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(when {
                            editor.isSaving -> R.string.category_saving
                            editor.original == null || editor.original.isPredefined -> R.string.category_save
                            else -> R.string.category_save_changes
                        }), modifier = Modifier.padding(horizontal = 6.dp))
                    }
                }
            }
        }
    }
}
