package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.screens.settings.components.SessionRecordCard
import com.FreeRave.remindme.screens.settings.components.SessionsStatusCard
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveSessionsScreenContent(
    state: ActiveSessionsUiState, snackbar: SnackbarHostState,
    onBack: () -> Unit, onRetry: () -> Unit, onRemove: (Session) -> Unit, onClearOthers: () -> Unit
) {
    val current = state.currentSession
    val others = state.otherSessions
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(), title = { Text(stringResource(R.string.sessions_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back)) }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            },
            bottomBar = {
                if (!state.isLoading && state.sessions.isNotEmpty() && others.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = onClearOthers, enabled = state.canClearOthers,
                                shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 56.dp)) {
                                Text(stringResource(R.string.sessions_clear_others), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item(key = "intro") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.sessions_intro), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!state.isLoading && !state.hasError) Text(
                                pluralStringResource(R.plurals.sessions_count, state.sessions.size, state.sessions.size),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "scope") {
                        GlassSurface(tint = MaterialTheme.colorScheme.primaryContainer) {
                            Text(stringResource(R.string.sessions_scope), modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (state.isLoading || state.hasError || state.sessions.isEmpty()) item(key = "status") {
                        SessionsStatusCard(state.isLoading, state.hasError, onRetry)
                    }
                    if (!state.isLoading && state.identityLoaded && state.sessions.isNotEmpty() && current == null) item(key = "identity") {
                        Text(stringResource(R.string.sessions_identity_missing), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (current != null) item(key = "current:${current.sessionId}") {
                        SessionRecordCard(current, current = true, removalEnabled = false, onRemove = {})
                    }
                    if (others.isNotEmpty()) {
                        item(key = "others-title") {
                            Text(stringResource(if (current == null) R.string.sessions_saved else R.string.sessions_other),
                                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(others, key = { "record:${it.sessionId}" }) { session ->
                            SessionRecordCard(session, current = false, removalEnabled = state.canRemoveSession(session.sessionId),
                                onRemove = { onRemove(session) })
                        }
                    } else if (current != null && !state.isLoading && !state.hasError) item(key = "only-current") {
                        Text(stringResource(R.string.sessions_only_current), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}
