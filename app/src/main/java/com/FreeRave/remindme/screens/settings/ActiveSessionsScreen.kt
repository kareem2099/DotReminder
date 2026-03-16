package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.ui.layout.AdaptiveContainer
import com.FreeRave.remindme.ui.responsive.getResponsivePadding
import com.FreeRave.remindme.ui.responsive.getResponsiveSpacing
import com.FreeRave.remindme.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(
    navController: NavController,
    viewModel: ActiveSessionsViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Active Sessions", style = AppTypography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { _, containerModifier ->
            Column(
                modifier = containerModifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = getResponsivePadding(16.dp))
            ) {
                Text(
                    "Devices currently signed into your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = getResponsiveSpacing(8.dp))
                )

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp))
                    ) {
                        items(sessions, key = { it.sessionId }) { session ->
                            SessionCard(
                                session = session,
                                isCurrentSession = session.sessionId == currentSessionId,
                                onSignOut = { viewModel.signOutSession(session.sessionId) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(getResponsiveSpacing(12.dp)))

                    Button(
                        onClick = { viewModel.signOutAllOtherSessions() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sessions.size > 1
                    ) {
                        Text("Sign Out All Other Devices")
                    }

                    Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    isCurrentSession: Boolean,
    onSignOut: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSession)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(getResponsivePadding(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (session.deviceModel.contains("iPad", true) ||
                        session.deviceModel.contains("Tab", true))
                        Icons.Filled.Computer else Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(getResponsiveSpacing(12.dp)))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.deviceName, style = MaterialTheme.typography.titleSmall)
                        if (isCurrentSession) {
                            Spacer(Modifier.width(6.dp))
                            Badge { Text("This device") }
                        }
                    }
                    Text(
                        "Login: ${dateFormat.format(session.loginTime.toDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Last active: ${dateFormat.format(session.lastActive.toDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isCurrentSession) {
                TextButton(onClick = onSignOut) { Text("Sign Out") }
            }
        }
    }
}