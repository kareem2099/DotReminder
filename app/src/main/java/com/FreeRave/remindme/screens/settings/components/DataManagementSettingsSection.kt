package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DataManagementSettingsSection(
    isDeleting: Boolean,
    onClearAllRemindersClicked: () -> Unit
) {
    Column {
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Button(
            onClick = onClearAllRemindersClicked,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDeleting
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Clear All Reminders")
            }
        }
        // Note: No HorizontalDivider here as it's the last content section before the Sign Out button in the original layout.
        // If more sections are added below this in SettingsScreen's main column, a divider might be needed.
    }
}
