package com.FreeRave.remindme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.FreeRave.remindme.data.ThemeSetting
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
//import com.FreeRave.remindme.screens.SplashScreen
import com.FreeRave.remindme.ui.theme.ScreenBackground
import com.FreeRave.remindme.ui.theme.ScreenType

@Composable
fun AppNavGraph(
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    // Check authentication state
    val loginState by authViewModel.loginState.collectAsState()

    val currentTheme by mainViewModel.currentTheme.collectAsState()
    val useDarkTheme = when (currentTheme) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
        ThemeSetting.OCEAN_BLUE -> isSystemInDarkTheme()
        ThemeSetting.FOREST_GREEN -> false
        ThemeSetting.SUNSET_ORANGE -> false
        ThemeSetting.MIDNIGHT_PURPLE -> true
        ThemeSetting.CANDY -> false
    }

    // Determine destination after splash based on authentication
    val destinationAfterSplash = when (loginState) {
        is AuthViewModel.LoginState.Success -> {
            // User is authenticated, go to home unless specific destination requested
            when {
                startDestination != Screen.Splash.route -> startDestination
                else -> Screen.Home.route
            }
        }
        else -> Screen.Login.route // Not authenticated, show login
    }

    NavHost(
        navController = navController,
        startDestination = destinationAfterSplash,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            ScreenBackground(screenType = ScreenType.LOGIN, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                LoginScreen(navController)
            }
        }
        composable(Screen.Splash.route) { /* No longer used in NavHost, handled by MainActivity */ }
        composable(Screen.SignUp.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            ScreenBackground(screenType = ScreenType.SIGNUP, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                SignUpScreen(
                    navController = navController,
                    authService = viewModel.firebaseAuthService,
                    verificationService = viewModel.verificationService
                )
            }
        }
        composable(Screen.Home.route) {
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                HomeScreen(navController)
            }
        }
        composable(Screen.Calendar.route) {
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                CalendarScreen(navController)
            }
        }
        composable(Screen.Add.route) {
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                AddReminderScreen(navController)
            }
        }
        composable(Screen.Edit.route) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId") ?: ""
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                EditReminderScreen(navController, reminderId)
            }
        }
        composable(Screen.Settings.route) {
            ScreenBackground(screenType = ScreenType.SETTINGS, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                SettingsScreen(navController)
            }
        }
        composable(Screen.AccountSecurity.route) {
            ScreenBackground(screenType = ScreenType.SETTINGS, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                AccountSecurityScreen(navController)
            }
        }
        composable(Screen.ActiveSessions.route) {
            ScreenBackground(screenType = ScreenType.SETTINGS, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                ActiveSessionsScreen(navController)
            }
        }
        composable(Screen.SignOut.route) {
            SignOutScreen(navController)
        }
        composable(Screen.VerifyEmail.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            ScreenBackground(screenType = ScreenType.LOGIN, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                VerifyEmailScreen(
                    navController = navController,
                    authService = viewModel.firebaseAuthService
                )
            }
        }
        composable(Screen.AiChat.route) {
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                AiChatScreen(navController)
            }
        }
        composable(Screen.Categories.route) {
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                CategoriesScreen(navController)
            }
        }
        composable(Screen.Templates.route) {
            ScreenBackground(screenType = ScreenType.HOME, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                TemplatesScreen(navController)
            }
        }
        composable(Screen.Support.route) {
            ScreenBackground(screenType = ScreenType.SETTINGS, themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                SupportScreen(navController)
            }
        }
    }
}
