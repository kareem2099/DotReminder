package com.FreeRave.remindme.screens.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.screens.ai.components.*
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiChatScreenContent(
    state: AiChatUiState, onBack: () -> Unit, onInput: (String) -> Unit, onSend: () -> Unit,
    onRetry: () -> Unit, onClear: () -> Unit, onReview: (AiReminderDraft) -> Unit, onManual: () -> Unit,
    onOpen: (String) -> Unit = {}
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showClear by rememberSaveable { mutableStateOf(false) }
    val lastItem = state.messages.size + (if (state.busy) 1 else 0) + (if (state.error != null) 1 else 0) - 1
    LaunchedEffect(state.messages.size, state.busy, state.error) {
        if (lastItem >= 0 && (state.busy && state.messages.lastOrNull()?.sender == SenderType.USER || !list.canScrollForward)) {
            list.animateScrollToItem(lastItem)
        }
    }
    GlassBackground {
        Scaffold(modifier = Modifier.imePadding(), containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(subtitle = true), title = {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.ai_chat_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.ai_chat_subtitle), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }, navigationIcon = {
                    GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.ai_chat_back)) }
                    }
                }, actions = {
                    IconButton(onClick = { showClear = true }, enabled = state.messages.isNotEmpty() && !state.busy) {
                        Icon(Icons.Outlined.AddComment, stringResource(R.string.ai_chat_new))
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }, bottomBar = { AiChatComposer(state, onInput, onSend) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 800.dp).fillMaxSize().testTag("ai-chat-messages"), state = list,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (state.messages.isEmpty()) item(key = "welcome") { AiChatWelcome(onInput) }
                    items(state.messages, key = { it.id }) { AiChatMessage(it, onReview, onOpen) }
                    if (state.busy) item(key = "thinking") { AiChatThinking() }
                    state.error?.let { error -> item(key = "error") { AiChatErrorCard(error, state.canRetry, onRetry, onManual) } }
                }
                if (state.messages.isNotEmpty() && list.canScrollForward) {
                    FilledTonalButton(onClick = { scope.launch { list.animateScrollToItem(lastItem.coerceAtLeast(0)) } },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)) {
                        Icon(Icons.Outlined.ArrowDownward, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ai_chat_latest))
                    }
                }
            }
        }
        if (showClear) AlertDialog(onDismissRequest = { showClear = false },
            title = { Text(stringResource(R.string.ai_chat_clear_title)) }, text = { Text(stringResource(R.string.ai_chat_clear_intro)) },
            confirmButton = { TextButton(onClick = { showClear = false; onClear() }, enabled = !state.busy) { Text(stringResource(R.string.ai_chat_clear)) } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text(stringResource(R.string.ai_chat_keep)) } })
    }
}
