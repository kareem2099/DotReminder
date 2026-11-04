package com.FreeRave.remindme.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.utils.CategoryUtils

@Composable
internal fun HomeCategoryFilters(
    categories: List<Category>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val names = remember(categories) {
        (CategoryUtils.predefinedCategories + categories.filterNot { it.isPredefined }.map { it.name }).distinct()
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") {
            CategoryChip(stringResource(R.string.home_all_categories), selected == null) { onSelect(null) }
        }
        items(names, key = { "category:$it" }) { name ->
            CategoryChip(name, selected == name) { onSelect(name) }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    )
}
