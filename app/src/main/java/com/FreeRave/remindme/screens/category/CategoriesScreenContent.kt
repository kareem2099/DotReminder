package com.FreeRave.remindme.screens.category

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.screens.category.components.CategoriesStatusCard
import com.FreeRave.remindme.screens.category.components.CategoryCard
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesScreenContent(
    state: CategoriesUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (CategoryFilter) -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit
) {
    val visible = state.visibleCategories
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(),
                    title = { Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Button(onClick = onCreate, modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 56.dp),
                            shape = RoundedCornerShape(18.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.categories_new), modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "intro") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.categories_intro), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pluralStringResource(R.plurals.categories_count, state.categories.size, state.categories.size),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "search") { CategoriesSearch(state.query, onQueryChange) }
                    item(key = "filters") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CategoryFilter.entries, key = { it.name }) { filter ->
                                FilterChip(
                                    selected = state.filter == filter, onClick = { onFilterChange(filter) },
                                    label = { Text(stringResource(filter.labelResource)) }, shape = RoundedCornerShape(50), border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Transparent, labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                    if (state.isLoading) item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (state.hasError || (visible.isEmpty() && !state.isLoading)) item(key = "status") {
                        CategoriesStatusCard(state.isLoading, state.hasError, state.query.isNotBlank(), onRetry, onReset)
                    }
                    listOf(false, true).forEach { builtIn ->
                        val categories = visible.filter { it.isPredefined == builtIn }
                        if (categories.isNotEmpty()) {
                            item(key = "group:$builtIn") {
                                Text(stringResource(if (builtIn) R.string.categories_built_in else R.string.categories_yours),
                                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                            }
                            items(categories, key = { if (it.isPredefined) "builtin:${categoryNameKey(it.name)}" else "custom:${it.id}" }) { category ->
                                CategoryCard(category, onEdit = { onEdit(category) }, onDelete = { onDelete(category) },
                                    modifier = Modifier.animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(140), placementSpec = tween(220)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesSearch(query: String, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    GlassSurface(shape = RoundedCornerShape(20.dp)) {
        TextField(
            value = query, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(stringResource(R.string.categories_search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = if (query.isNotEmpty()) { {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, stringResource(R.string.categories_clear_search)) }
            } } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() })
        )
    }
}

private val CategoryFilter.labelResource: Int get() = when (this) {
    CategoryFilter.ALL -> R.string.categories_all
    CategoryFilter.CUSTOM -> R.string.categories_custom
    CategoryFilter.BUILT_IN -> R.string.categories_built_in
}
