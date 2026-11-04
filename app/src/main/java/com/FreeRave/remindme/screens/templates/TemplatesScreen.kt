package com.FreeRave.remindme.screens.templates

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.R
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.category.CategoryViewModel
import com.FreeRave.remindme.screens.templates.components.DeleteTemplateDialog
import com.FreeRave.remindme.screens.templates.components.TemplateEditorSheet
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.launch

@Composable
fun TemplatesScreen(
    navController: NavController, viewModel: TemplatesViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            scope.launch { snackbar.showSnackbar(resources.getString(when (effect) {
                TemplateEffect.CREATED -> R.string.templates_created
                TemplateEffect.UPDATED -> R.string.templates_updated
                TemplateEffect.DELETED -> R.string.templates_deleted
            })) }
        }
    }
    GlassTheme {
        TemplatesScreenContent(state, snackbar, onBack = { navController.popBackStack() }, onCreate = { viewModel.openEditor() },
            onUse = { template ->
                if (state.canInteract) {
                    val previous = navController.previousBackStackEntry
                    if (previous?.destination?.route == Screen.Add.route) {
                        previous.savedStateHandle["selected_template"] = template
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Add.route) {
                            popUpTo(Screen.Templates.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        navController.getBackStackEntry(Screen.Add.route).savedStateHandle["selected_template"] = template
                    }
                }
            }, onEdit = viewModel::openEditor, onDelete = viewModel::requestDelete,
            onQuery = viewModel::setQuery, onFilter = viewModel::setFilter, onRetry = viewModel::retry, onReset = viewModel::resetFilters)
        state.editor?.let { editor ->
            TemplateEditorSheet(editor, categories, canSave = !state.isLoading && state.isAuthenticated,
                onChange = viewModel::updateDraft, onSave = viewModel::saveEditor, onDismiss = viewModel::dismissEditor)
        }
        state.deleteTarget?.let { template ->
            DeleteTemplateDialog(template, state.isDeleting, state.deleteFailed,
                onConfirm = viewModel::deleteTemplate, onDismiss = viewModel::dismissDelete)
        }
    }
}
