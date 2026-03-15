package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Changed for AutoMirrored
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.FreeRave.remindme.ui.theme.AppTypography
import kotlinx.coroutines.launch
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

data class MockSession(
    val id: String,
    val deviceName: String,
    val location: String,
    val lastActive: String,
    val isCurrentSession: Boolean = false,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(navController: NavController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Mock data
    val mockSessions = remember {
        listOf(
            MockSession("1", "Pixel 7 Pro (This Device)", "Cairo, Egypt", "Active now", true, Icons.Filled.PhoneAndroid),
            MockSession("2", "Chrome on Windows", "Alexandria, Egypt", "Yesterday, 10:30 PM", false, Icons.Filled.Computer),
            MockSession("3", "iPhone 14", "Giza, Egypt", "May 15, 2025", false, Icons.Filled.PhoneAndroid)
        )
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
        AdaptiveContainer { windowSizeClass, containerModifier ->
            AdaptiveColumn(
                modifier = containerModifier.padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Descriptive text
                Text(
                    "These are the devices currently signed into your account. If you don't recognize a session, you can sign it out.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = getResponsiveSpacing(8.dp))
                )

                // Sessions list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(4.dp)),
                    modifier = Modifier.weight(1f)
                ) {
                    items(mockSessions) { session ->
                        ResponsiveSessionCard(
                            session = session,
                            onSignOut = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Session for ${session.deviceName} signed out (mocked).")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(getResponsiveSpacing(12.dp)))

                // Sign out all other sessions button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Signed out all other sessions (mocked).")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out All Other Sessions")
                }

                Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
            }
        }
    }
}

@Composable
fun ResponsiveSessionCard(
    session: MockSession,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(getResponsivePadding(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    session.icon,
                    contentDescription = null,
                    modifier = Modifier.size(getResponsiveSpacing(12.dp))
                )
                Spacer(modifier = Modifier.width(getResponsiveSpacing(8.dp)))
                Column {
                    Text(
                        session.deviceName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        session.location,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        session.lastActive,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!session.isCurrentSession) {
                TextButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
            }
        }
    }
}
