package com.FreeRave.remindme.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color as ComposeColor
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val allCategories by viewModel.allCategories.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var selectedCategoryForEdit by remember { mutableStateOf<com.FreeRave.remindme.model.Category?>(null) }
    var selectedCategoryForDelete by remember { mutableStateOf<com.FreeRave.remindme.model.Category?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showAddDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showAddDialog = true }) {
                        Text("+")
                    }
                    if (showAddDialog) {
                        CategoryDialog(
                            onDismiss = { showAddDialog = false },
                            onConfirm = { name, colorHex, description, icon ->
                                coroutineScope.launch {
                                    try {
                                        val success = viewModel.addCategory(name, colorHex, description, icon)
                                        if (success) {
                                            showAddDialog = false
                                        } else {
                                            errorMessage = "Failed to add category. Please try again."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error adding category: ${e.localizedMessage ?: "Unknown error"}"
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { _, containerModifier ->
            // ✅ LazyColumn directly instead of AdaptiveColumn to avoid nested scroll
            LazyColumn(
                modifier = containerModifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(getResponsivePadding(16.dp)),
                verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(12.dp))
            ) {
                items(allCategories) { category ->
                    ResponsiveCategoryItem(
                        category = category,
                        onEditClick = { selectedCategoryForEdit = category },
                        onDeleteClick = { selectedCategoryForDelete = category },
                        categoryColors = viewModel.categoryColors
                    )
                }
            }
        }
    }

    // Edit Dialog
    selectedCategoryForEdit?.let { cat ->
        CategoryDialog(
            category = cat,
            onDismiss = { selectedCategoryForEdit = null },
            onConfirm = { name, colorHex, description, icon ->
                coroutineScope.launch {
                    try {
                        val success = if (cat.isPredefined) {
                            viewModel.addCategory(name, colorHex, description, icon)
                        } else {
                            viewModel.updateCategory(
                                cat.copy(
                                    name = name,
                                    colorHex = colorHex,
                                    description = description,
                                    icon = icon,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                        if (success) {
                            selectedCategoryForEdit = null
                        } else {
                            errorMessage = "Failed to update category. Please try again."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error updating category: ${e.localizedMessage ?: "Unknown error"}"
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    selectedCategoryForDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { selectedCategoryForDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${cat.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            val success = viewModel.deleteCategory(cat)
                            if (!success) {
                                errorMessage = "Failed to delete category. Please try again."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error deleting category: ${e.localizedMessage ?: "Unknown error"}"
                        }
                        selectedCategoryForDelete = null
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForDelete = null }) {
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
fun ResponsiveCategoryItem(
    category: com.FreeRave.remindme.model.Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    categoryColors: Map<String, androidx.compose.ui.graphics.Color>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(getResponsivePadding(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(getResponsiveSpacing(24.dp))
                    .background(
                        color = categoryColors[category.name] ?: ComposeColor.Gray,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(getResponsiveSpacing(12.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall
                )
                if (category.description.isNotBlank()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (category.isPredefined) "Predefined" else "Custom",
                    style = MaterialTheme.typography.labelSmall,
                    color = ComposeColor.Gray
                )
                Spacer(modifier = Modifier.width(getResponsiveSpacing(8.dp)))
                if (!category.isPredefined) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onDeleteClick() }
                    )
                    Spacer(modifier = Modifier.width(getResponsiveSpacing(8.dp)))
                }
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onEditClick() }
                )
            }
        }
    }
}

@Composable
fun CategoryDialog(
    viewModel: CategoryViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    category: com.FreeRave.remindme.model.Category? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#FF42A5F5") }
    var icon by remember { mutableStateOf(category?.icon ?: "📁") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            AdaptiveFormContainer {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { colorHex = it },
                        label = { Text("Color (Hex)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("Icon (Emoji)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton
                    isValidating = true
                    coroutineScope.launch {
                        val isValid = viewModel.validateCategoryName(name, category?.id)
                        isValidating = false
                        if (isValid) {
                            onConfirm(name, colorHex, description, icon)
                        } else {
                            nameError = "Category name already exists"
                        }
                    }
                },
                enabled = !isValidating
            ) {
                if (isValidating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}