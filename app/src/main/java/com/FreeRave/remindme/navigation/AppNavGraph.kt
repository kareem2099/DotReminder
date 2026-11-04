package com.FreeRave.remindme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.FreeRave.remindme.security.lock.AppLockGate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.FreeRave.remindme.MainViewModel
import com.FreeRave.remindme.screens.auth.AuthViewModel
import com.FreeRave.remindme.screens.auth.LoginScreen
import com.FreeRave.remindme.screens.auth.SignUpScreen
import com.FreeRave.remindme.screens.auth.SignOutScreen
import com.FreeRave.remindme.screens.auth.VerifyEmailScreen
import com.FreeRave.remindme.screens.home.HomeScreen
import com.FreeRave.remindme.screens.settings.SettingsScreen
import com.FreeRave.remindme.screens.settings.AccountSecurityScreen
import com.FreeRave.remindme.screens.settings.ActiveSessionsScreen
import com.FreeRave.remindme.screens.add.AddReminderScreen
import com.FreeRave.remindme.screens.edit.EditReminderScreen
import com.FreeRave.remindme.screens.ai.AiChatScreen
import com.FreeRave.remindme.screens.support.SupportScreen
import com.FreeRave.remindme.screens.calendar.CalendarScreen
import com.FreeRave.remindme.screens.category.CategoriesScreen
import com.FreeRave.remindme.screens.templates.TemplatesScreen

@Composable
fun AppNavGraph(
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    // Check authentication state
    val loginState by authViewModel.loginState.collectAsState()
    var forceLoginStart by rememberSaveable { mutableStateOf(false) }
    var redirectToLogin by rememberSaveable { mutableStateOf(false) }

    // Determine destination after splash based on authentication
    val destinationAfterSplash = when (loginState) {
        is AuthViewModel.LoginState.Success -> {
            when {
                startDestination != Screen.Splash.route -> startDestination
                else -> Screen.Home.route
            }
        }
        else -> Screen.Login.route
    }

    AppLockGate(onRequireLogin = { forceLoginStart = true; redirectToLogin = true }) {
    NavHost(
        navController = navController,
        startDestination = if (forceLoginStart) Screen.Login.route else destinationAfterSplash,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Splash.route) { /* Handled by MainActivity */ }

        composable(Screen.SignUp.route) {
            SignUpScreen(navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(navController)
        }

        composable(Screen.Add.route) { entry ->
            AddReminderScreen(navController, entry)
        }

        composable(Screen.Edit.route) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId") ?: ""
            EditReminderScreen(navController, reminderId)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }

        composable(Screen.AccountSecurity.route) {
            AccountSecurityScreen(navController)
        }

        composable(Screen.ActiveSessions.route) {
            ActiveSessionsScreen(navController)
        }

        composable(Screen.SignOut.route) {
            SignOutScreen(navController)
        }

        composable(Screen.VerifyEmail.route) {
            VerifyEmailScreen(navController)
        }

        composable(Screen.AiChat.route) { entry ->
            AiChatScreen(navController, entry)
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(navController)
        }

        composable(Screen.Templates.route) {
            TemplatesScreen(navController)
        }

        composable(Screen.Support.route) {
            SupportScreen(navController)
        }
    }
    LaunchedEffect(redirectToLogin) {
        if (redirectToLogin) {
            navController.navigate(Screen.Login.route) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
            redirectToLogin = false
        }
    }
    }
}
