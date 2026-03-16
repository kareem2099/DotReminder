package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.ButtonDefaults 
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

// PrimaryButton, GoogleSignInButton, and FacebookSignInButton are in the same package
// and should be resolved without explicit imports.

@Composable
fun LoginActions(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    isLoginButtonEnabled: Boolean,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    onBiometricLoginClick: () -> Unit,
    isBiometricAuthPossible: Boolean
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(8.dp)) // Add a small spacer when loading indicator is visible
        }

        PrimaryButton(
            text = if (isLoading) "Logging in..." else "Login",
            modifier = Modifier.fillMaxWidth().scale(
                if (isLoading) 0.98f else 1f
            ),
            enabled = isLoginButtonEnabled && !isLoading,
            onClick = onLoginClick
        )

        Spacer(Modifier.height(12.dp))

        GoogleSignInButton(
            onSignInClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        FacebookSignInButton(
            onSignInClick = onFacebookSignInClick,
            modifier = Modifier.fillMaxWidth()
        )

        if (isBiometricAuthPossible) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBiometricLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = "Login with Biometrics",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Login with Biometrics")
            }
        }
    }
}
