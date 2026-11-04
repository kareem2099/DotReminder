package com.FreeRave.remindme.screens.templates

import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
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
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.screens.templates.components.TemplateCard
import com.FreeRave.remindme.screens.templates.components.TemplatesStatusCard
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplatesScreenContent(
    state: TemplatesUiState, snackbar: SnackbarHostState, onBack: () -> Unit, onCreate: () -> Unit,
    onUse: (ReminderTemplate) -> Unit, onEdit: (ReminderTemplate) -> Unit, onDelete: (ReminderTemplate) -> Unit,
    onQuery: (String) -> Unit, onFilter: (TemplateFilter) -> Unit, onRetry: () -> Unit, onReset: () -> Unit
) {
    val visible = state.visibleTemplates
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(), title = { Text(stringResource(R.string.templates_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back)) }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }, bottomBar = {
                Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Button(onClick = onCreate, enabled = state.canInteract,
                            shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 56.dp)) {
                            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.templates_new))
                        }
                    }
                }
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item(key = "intro") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.templates_intro), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!state.isLoading && !state.hasError && state.isAuthenticated) Text(
                                pluralStringResource(R.plurals.templates_count, state.templates.size, state.templates.size),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "search") { TemplatesSearch(state.query, onQuery) }
                    item(key = "filters") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TemplateFilter.entries, key = { it.name }) { filter ->
                                FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) },
                                    label = { Text(stringResource(filter.label)) }, shape = RoundedCornerShape(50), border = null,
                                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                            }
                        }
                    }
                    if (state.isLoading || state.hasError || !state.isAuthenticated || visible.isEmpty()) item(key = "status") {
                        TemplatesStatusCard(state, onRetry, onReset)
                    }
                    items(visible, key = { "template:${it.id}" }) { template ->
                        TemplateCard(template, state.canInteract, onUse = { onUse(template) },
                            onEdit = { onEdit(template) }, onDelete = { onDelete(template) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesSearch(query: String, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    GlassSurface(shape = RoundedCornerShape(20.dp)) {
        TextField(query, onChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(stringResource(R.string.templates_search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = if (query.isNotEmpty()) { {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, stringResource(R.string.templates_clear_search)) }
            } } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
    }
}
private val TemplateFilter.label: Int get() = when (this) {
    TemplateFilter.ALL -> R.string.templates_all
    TemplateFilter.RECURRING -> R.string.templates_recurring
    TemplateFilter.LOCATION -> R.string.templates_location
}
