package com.FreeRave.remindme.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding 
import androidx.compose.foundation.layout.navigationBarsPadding 
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box // Keep Box for ChatMessageItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Removed many animation/layout imports that are now in AiIntroAnimation.kt
// Keep specific ones if still needed by ChatMessageItem or Scaffold structure.
import androidx.compose.foundation.shape.RoundedCornerShape // For ChatMessageItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // For ChatMessageItem
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
// Removed kotlinx.coroutines.delay and launch as they are in AiIntroAnimation
import androidx.navigation.NavController
import com.FreeRave.remindme.ui.layout.AdaptiveContainer
import com.FreeRave.remindme.ui.responsive.getResponsivePadding
import com.FreeRave.remindme.ui.responsive.getResponsiveSpacing

// Placeholder data class for chat messages
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: SenderType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SenderType {
    USER, AI
}

// @SuppressLint("UnusedAutoMirrored") // Removed to avoid Kapt error
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    navController: NavController,
    viewModel: AiViewModel = hiltViewModel()
) {
    val userInput by viewModel.userInput.collectAsState()
    val chatMessages = viewModel.chatMessages
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showIntro by remember { mutableStateOf(true) }

    // Collect effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.actionEffect.collectLatest { effect ->
            when (effect) {
                is AiActionEffect.NavigateToAddReminder -> {
                    snackbarHostState.showSnackbar("Navigate to Add with: ${effect.title}")
                }
                is AiActionEffect.NavigateToEditReminder -> {
                    snackbarHostState.showSnackbar("Navigate to Edit: ${effect.reminderId}")
                }
                is AiActionEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    if (showIntro) {
        AiIntroAnimation {
            showIntro = false
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("AI Assistant") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { viewModel.onUserInputChange(it) },
                            label = { Text("Ask something...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = userInput.isNotBlank() && !isLoading
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send message")
                        }
                    }
                    if (isLoading && chatMessages.lastOrNull()?.text == "Thinking...") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                    }
                }
            }
        ) { paddingValues ->
        AdaptiveContainer { windowSizeClass, containerModifier ->
            LazyColumn(
                modifier = containerModifier
                    .padding(paddingValues)
                    .imePadding() // Apply IME padding here to adjust content above keyboard
                    .padding(horizontal = getResponsivePadding(8.dp), vertical = getResponsivePadding(8.dp)),
                reverseLayout = true
            ) {
                items(chatMessages.toList().reversed()) { message ->
                    ChatMessageItem(message = message)
                    Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                }
            }
        }
    }
    }
}

// AnimationStage enum is now in AiIntroAnimation.kt

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.sender == SenderType.USER) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.sender == SenderType.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.sender == SenderType.USER) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.sender == SenderType.AI) 0.dp else 40.dp,
                end = if (message.sender == SenderType.USER) 0.dp else 40.dp
            ),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
