package com.FreeRave.remindme.ui.components

import androidx.compose.material3.Text // Ensuring this import is present
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SignUpNavigationLink(
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onSignUpClick, modifier = modifier) {
        Text("Don't have an account? Sign up")
    }
}
