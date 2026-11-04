package com.FreeRave.remindme.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun HomeActions(onAdd: () -> Unit, onAssistant: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassSurface(shape = RoundedCornerShape(20.dp)) {
            IconButton(onClick = onAssistant, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.AutoAwesome, stringResource(R.string.home_ai_assistant),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale > 1.3f) {
            FloatingActionButton(
                onClick = onAdd,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.home_new_reminder))
            }
        } else ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(stringResource(R.string.home_new_reminder)) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
        )
    }
}
