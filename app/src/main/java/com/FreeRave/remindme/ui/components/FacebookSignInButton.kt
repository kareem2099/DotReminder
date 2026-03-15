package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun FacebookSignInButton(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSignInClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)) // Facebook Blue
    ) {
        // If you have a Facebook icon in drawable, you can use it like this:
        // Icon(
        //     painter = painterResource(id = R.drawable.ic_facebook_logo), // Replace with actual resource ID
        //     contentDescription = "Facebook Logo",
        //     tint = Color.White
        // )
        // Spacer(Modifier.width(8.dp)) // Add space between icon and text if icon is present
        Text("Sign in with Facebook", color = Color.White)
    }
}
