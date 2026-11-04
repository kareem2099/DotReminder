package com.FreeRave.remindme.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.theme.GlassTheme

@Composable
fun VerifyEmailScreen(navController: NavController, viewModel: EmailVerificationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    GlassTheme {
        VerifyEmailScreenContent(state, onBack = { navController.popBackStack() },
            onCheck = { viewModel.refresh(showPending = true) }, onSend = viewModel::sendLink,
            onRetry = { viewModel.refresh() }, onRetrySync = viewModel::retrySync,
            onContinue = {
                if (!navController.popBackStack(Screen.Home.route, false)) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }; launchSingleTop = true
                    }
                }
            }, onLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = false }; launchSingleTop = true
                }
            })
    }
}
