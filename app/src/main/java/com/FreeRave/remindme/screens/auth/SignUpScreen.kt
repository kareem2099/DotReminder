package com.FreeRave.remindme.screens.auth

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.components.SecureWindowEffect

@Composable
fun SignUpScreen(navController: NavController, viewModel: SignUpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    SecureWindowEffect(state.password.isNotEmpty() || state.code.isNotEmpty() || state.step == SignUpStep.CODE)
    val backToLogin = {
        if (!navController.popBackStack(Screen.Login.route, false)) {
            navController.navigate(Screen.Login.route) { popUpTo(Screen.SignUp.route) { inclusive = true }; launchSingleTop = true }
        }
    }
    BackHandler(enabled = state.busy || state.step != SignUpStep.DETAILS) {
        if (state.sendingCode) viewModel.cancelSend()
        else if (!state.busy && state.step == SignUpStep.CODE) viewModel.editDetails()
    }
    GlassTheme {
        SignUpScreenContent(state, onEdit = viewModel::edit, onSend = { viewModel.sendCode(activity) },
            onCreate = viewModel::createAccount, onEditDetails = viewModel::editDetails,
            onRetry = viewModel::retryProfile, onLogin = backToLogin,
            onResend = { viewModel.resendCode(activity) }, onCancelSend = viewModel::cancelSend,
            onContinue = {
                navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true }; launchSingleTop = true }
            })
    }
}
