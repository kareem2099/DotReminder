package com.FreeRave.remindme.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreen(
    navController: NavController,
    authService: FirebaseAuthServiceInterface
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AdaptiveContainer { windowSizeClass, containerModifier ->
        Column(
            modifier = containerModifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineSmall
            )

            AdaptiveSpacer(getResponsiveSpacing(16.dp))

            if (isVerified) {
                Text("Email verified successfully!")
                AdaptiveSpacer(getResponsiveSpacing(16.dp))
                PrimaryButton(
                    text = "Continue to Login",
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                        }
                    }
                )
            } else {
                Text("Please check your email for a verification link")
                AdaptiveSpacer(getResponsiveSpacing(16.dp))

                PrimaryButton(
                    text = if (isLoading) "Checking..." else "Check Verification",
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                authService.checkEmailVerification(
                                    onVerified = {
                                        isVerified = true
                                        isLoading = false
                                        // Update user's email verification status
                                        coroutineScope.launch {
                                            authService.updateEmailVerificationStatus(true)
                                        }
                                    },
                                    onNotVerified = {
                                        errorMessage = "Email not verified yet"
                                        isLoading = false
                                    },
                                    onFailure = { e ->
                                        errorMessage = "Error: ${e.message}"
                                        isLoading = false
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                )

                errorMessage?.let {
                    AdaptiveSpacer(getResponsiveSpacing(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
