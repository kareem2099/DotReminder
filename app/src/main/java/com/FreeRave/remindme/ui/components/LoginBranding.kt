package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R

@Composable
fun LoginBranding(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassSurface(shape = RoundedCornerShape(24.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp).size(32.dp))
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(stringResource(R.string.login_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
