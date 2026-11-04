package com.FreeRave.remindme.screens.category

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.category.components.CategoryEditorSheet
import com.FreeRave.remindme.screens.category.components.DeleteCategoryDialog
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(navController: NavController, viewModel: CategoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            scope.launch {
                val label = when (effect) {
                    CategoryEffect.CREATED -> R.string.category_created
                    CategoryEffect.UPDATED -> R.string.category_updated
                    CategoryEffect.DELETED -> R.string.category_deleted
                }
                snackbar.showSnackbar(resources.getString(label))
            }
        }
    }
    GlassTheme {
        CategoriesScreenContent(
            state = state, snackbar = snackbar,
            onBack = { navController.popBackStack() }, onCreate = { viewModel.openEditor() },
            onEdit = viewModel::openEditor, onDelete = viewModel::requestDelete,
            onQueryChange = viewModel::setQuery, onFilterChange = viewModel::setFilter,
            onReset = viewModel::resetFilters, onRetry = viewModel::retry
        )
        state.editor?.let { editor ->
            CategoryEditorSheet(editor, canSave = !state.isLoading, onChange = viewModel::updateDraft,
                onSave = viewModel::saveCategory, onDismiss = viewModel::dismissEditor)
        }
        state.deleteTarget?.let { category ->
            DeleteCategoryDialog(category, state.isDeleting, state.deleteFailed,
                onConfirm = viewModel::deleteCategory, onDismiss = viewModel::dismissDelete)
        }
    }
}
