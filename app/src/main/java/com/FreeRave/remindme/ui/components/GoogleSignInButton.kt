package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R

@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(onClick = onSignInClick, enabled = enabled, modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF202124))) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(painterResource(R.drawable.ic_google_logo), null, Modifier.size(22.dp), tint = Color.Unspecified)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(if (loading) R.string.login_google_loading else R.string.login_google),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
