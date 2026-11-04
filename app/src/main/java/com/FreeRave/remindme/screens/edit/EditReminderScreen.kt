package com.FreeRave.remindme.screens.edit

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
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.category.CategoryViewModel
import com.FreeRave.remindme.ui.reminder.ReminderFormContent
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent
import com.FreeRave.remindme.ui.reminder.ReminderFormMode
import com.FreeRave.remindme.ui.reminder.validate
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.reminders.rememberReminderDeliveryAccess
import kotlinx.coroutines.launch

@Composable
fun EditReminderScreen(
    navController: NavController,
    reminderId: String,
    viewModel: EditReminderViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val saveWithDeliveryAccess = rememberReminderDeliveryAccess(viewModel::saveReminder)

    LaunchedEffect(reminderId) { viewModel.loadReminder(reminderId) }
    LaunchedEffect(viewModel, reminderId) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditReminderEffect.SAVED -> {
                    val current = navController.currentBackStackEntry
                    if (current?.destination?.route == Screen.Edit.route && current.arguments?.getString("reminderId") == reminderId) {
                        navController.popBackStack()
                    }
                }
                else -> scope.launch {
                    val message = if (effect == EditReminderEffect.VALIDATION_FAILED) R.string.reminder_error_fields
                        else R.string.reminder_update_error
                    snackbar.showSnackbar(resources.getString(message))
                }
            }
        }
    }

    GlassTheme {
        if (state.isLoading || state.original == null) {
            EditReminderLoadingContent(state.isLoading, snackbar,
                onBack = { navController.popBackStack() }, onRetry = { viewModel.loadReminder(reminderId) })
        } else {
            val errors = if (state.validationRequest > 0) {
                state.draft.validate(System.currentTimeMillis(), state.original?.reminderTime)
            } else emptySet()
            ReminderFormContent(
                draft = state.draft, categories = categories, errors = errors,
                validationRequest = state.validationRequest, isSaving = state.isSaving, isParsing = false,
                snackbar = snackbar, mode = ReminderFormMode.EDIT,
                referenceId = state.original?.customId, saveEnabled = state.hasChanges,
                onEvent = { event ->
                    when (event) {
                        ReminderFormEvent.Back -> navController.popBackStack()
                        is ReminderFormEvent.Save -> saveWithDeliveryAccess()
                        else -> viewModel.updateDraft(event)
                    }
                }
            )
        }
    }
}
