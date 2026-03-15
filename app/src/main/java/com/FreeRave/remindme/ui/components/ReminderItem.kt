package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.ui.theme.AppTypography

@Composable
fun ReminderItem(
    reminder: Reminder,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = reminder.title,
                style = AppTypography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reminder.description,
                style = AppTypography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}
