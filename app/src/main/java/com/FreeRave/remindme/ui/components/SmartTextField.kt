package com.FreeRave.remindme.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.utils.SmartSuggestionEngine
import com.FreeRave.remindme.utils.SuggestionResult
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    smartSuggestions: Boolean = true
) {
    val suggestionEngine = remember { SmartSuggestionEngine() }
    var suggestions by remember { mutableStateOf<SuggestionResult?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(value) {
        if (smartSuggestions && value.isNotBlank()) {
            isLoading = true
            delay(300) // Wait for user to stop typing
            suggestions = suggestionEngine.analyzeText(value)
            isLoading = false
        } else {
            suggestions = null
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                showSuggestions = smartSuggestions && newValue.isNotBlank()
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        showSuggestions = false
                    }
                },
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    showSuggestions = false
                }
            ),
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        // Smart Suggestions Dropdown
        AnimatedVisibility(
            visible = showSuggestions && suggestions != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SmartSuggestionsDropdown(
                suggestions = suggestions!!,
                onSuggestionSelected = { suggestion ->
                    onValueChange(suggestion)
                    showSuggestions = false
                    focusManager.clearFocus()
                },
                onDismiss = { showSuggestions = false }
            )
        }
    }
}

@Composable
private fun SmartSuggestionsDropdown(
    suggestions: SuggestionResult,
    onSuggestionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            // Title Suggestions
            if (suggestions.titleSuggestions.isNotEmpty()) {
                item {
                    SuggestionSectionHeader("💡 Smart Titles")
                }
                items(suggestions.titleSuggestions) { suggestion ->
                    SuggestionItem(
                        text = suggestion,
                        icon = Icons.Default.Lightbulb,
                        onClick = { onSuggestionSelected(suggestion) }
                    )
                }
            }

            // Description Suggestions
            if (suggestions.descriptionSuggestions.isNotEmpty()) {
                item {
                    SuggestionSectionHeader("📝 Description Ideas")
                }
                items(suggestions.descriptionSuggestions) { suggestion ->
                    SuggestionItem(
                        text = suggestion,
                        icon = Icons.Default.Description,
                        onClick = { onSuggestionSelected(suggestion) }
                    )
                }
            }

            // Time Suggestions
            if (suggestions.timeSuggestions.isNotEmpty()) {
                item {
                    SuggestionSectionHeader("⏰ Suggested Times")
                }
                items(suggestions.timeSuggestions) { suggestion ->
                    SuggestionItem(
                        text = "${suggestion.displayText} - ${suggestion.context}",
                        icon = Icons.Default.Schedule,
                        onClick = { /* Time suggestions would be handled differently */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SuggestionItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Use suggestion",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
