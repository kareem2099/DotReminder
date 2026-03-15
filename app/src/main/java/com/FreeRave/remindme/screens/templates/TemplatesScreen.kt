package com.FreeRave.remindme.screens.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.ui.theme.AppTypography
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import com.FreeRave.remindme.utils.CategoryUtils
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    navController: NavController,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTemplateForEdit by remember { mutableStateOf<ReminderTemplate?>(null) }
    var selectedTemplateForDelete by remember { mutableStateOf<ReminderTemplate?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder Templates") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        enabled = isAuthenticated
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Template")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { windowSizeClass, containerModifier ->
            Box(
                modifier = containerModifier.padding(paddingValues).fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    templates.isEmpty() -> {
                        ResponsiveEmptyState(
                            onAddClick = { showAddDialog = true },
                            isAuthenticated = isAuthenticated
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(getResponsivePadding(16.dp)),
                            verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(12.dp))
                        ) {
                            items(templates) { template ->
                                ResponsiveTemplateItem(
                                    template = template,
                                    onTemplateClick = {
                                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_template", template)
                                        navController.popBackStack()
                                    },
                                    onEdit = { selectedTemplateForEdit = template },
                                    onDelete = { selectedTemplateForDelete = template }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Template Dialog
    if (showAddDialog) {
        ResponsiveTemplateDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { template ->
                viewModel.addTemplate(template) { success ->
                    if (success) {
                        showAddDialog = false
                    } else {
                        errorMessage = "Failed to add template. Please try again."
                    }
                }
            }
        )
    }

    // Edit Template Dialog
    selectedTemplateForEdit?.let { template ->
        ResponsiveTemplateDialog(
            template = template,
            onDismiss = { selectedTemplateForEdit = null },
            onConfirm = { updatedTemplate ->
                viewModel.updateTemplate(updatedTemplate) { success ->
                    if (success) {
                        selectedTemplateForEdit = null
                    } else {
                        errorMessage = "Failed to update template. Please try again."
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    selectedTemplateForDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { selectedTemplateForDelete = null },
            title = { Text("Delete Template") },
            text = { Text("Are you sure you want to delete '${template.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(template.id) { success ->
                            if (success) {
                                selectedTemplateForDelete = null
                            } else {
                                errorMessage = "Failed to delete template. Please try again."
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTemplateForDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Dialog
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ResponsiveEmptyState(onAddClick: () -> Unit, isAuthenticated: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.padding(getResponsivePadding(32.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No templates yet",
                style = AppTypography.headlineMedium
            )
            Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
            Text(
                text = "Create templates to quickly add common reminders",
                style = AppTypography.bodyLarge,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(getResponsiveSpacing(24.dp)))
            Button(
                onClick = onAddClick,
                enabled = isAuthenticated
            ) {
                Text("Create First Template")
            }
        }
    }
}

@Composable
fun ResponsiveTemplateItem(
    template: ReminderTemplate,
    onTemplateClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTemplateClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(getResponsivePadding(16.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.name,
                    style = AppTypography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    Text(
                        "Edit",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onEdit() }
                            .padding(horizontal = getResponsiveSpacing(8.dp), vertical = getResponsiveSpacing(4.dp))
                    )
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(horizontal = getResponsiveSpacing(8.dp), vertical = getResponsiveSpacing(4.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))

            Text(text = template.title, style = AppTypography.bodyLarge)

            if (template.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(getResponsiveSpacing(4.dp)))
                Text(
                    text = template.description,
                    style = AppTypography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))

            Row(horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp))) {
                if (template.isRecurring) {
                    val intervalText = formatInterval(template.recurrenceIntervalMillis ?: 0)
                    Text(
                        text = "Recurring: $intervalText",
                        style = AppTypography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = getResponsiveSpacing(6.dp), vertical = getResponsiveSpacing(2.dp))
                    )
                }

                if (template.isLocationBased) {
                    Text(
                        text = "Location-based",
                        style = AppTypography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = getResponsiveSpacing(6.dp), vertical = getResponsiveSpacing(2.dp))
                    )
                }

                if (template.category.isNotBlank()) {
                    Text(
                        text = template.category,
                        style = AppTypography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .background(CategoryUtils.getCategoryColor(template.category) ?: Color.Gray, RoundedCornerShape(4.dp))
                            .padding(horizontal = getResponsiveSpacing(6.dp), vertical = getResponsiveSpacing(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun ResponsiveTemplateDialog(
    template: ReminderTemplate? = null,
    onDismiss: () -> Unit,
    onConfirm: (ReminderTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var title by remember { mutableStateOf(template?.title ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var category by remember { mutableStateOf(template?.category ?: "") }
    var isRecurring by remember { mutableStateOf(template?.isRecurring ?: false) }
    var recurrenceIntervalMillis by remember { mutableStateOf(template?.recurrenceIntervalMillis) }
    var isLocationBased by remember { mutableStateOf(template?.isLocationBased ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "Create Template" else "Edit Template") },
        text = {
            AdaptiveFormContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp))
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Default Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Default Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Default Category") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                        Text("Recurring reminder")
                    }

                    if (isRecurring) {
                        OutlinedTextField(
                            value = recurrenceIntervalMillis?.toString() ?: "",
                            onValueChange = { recurrenceIntervalMillis = it.toLongOrNull() },
                            label = { Text("Interval (milliseconds)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isLocationBased, onCheckedChange = { isLocationBased = it })
                        Text("Location-based reminder")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && title.isNotBlank()) {
                        val newTemplate = ReminderTemplate(
                            id = template?.id ?: "",
                            userId = template?.userId ?: "",
                            name = name,
                            title = title,
                            description = description,
                            category = category,
                            isRecurring = isRecurring,
                            recurrenceIntervalMillis = recurrenceIntervalMillis,
                            isLocationBased = isLocationBased
                        )
                        onConfirm(newTemplate)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatInterval(millis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

    return buildString {
        if (days > 0) append("$days day(s) ")
        if (hours > 0) append("$hours hour(s) ")
        if (minutes > 0) append("$minutes minute(s) ")
    }.trim().ifEmpty { "$millis ms" }
}
