package com.FreeRave.remindme.screens.edit

import android.util.Log
import androidx.compose.foundation.layout.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.components.TextFieldInput
import androidx.compose.material.icons.Icons // Added for Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added for ArrowBack
import androidx.compose.foundation.rememberScrollState // Added for scroll
import androidx.compose.foundation.verticalScroll // Added for scroll
import com.FreeRave.remindme.utils.CategoryUtils
import com.FreeRave.remindme.screens.category.CategoryViewModel
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

@OptIn(ExperimentalMaterial3Api::class) // Added for Scaffold/TopAppBar
@Composable
fun EditReminderScreen(
    navController: NavController,
    reminderId: String,
    viewModel: EditReminderViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState() // Added for scrolling content
    val allCategories by categoryViewModel.allCategories.collectAsState()

    LaunchedEffect(reminderId) {
        Log.d("EditReminderScreen", "Loading reminder with ID: $reminderId")
        if (reminderId.isBlank()) {
            Log.e("EditReminderScreen", "Received blank reminder ID!")
        }
        viewModel.loadReminder(reminderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { windowSizeClass, containerModifier ->
            AdaptiveColumn(
                modifier = containerModifier.padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                viewModel.reminder.value?.let { reminder ->
                    Text(
                        text = "Reminder ID: ${reminder.customId}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = getResponsiveSpacing(8.dp))
                    )
                }

                AdaptiveFormContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(16.dp))
                    ) {
                        TextFieldInput(
                            value = title,
                            onValueChange = { title = it },
                            label = "Title",
                            placeholder = "Enter reminder title"
                        )

                        TextFieldInput(
                            value = description,
                            onValueChange = { description = it },
                            label = "Description",
                            placeholder = "Enter reminder description",
                            singleLine = false,
                            modifier = Modifier.heightIn(min = getResponsiveSpacing(80.dp))
                        )

                        // Category Dropdown
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedCategory = allCategories.find { it.name == category }
                            OutlinedTextField(
                                value = if (category.isEmpty()) "Select Category" else "${selectedCategory?.icon ?: CategoryUtils.getCategoryIcon(category)} $category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category (Optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                allCategories.forEach { categoryItem ->
                                    DropdownMenuItem(
                                        text = { Text("${categoryItem.icon} ${categoryItem.name}") },
                                        onClick = {
                                            category = categoryItem.name
                                            expanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        category = ""
                                        expanded = false
                                    }
                                )
                            }
                        }

                        DateTimePickerSection(
                            initialTime = viewModel.reminder.value?.reminderTime ?: System.currentTimeMillis(),
                            onTimeSelected = { newTime ->
                                viewModel.updateReminderTime(newTime)
                            }
                        )

                        if (isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        PrimaryButton(
                            text = "Save Changes",
                            onClick = {
                                if (title.isNotBlank()) {
                                    isLoading = true
                                    Log.d("EditReminderScreen", "Save button clicked")
                                    val currentReminder = viewModel.reminder.value
                                    if (currentReminder == null) {
                                        Log.e("EditReminderScreen", "Current reminder is null! Cannot save.")
                                        isLoading = false
                                        return@PrimaryButton
                                    }

                                    Log.d("EditReminderScreen", "Calling updateReminder with title: $title, desc: $description, time: ${currentReminder.reminderTime}")
                                    viewModel.updateReminder(
                                        title = title,
                                        description = description,
                                        reminderTime = currentReminder.reminderTime,
                                        category = category,
                                        onComplete = { success ->
                                            isLoading = false
                                            Log.d("EditReminderScreen", "Update completed with success: $success")
                                            if (success) {
                                                navController.popBackStack()
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                    }
                }

                LaunchedEffect(viewModel.reminder.value) {
                    viewModel.reminder.value?.let { reminder ->
                        if (title != reminder.title) title = reminder.title
                        if (description != reminder.description) description = reminder.description
                        if (category != reminder.category) category = reminder.category
                    }
                }
            }
        }
    }
}

@Composable
fun DateTimePickerSection(
    initialTime: Long,
    onTimeSelected: (Long) -> Unit
) {
    var showDateTimePicker by remember { mutableStateOf(false) }
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault()) }
    val dateTimeText = dateTimeFormatter.format(Date(initialTime))
    val context = LocalContext.current

    Column {
        Button(
            onClick = { showDateTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Date & Time: $dateTimeText")
        }

        LaunchedEffect(showDateTimePicker) {
            if (showDateTimePicker) {
                val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        val timePickerDialog = android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                onTimeSelected(calendar.timeInMillis)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        )
                        timePickerDialog.show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
                showDateTimePicker = false
            }
        }
    }
}
