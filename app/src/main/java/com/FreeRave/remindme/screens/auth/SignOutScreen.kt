package com.FreeRave.remindme.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.theme.GlassTheme

@Composable
fun SignOutScreen(navController: NavController, viewModel: SignOutViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.busy) { /* Finish the in-progress sign-out before leaving. */ }
    LaunchedEffect(state.status) {
        if (state.status == SignOutStatus.COMPLETE) navigateAfterSignOut(navController)
    }
    GlassTheme {
        SignOutScreenContent(state, onConfirm = viewModel::signOut,
            onCancel = { if (!state.busy) navController.popBackStack() })
    }
}

internal fun navigateAfterSignOut(navController: NavController) {
    navController.navigate(Screen.Login.route) {
        popUpTo(navController.graph.id) { inclusive = false }
        launchSingleTop = true
    }
}
