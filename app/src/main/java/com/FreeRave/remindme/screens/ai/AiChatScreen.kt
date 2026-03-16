package com.FreeRave.remindme.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.MainViewModel
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.layout.AdaptiveContainer
import com.FreeRave.remindme.ui.responsive.getResponsivePadding
import com.FreeRave.remindme.ui.responsive.getResponsiveSpacing
import com.FreeRave.remindme.ui.theme.OceanBlueBackground
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.layout.imePadding

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: SenderType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SenderType {
    USER, AI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    navController: NavController,
    viewModel: AiViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val userInput by viewModel.userInput.collectAsState()
    val chatMessages = viewModel.chatMessages
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTheme by mainViewModel.currentTheme.collectAsStateWithLifecycle()
    val isSystemDarkTheme = isSystemInDarkTheme()
    val resolvedUseDarkTheme = remember(currentTheme, isSystemDarkTheme) {
        when (currentTheme) {
            ThemeSetting.LIGHT -> false
            ThemeSetting.DARK -> true
            else -> isSystemDarkTheme
        }
    }

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

    OceanBlueBackground(
        themeSetting = currentTheme,
        useDarkTheme = resolvedUseDarkTheme
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("AI Assistant") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent // Transparent AppBar
                    ),
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) // Light background for bottom bar
                        .navigationBarsPadding()
                ) {
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { viewModel.onUserInputChange(it) },
                            placeholder = { Text("Ask something...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = { viewModel.sendMessage() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        ) { paddingValues ->
            AdaptiveContainer { _, containerModifier ->
                LazyColumn(
                    modifier = containerModifier
                        .padding(paddingValues)
                        .imePadding()
                        .padding(horizontal = getResponsivePadding(16.dp), vertical = getResponsivePadding(8.dp)),
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

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.sender == SenderType.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 40.dp else 0.dp,
                end = if (isUser) 0.dp else 40.dp
            ),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .clip(shape)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}