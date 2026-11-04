package com.FreeRave.remindme.screens.settings

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
import com.FreeRave.remindme.screens.settings.components.SessionRemovalDialog
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.launch

@Composable
fun ActiveSessionsScreen(navController: NavController, viewModel: ActiveSessionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            scope.launch {
                snackbar.showSnackbar(resources.getString(when (effect) {
                    SessionEffect.RECORD_REMOVED -> R.string.sessions_removed_one
                    SessionEffect.OTHER_RECORDS_REMOVED -> R.string.sessions_removed_others
                }))
            }
        }
    }
    GlassTheme {
        ActiveSessionsScreenContent(state, snackbar, onBack = { navController.popBackStack() },
            onRetry = viewModel::retry, onRemove = viewModel::requestRemoval, onClearOthers = viewModel::requestClearOthers)
        state.removal?.let { target ->
            SessionRemovalDialog(target, state.isRemoving, state.removalFailed,
                canConfirm = state.canConfirmRemoval(), onConfirm = viewModel::confirmRemoval, onDismiss = viewModel::dismissRemoval)
        }
    }
}
