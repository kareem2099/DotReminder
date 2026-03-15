package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SupportDeveloperSection(
    onSupportClicked: () -> Unit
) {
    Column {
        Text(
            text = "Support",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        SettingItem(
            title = "Support Developer",
            currentValue = "View ads to help",
            onClick = onSupportClicked
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
