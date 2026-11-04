package com.FreeRave.remindme.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.screens.category.CategoryViewModel
import com.FreeRave.remindme.model.QuickFilter
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.utils.GpsEnableDialog
import kotlinx.coroutines.delay

internal data class HomeUiState(
    val reminders: List<Reminder> = emptyList(),
    val allReminders: List<Reminder> = emptyList(),
    val categories: List<Category> = emptyList(),
    val userName: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedQuickFilter: QuickFilter = QuickFilter.ALL,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false
) {
    val hasFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedCategory != null || selectedQuickFilter != QuickFilter.ALL
}

/** Navigation and state collection live here; the content can render without services. */
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedQuickFilter by viewModel.selectedQuickFilter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val categories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val showGpsDialog by viewModel.showGpsEnableDialog.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentTime by produceState(System.currentTimeMillis(), lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = System.currentTimeMillis()
                delay(30_000L)
            }
        }
    }

    GlassTheme {
        HomeScreenContent(
            state = HomeUiState(
                reminders = reminders,
                allReminders = allReminders,
                categories = categories,
                userName = userName,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                selectedQuickFilter = selectedQuickFilter,
                isRefreshing = isRefreshing, hasError = hasError
            ),
            currentTime = currentTime,
            onNavigate = { navController.navigate(it) },
            onSearchChange = viewModel::searchReminders,
            onCategoryChange = viewModel::selectCategory,
            onQuickFilterChange = viewModel::selectQuickFilter,
            onRefresh = viewModel::refreshReminders,
            onDelete = viewModel::deleteReminder,
            onResetFilters = {
                viewModel.selectCategory(null)
                viewModel.selectQuickFilter(QuickFilter.ALL)
                viewModel.searchReminders("")
            }
        )
        GpsEnableDialog(
            showDialog = showGpsDialog,
            onDismiss = viewModel::dismissGpsDialog,
            onConfirm = viewModel::enableGps
        )
    }
}
