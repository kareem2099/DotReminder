package com.FreeRave.remindme.screens.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.delay

@Composable
fun CalendarScreen(navController: NavController) {
    val viewModel: CalendarViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.updateClock()
                delay(30_000)
            }
        }
    }
    GlassTheme {
        CalendarScreenContent(
            state = state,
            onBack = { navController.popBackStack() },
            onSelectDate = viewModel::selectDate,
            onChangeMonth = viewModel::changeMonth,
            onToday = viewModel::goToToday,
            onRetry = viewModel::retry,
            onReminderClick = { navController.navigate(Screen.Edit.createRoute(it)) }
        )
    }
}
