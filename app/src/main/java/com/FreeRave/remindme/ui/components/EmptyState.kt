package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.theme.AppTypography

@Composable
fun EmptyRemindersState(
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Using app icon as placeholder
            contentDescription = "No reminders illustration",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp)
        )

        // Title
        Text(
            text = "No reminders yet",
            style = AppTypography.displayLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Create your first reminder to stay organized and never miss important tasks",
            style = AppTypography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Add Reminder Button
        PrimaryButton(
            text = "Create Your First Reminder",
            onClick = onAddReminder,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
fun NoSearchResultsState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Using app icon as placeholder
            contentDescription = "No search results illustration",
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 24.dp)
        )

        // Title
        Text(
            text = "No reminders found",
            style = AppTypography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "We couldn't find any reminders matching \"$searchQuery\"",
            style = AppTypography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Clear Search Button
        OutlinedButton(
            onClick = onClearSearch,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(
                text = "Clear Search",
                style = AppTypography.labelLarge
            )
        }
    }
}
