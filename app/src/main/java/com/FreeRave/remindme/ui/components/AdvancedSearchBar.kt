package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.QuickFilter

@Composable
fun AdvancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: QuickFilter,
    onFilterChange: (QuickFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassSurface(shape = RoundedCornerShape(20.dp)) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.home_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(22.dp)) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, stringResource(R.string.home_clear_search), modifier = Modifier.size(20.dp))
                    } }
                } else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Completion is not tracked by the data model; don't present overdue items as completed.
            items(visibleQuickFilters, key = { it.name }) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(stringResource(filter.labelResource)) },
                    shape = RoundedCornerShape(50),
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

private val visibleQuickFilters = listOf(QuickFilter.ALL, QuickFilter.TODAY, QuickFilter.THIS_WEEK, QuickFilter.OVERDUE)

private val QuickFilter.labelResource: Int
    get() = when (this) {
        QuickFilter.ALL -> R.string.home_all
        QuickFilter.TODAY -> R.string.home_today
        QuickFilter.THIS_WEEK -> R.string.home_week
        QuickFilter.OVERDUE -> R.string.home_overdue
        QuickFilter.COMPLETED -> R.string.home_completed
    }
