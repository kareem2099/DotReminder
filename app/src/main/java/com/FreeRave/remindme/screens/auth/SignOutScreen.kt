package com.FreeRave.remindme.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

@Composable
fun SignOutScreen(
    navController: NavController
) {
    val viewModel: SignOutViewModel = hiltViewModel()
    val signOutComplete by viewModel.signOutComplete.collectAsStateWithLifecycle()

    LaunchedEffect(signOutComplete) {
        if (signOutComplete) {
            navController.navigate(Screen.Login.route) {
                // Clear the entire back stack to prevent navigating back to the app
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                // Ensure this is the only instance of the login screen
                launchSingleTop = true
            }
        }
    }

    AdaptiveContainer { windowSizeClass, containerModifier ->
        Column(
            modifier = containerModifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Are you sure you want to sign out?",
                style = MaterialTheme.typography.headlineSmall
            )

            AdaptiveSpacer(getResponsiveSpacing(24.dp))

            PrimaryButton(
                text = "Sign Out",
                onClick = {
                    viewModel.signOut()
                },
                modifier = Modifier.fillMaxWidth()
            )

            AdaptiveSpacer(getResponsiveSpacing(8.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
