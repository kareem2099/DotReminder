package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R // Assuming you'll add a Google icon drawable

@Composable
fun GoogleSignInButton(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSignInClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
    ) {
        // It's better to use an actual Google icon from your resources
        // For now, using a placeholder or assuming you have R.drawable.ic_google_logo
        // If you don't have one, you can download the official Google sign-in button asset.
        Icon(
            painter = painterResource(id = R.drawable.ic_google_logo), // Replace with your actual Google icon
            contentDescription = "Google Sign-In",
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified // Important if your icon has its own colors
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text("Sign in with Google")
    }
}
