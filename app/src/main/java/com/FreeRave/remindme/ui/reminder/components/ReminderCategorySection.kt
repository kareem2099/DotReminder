package com.FreeRave.remindme.ui.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.utils.CategoryUtils
import com.FreeRave.remindme.utils.SmartSuggestionEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderCategorySection(
    title: String,
    description: String,
    selected: String,
    categories: List<Category>,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val engine = remember { SmartSuggestionEngine() }
    val suggestions = remember(title, description) {
        if (title.isBlank() && description.isBlank()) emptyList() else engine.suggestCategory(title, description).take(2)
    }
    val names = remember(categories, selected) {
        (CategoryUtils.predefinedCategories + categories.map { it.name } + listOfNotNull(selected.takeIf { it.isNotBlank() }))
            .distinct()
    }
    ReminderFormSection(stringResource(R.string.reminder_category), Icons.AutoMirrored.Outlined.Label) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderOptionChip(stringResource(R.string.reminder_none), selected.isEmpty(), enabled) { onSelect("") }
            names.forEach { name ->
                ReminderOptionChip(name, selected == name, enabled) { onSelect(name) }
            }
        }
        if (suggestions.isNotEmpty()) {
            Text(stringResource(R.string.reminder_category_suggestions), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    ReminderOptionChip(suggestion.categoryName, selected == suggestion.categoryName, enabled) {
                        onSelect(suggestion.categoryName)
                    }
                }
            }
        }
    }
}
