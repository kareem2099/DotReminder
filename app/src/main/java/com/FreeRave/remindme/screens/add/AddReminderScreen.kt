package com.FreeRave.remindme.screens.add

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.text.font.FontStyle
import androidx.navigation.NavController
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.screens.settings.SettingsViewModel
import com.FreeRave.remindme.screens.category.CategoryViewModel
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.components.TextFieldInput
import com.FreeRave.remindme.ui.components.LocationPickerDialog
import com.FreeRave.remindme.ui.components.SmartTextField
import com.FreeRave.remindme.ui.components.SmartCategorySuggestions
import com.FreeRave.remindme.ui.components.SmartTimeSuggestions
import com.FreeRave.remindme.ui.components.MapLocationPickerDialog
import androidx.compose.material.icons.filled.AutoAwesome
import com.FreeRave.remindme.utils.CategoryUtils
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

// Helper function defined at top level
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    navController: NavController,
    addViewModel: AddReminderViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    // States from ViewModel
    val nlpInputText by addViewModel.nlpInputText.collectAsState()
    val title by addViewModel.title.collectAsState()
    val description by addViewModel.description.collectAsState()
    val category by addViewModel.category.collectAsState()
    val reminderTimeMillis by addViewModel.reminderTimeMillis.collectAsState()
    val allCategories by categoryViewModel.allCategories.collectAsState()

    // Recurrence states from ViewModel
    val isRecurring by addViewModel.isRecurring.collectAsState()
    val recurrenceIntervalMillis by addViewModel.recurrenceIntervalMillis.collectAsState()
    val recurrenceEndTime by addViewModel.recurrenceEndTime.collectAsState()

    // Location-based states from ViewModel
    val isLocationBased by addViewModel.isLocationBased.collectAsState()
    val latitude by addViewModel.latitude.collectAsState()
    val longitude by addViewModel.longitude.collectAsState()
    val geofenceRadius by addViewModel.geofenceRadius.collectAsState()
    val geofenceTransitionType by addViewModel.geofenceTransitionType.collectAsState()
    val geofenceDwellTimeMillis by addViewModel.geofenceDwellTimeMillis.collectAsState()
    val geofencePriority by addViewModel.geofencePriority.collectAsState()

    // UI states for location picker
    var showLocationPicker by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var showLocationPickerChoice by remember { mutableStateOf(false) }

    // Local state for UI control
    var customId by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Default time from settings
    val defaultHour by settingsViewModel.defaultReminderHour.collectAsState()
    val defaultMinute by settingsViewModel.defaultReminderMinute.collectAsState()

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault()) }

    LaunchedEffect(Unit, defaultHour, defaultMinute) {
        if (addViewModel.reminderTimeMillis.value == null && !addViewModel.isRecurring.value) {
            val cal = Calendar.getInstance()
            if (defaultHour != UserPreferencesRepository.NO_DEFAULT_TIME_SET && defaultMinute != UserPreferencesRepository.NO_DEFAULT_TIME_SET) {
                cal.set(Calendar.HOUR_OF_DAY, defaultHour)
                cal.set(Calendar.MINUTE, defaultMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                addViewModel.onReminderTimeChanged(cal.timeInMillis)
            } else {
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                addViewModel.onReminderTimeChanged(cal.timeInMillis)
            }
        }
    }

    if (showDatePicker) {
        val currentCalendar = Calendar.getInstance().apply {
            timeInMillis = reminderTimeMillis ?: System.currentTimeMillis()
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        addViewModel.onReminderTimeChanged(selectedCalendar.timeInMillis)
                        showDatePicker = false
                    },
                    currentCalendar.get(Calendar.HOUR_OF_DAY),
                    currentCalendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    val scrollState = rememberScrollState()

    // Handle template selection result
    val selectedTemplate = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<ReminderTemplate>("selected_template")
        ?.value

    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { template ->
            addViewModel.onTitleChanged(template.title)
            addViewModel.onDescriptionChanged(template.description)
            addViewModel.onCategoryChanged(template.category)
            addViewModel.onIsRecurringChanged(template.isRecurring)
            template.recurrenceIntervalMillis?.let { addViewModel.onRecurrenceIntervalChanged(it) }
            addViewModel.onIsLocationBasedChanged(template.isLocationBased)

            navController.currentBackStackEntry?.savedStateHandle?.remove<ReminderTemplate>("selected_template")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.Templates.route) }) {
                        Text("Use Template")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            AdaptiveContainer { windowSizeClass, containerModifier ->
                Column(
                    modifier = containerModifier
                        .padding(paddingValues)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Magic Prompt (AI) ✨",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = nlpInputText,
                                    onValueChange = { addViewModel.onNlpInputChanged(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. Remind me to call Mom tomorrow at 5 PM") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledIconButton(
                                    onClick = { addViewModel.parseNaturalLanguageInput() },
                                    modifier = Modifier.size(56.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Parse with AI")
                                }
                            }
                        }
                    }
                    // --- Smart Title Input ---
                    SmartTextField(
                        value = title,
                        onValueChange = { addViewModel.onTitleChanged(it) },
                        label = "Title",
                        placeholder = "Enter reminder title or describe what you need to remember"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Smart Time Suggestions ---
                    SmartTimeSuggestions(
                        currentTimeMillis = reminderTimeMillis,
                        onTimeSelected = { addViewModel.onReminderTimeChanged(it) }
                    )

                    // --- Time and Recurrence Card ---
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val displayTime = reminderTimeMillis
                            if (displayTime != null) {
                                Text(
                                    text = "Time: ${dateFormat.format(Date(displayTime))}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                Text(
                                    text = "Time: Not set",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.EditCalendar,
                                    contentDescription = "Select Date/Time Icon"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set/Change Date & Time")
                            }

                            // Display Recurrence Info
                            if (isRecurring) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Recurring Reminder:",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                recurrenceIntervalMillis?.let { interval ->
                                    val intervalText = formatInterval(interval)
                                    Text(
                                        "Repeats every $intervalText",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                recurrenceEndTime?.let { endTime ->
                                    Text(
                                        "Until: ${dateFormat.format(Date(endTime))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Smart Description Input ---
                    SmartTextField(
                        value = description,
                        onValueChange = { addViewModel.onDescriptionChanged(it) },
                        label = "Description (Optional)",
                        placeholder = "Add more details or notes about this reminder",
                        singleLine = false,
                        modifier = Modifier.heightIn(min = 80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Smart Category Suggestions ---
                    SmartCategorySuggestions(
                        title = title,
                        description = description,
                        selectedCategory = category,
                        onCategorySelected = { addViewModel.onCategoryChanged(it) }
                    )

                    // --- Location-Based Settings ---
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = "Location")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Location-Based Reminder",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = isLocationBased,
                                    onCheckedChange = { addViewModel.onIsLocationBasedChanged(it) }
                                )
                            }

                            if (isLocationBased) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Transition type selector
                                Text("Trigger when:", style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = geofenceTransitionType == 1,
                                        onClick = { addViewModel.onGeofenceTransitionTypeChanged(1) },
                                        label = { Text("Entering") }
                                    )
                                    FilterChip(
                                        selected = geofenceTransitionType == 2,
                                        onClick = { addViewModel.onGeofenceTransitionTypeChanged(2) },
                                        label = { Text("Leaving") }
                                    )
                                    FilterChip(
                                        selected = geofenceTransitionType == 3,
                                        onClick = { addViewModel.onGeofenceTransitionTypeChanged(3) },
                                        label = { Text("Both") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Location picker buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showLocationPickerChoice = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Filled.Place,
                                            contentDescription = "Select Location"
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pick Location")
                                    }

                                    if (latitude != null && longitude != null) {
                                        Text(
                                            text = "(%.6f, %.6f)".format(latitude, longitude),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Dwell time selector
                                Text(
                                    "Dwell Time (wait before triggering):",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = geofenceDwellTimeMillis == null,
                                        onClick = { addViewModel.onGeofenceDwellTimeChanged(null) },
                                        label = { Text("Immediate") }
                                    )
                                    FilterChip(
                                        selected = geofenceDwellTimeMillis == 30000L,
                                        onClick = { addViewModel.onGeofenceDwellTimeChanged(30000L) },
                                        label = { Text("30 sec") }
                                    )
                                    FilterChip(
                                        selected = geofenceDwellTimeMillis == 60000L,
                                        onClick = { addViewModel.onGeofenceDwellTimeChanged(60000L) },
                                        label = { Text("1 min") }
                                    )
                                    FilterChip(
                                        selected = geofenceDwellTimeMillis == 300000L,
                                        onClick = { addViewModel.onGeofenceDwellTimeChanged(300000L) },
                                        label = { Text("5 min") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Priority selector
                                Text(
                                    "Priority (for geofence management):",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = geofencePriority == 1,
                                        onClick = { addViewModel.onGeofencePriorityChanged(1) },
                                        label = { Text("Low") }
                                    )
                                    FilterChip(
                                        selected = geofencePriority == 3,
                                        onClick = { addViewModel.onGeofencePriorityChanged(3) },
                                        label = { Text("Medium") }
                                    )
                                    FilterChip(
                                        selected = geofencePriority == 5,
                                        onClick = { addViewModel.onGeofencePriorityChanged(5) },
                                        label = { Text("High") }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Category (Optional) ---
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedCategory = allCategories.find { it.name == category }
                        OutlinedTextField(
                            value = if (category.isEmpty()) "Select Category" else "${
                                selectedCategory?.icon ?: CategoryUtils.getCategoryIcon(
                                    category
                                )
                            } $category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
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
                                        addViewModel.onCategoryChanged(categoryItem.name)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    addViewModel.onCategoryChanged("")
                                    expanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextFieldInput(
                        value = customId,
                        onValueChange = { customId = it },
                        label = "Custom ID (Optional)",
                        placeholder = "Enter unique ID"
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        PrimaryButton(
                            text = "Add Reminder",
                            onClick = {
                                if (title.isNotBlank() && reminderTimeMillis != null) {
                                    if (isRecurring && recurrenceIntervalMillis == null) {
                                        Toast.makeText(
                                            context,
                                            "Please set an interval for recurring reminders.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@PrimaryButton
                                    }
                                    if (isLocationBased && (latitude == null || longitude == null)) {
                                        Toast.makeText(
                                            context,
                                            "Please select a location for location-based reminders.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@PrimaryButton
                                    }
                                    isLoading = true
                                    addViewModel.addReminder(
                                        customId = customId.ifBlank {
                                            UUID.randomUUID().toString().substring(0, 8)
                                        },
                                        onComplete = { success, _ ->
                                            isLoading = false
                                            if (success) {
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to add reminder.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please enter a title and set a reminder time.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showSaveTemplateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save as Template")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Location Picker Choice Dialog
            if (showLocationPickerChoice) {
                AlertDialog(
                    onDismissRequest = { showLocationPickerChoice = false },
                    title = { Text("Choose Location Method") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "How would you like to select the location?",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedButton(
                                onClick = {
                                    showLocationPickerChoice = false
                                    showMapLocationPicker = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🗺️ Interactive Map")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Tap on map to select",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    showLocationPickerChoice = false
                                    showLocationPicker = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📍 Enter Coordinates")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Manual lat/lng input",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLocationPickerChoice = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Coordinate Location Picker Dialog
            if (showLocationPicker) {
                LocationPickerDialog(
                    onDismiss = { showLocationPicker = false },
                    onLocationSelected = { lat, lng, radius ->
                        addViewModel.onLocationChanged(lat, lng, radius)
                        showLocationPicker = false
                    },
                    initialLatitude = latitude,
                    initialLongitude = longitude,
                    initialRadius = geofenceRadius
                )
            }

            // Map Location Picker Dialog
            if (showMapLocationPicker) {
                MapLocationPickerDialog(
                    onDismiss = { showMapLocationPicker = false },
                    onLocationSelected = { lat, lng, radius ->
                        addViewModel.onLocationChanged(lat, lng, radius)
                        showMapLocationPicker = false
                    },
                    initialLatitude = latitude,
                    initialLongitude = longitude,
                    initialRadius = geofenceRadius
                )
            }

            // Save Template Dialog
            if (showSaveTemplateDialog) {
                var templateName by remember { mutableStateOf("") }
                val templatesViewModel: com.FreeRave.remindme.screens.templates.TemplatesViewModel =
                    hiltViewModel()

                AlertDialog(
                    onDismissRequest = { showSaveTemplateDialog = false },
                    title = { Text("Save as Template") },
                    text = {
                        Column {
                            Text("Enter a name for this template:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = templateName,
                                onValueChange = { templateName = it },
                                label = { Text("Template Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (templateName.isNotBlank()) {
                                    val template = templatesViewModel.createTemplateFromReminder(
                                        reminderTitle = title,
                                        reminderDescription = description,
                                        reminderCategory = category,
                                        isRecurring = isRecurring,
                                        recurrenceIntervalMillis = recurrenceIntervalMillis,
                                        isLocationBased = isLocationBased,
                                        templateName = templateName
                                    )
                                    templatesViewModel.addTemplate(template) { success ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Template saved successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showSaveTemplateDialog = false
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to save template.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = templateName.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveTemplateDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}