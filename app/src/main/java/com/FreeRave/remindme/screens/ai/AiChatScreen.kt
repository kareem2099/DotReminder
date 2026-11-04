package com.FreeRave.remindme.screens.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.model.AiReminderSaveResult
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.theme.GlassTheme

internal const val AI_REMINDER_DRAFT_KEY = "ai_reminder_draft"
internal const val AI_REMINDER_RESULT_KEY = "ai_reminder_result"
internal const val AI_SOURCE_DRAFT_KEY = "ai_source_draft"

@Composable
fun AiChatScreen(navController: NavController, backStackEntry: NavBackStackEntry, viewModel: AiViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val handle = backStackEntry.savedStateHandle
    val resultFlow = remember(handle) { handle.getStateFlow<AiReminderSaveResult?>(AI_REMINDER_RESULT_KEY, null) }
    val result by resultFlow.collectAsStateWithLifecycle()
    LaunchedEffect(result) {
        result?.let {
            viewModel.markSaved(it.draftId, it.reminderId)
            handle.set<AiReminderSaveResult?>(AI_REMINDER_RESULT_KEY, null)
        }
    }
    GlassTheme {
        AiChatScreenContent(state, onBack = { navController.popBackStack() }, onInput = viewModel::onUserInputChange,
            onSend = viewModel::sendMessage, onRetry = viewModel::retry, onClear = viewModel::clearConversation,
            onReview = { openAiReminderForm(navController, it) }, onManual = { openAiReminderForm(navController, null) },
            onOpen = { id -> if (navController.currentDestination?.route == Screen.AiChat.route) navController.navigate(Screen.Edit.createRoute(id)) })
    }
}

internal fun openAiReminderForm(navController: NavController, draft: AiReminderDraft?) {
    if (navController.currentDestination?.route != Screen.AiChat.route) return
    navController.navigate(Screen.Add.route) { launchSingleTop = true }
    draft?.let {
        val handle = navController.getBackStackEntry(Screen.Add.route).savedStateHandle
        handle[AI_REMINDER_DRAFT_KEY] = it
        handle[AI_SOURCE_DRAFT_KEY] = it.id
    }
}
