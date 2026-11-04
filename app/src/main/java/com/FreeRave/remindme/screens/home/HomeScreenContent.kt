package com.FreeRave.remindme.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.home.components.HomeActions
import com.FreeRave.remindme.screens.home.components.HomeCategoryFilters
import com.FreeRave.remindme.screens.home.components.HomeEmptyState
import com.FreeRave.remindme.screens.home.components.HomeToolbar
import com.FreeRave.remindme.ui.components.AdvancedSearchBar
import com.FreeRave.remindme.model.QuickFilter
import com.FreeRave.remindme.ui.components.WelcomeSection
import com.FreeRave.remindme.ui.components.reminderItems
import com.FreeRave.remindme.ui.theme.GlassBackground
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    currentTime: Long,
    onNavigate: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onQuickFilterChange: (QuickFilter) -> Unit,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onResetFilters: () -> Unit
) {
    val refreshState = rememberPullRefreshState(state.isRefreshing, onRefresh)
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                HomeActions(
                    onAdd = { onNavigate(Screen.Add.route) },
                    onAssistant = { onNavigate(Screen.AiChat.route) }
                )
            }
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets).pullRefresh(refreshState)) {
                LazyColumn(
                    modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().testTag("home-reminders"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "toolbar") { HomeToolbar(onNavigate) }
                    item(key = "welcome") {
                        WelcomeSection(
                            userName = state.userName,
                            reminders = state.allReminders,
                            currentTime = currentTime
                        )
                    }
                    item(key = "search") {
                        AdvancedSearchBar(
                            query = state.searchQuery,
                            onQueryChange = onSearchChange,
                            selectedFilter = state.selectedQuickFilter,
                            onFilterChange = onQuickFilterChange
                        )
                    }
                    item(key = "categories") {
                        HomeCategoryFilters(state.categories, state.selectedCategory, onCategoryChange)
                    }
                    item(key = "section") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.home_library), style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f))
                            Text(
                                pluralStringResource(R.plurals.home_result_count, state.reminders.size, state.reminders.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (state.hasError) item(key = "load-error") {
                        HomeEmptyState(false, false, onRefresh, hasError = true)
                    }
                    if (state.reminders.isEmpty() && !state.hasError) {
                        item(key = "empty") {
                            HomeEmptyState(
                                isLoading = state.isRefreshing,
                                hasFilters = state.hasFilters,
                                onAction = { if (state.hasFilters) onResetFilters() else onNavigate(Screen.Add.route) }
                            )
                        }
                    }
                    reminderItems(
                        reminders = state.reminders,
                        currentTime = currentTime,
                        onReminderClick = { onNavigate(Screen.Edit.createRoute(it.id)) },
                        onDelete = onDelete
                    )
                }
                PullRefreshIndicator(
                    refreshing = state.isRefreshing,
                    state = refreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private const val PREVIEW_TIME = 1_800_000_000_000L

@Preview(name = "Home · Light", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeLightPreview() = HomePreview(false)

@Preview(name = "Home · Dark", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeDarkPreview() = HomePreview(true)

@Preview(name = "Home · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun HomeCompactPreview() = HomePreview(false)

@Preview(name = "Home · Empty", widthDp = 393, heightDp = 852)
@Composable
private fun HomeEmptyPreview() = HomePreview(false, empty = true)

@Preview(name = "Home · Filtered empty", widthDp = 393, heightDp = 852)
@Composable
private fun HomeFilteredPreview() = HomePreview(true, empty = true, filtered = true)

@Preview(name = "Home · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun HomeTabletPreview() = HomePreview(false)

@Preview(name = "Home · RTL", widthDp = 393, heightDp = 852, locale = "ar")
@Composable
private fun HomeRtlPreview() = HomePreview(true)

@Composable
private fun HomePreview(dark: Boolean, empty: Boolean = false, filtered: Boolean = false) {
    val reminders = if (empty) emptyList() else listOf(
        Reminder(id = "1", title = "A little time to reset", description = "Step outside. Clear your mind.",
            category = "Personal", reminderTime = PREVIEW_TIME + 3_600_000),
        Reminder(id = "2", title = "Bring your ideas to life", category = "Work",
            reminderTime = PREVIEW_TIME + 7_200_000, isRecurring = true),
        Reminder(id = "3", title = "Pick up something good", category = "Shopping",
            reminderTime = PREVIEW_TIME - 3_600_000)
    )
    RemindMeAppTheme(themeSetting = if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            HomeScreenContent(
                state = HomeUiState(reminders = reminders, allReminders = reminders, userName = "Alex",
                    searchQuery = if (filtered) "Meetings" else ""),
                currentTime = PREVIEW_TIME,
                onNavigate = {}, onSearchChange = {}, onCategoryChange = {}, onQuickFilterChange = {},
                onRefresh = {}, onDelete = {}, onResetFilters = {}
            )
        }
    }
}
