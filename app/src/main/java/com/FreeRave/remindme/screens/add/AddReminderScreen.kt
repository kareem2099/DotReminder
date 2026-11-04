package com.FreeRave.remindme.screens.add

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.model.AiReminderSaveResult
import com.FreeRave.remindme.screens.ai.AI_REMINDER_DRAFT_KEY
import com.FreeRave.remindme.screens.ai.AI_REMINDER_RESULT_KEY
import com.FreeRave.remindme.screens.ai.AI_SOURCE_DRAFT_KEY
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.category.CategoryViewModel
import com.FreeRave.remindme.screens.settings.SettingsViewModel
import com.FreeRave.remindme.ui.reminder.DAY_MILLIS
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent
import com.FreeRave.remindme.ui.reminder.initialReminderTime
import com.FreeRave.remindme.ui.reminder.validate
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.reminders.rememberReminderDeliveryAccess
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun AddReminderScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    addViewModel: AddReminderViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val draft = addViewModel.collectReminderDraft()
    val categories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val defaultHour by settingsViewModel.defaultReminderHour.collectAsStateWithLifecycle()
    val defaultMinute by settingsViewModel.defaultReminderMinute.collectAsStateWithLifecycle()
    val isSaving by addViewModel.isSaving.collectAsStateWithLifecycle()
    val isParsing by addViewModel.isParsing.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var validationRequest by rememberSaveable { mutableIntStateOf(0) }
    var showTemplateDialog by rememberSaveable { mutableStateOf(false) }
    val errors = if (validationRequest > 0) draft.validate(System.currentTimeMillis()) else emptySet()

    fun showMessage(@StringRes message: Int) {
        scope.launch { snackbar.showSnackbar(resources.getString(message)) }
    }

    LaunchedEffect(defaultHour, defaultMinute) {
        if (addViewModel.reminderTimeMillis.value == null) {
            addViewModel.onReminderTimeChanged(initialReminderTime(System.currentTimeMillis(), defaultHour, defaultMinute))
        }
    }

    val savedStateHandle = backStackEntry.savedStateHandle
    val selectedTemplateFlow = remember(savedStateHandle) {
        savedStateHandle.getStateFlow<ReminderTemplate?>("selected_template", null)
    }
    val selectedTemplate by selectedTemplateFlow.collectAsStateWithLifecycle()
    val aiDraftFlow = remember(savedStateHandle) { savedStateHandle.getStateFlow<AiReminderDraft?>(AI_REMINDER_DRAFT_KEY, null) }
    val aiDraft by aiDraftFlow.collectAsStateWithLifecycle()
    LaunchedEffect(aiDraft) {
        aiDraft?.let {
            addViewModel.applyAiDraft(it)
            savedStateHandle.set<AiReminderDraft?>(AI_REMINDER_DRAFT_KEY, null)
        }
    }
    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { template ->
            addViewModel.onTitleChanged(template.title)
            addViewModel.onDescriptionChanged(template.description)
            addViewModel.onCategoryChanged(template.category)
            addViewModel.onIsRecurringChanged(template.isRecurring)
            addViewModel.onRecurrenceIntervalChanged(if (template.isRecurring) template.recurrenceIntervalMillis ?: DAY_MILLIS else null)
            addViewModel.onRecurrenceEndTimeChanged(null)
            addViewModel.onIsLocationBasedChanged(template.isLocationBased)
            if (template.isLocationBased && addViewModel.geofenceTransitionType.value == 0) {
                addViewModel.onGeofenceTransitionTypeChanged(1)
            }
            savedStateHandle.set<ReminderTemplate?>("selected_template", null)
        }
    }

    var pendingCustomId by rememberSaveable { mutableStateOf("") }
    val saveWithDeliveryAccess = rememberReminderDeliveryAccess {
        if (draft.validate(System.currentTimeMillis()).isNotEmpty()) showMessage(R.string.reminder_error_fields)
        else addViewModel.addReminder(pendingCustomId) { success, reminderId ->
            if (success) {
                if (navController.currentBackStackEntry?.destination?.route == Screen.Add.route) {
                    val draftId = savedStateHandle.get<String>(AI_SOURCE_DRAFT_KEY)
                    val previous = navController.previousBackStackEntry
                    if (draftId != null && !reminderId.isNullOrBlank() && previous?.destination?.route == Screen.AiChat.route)
                        previous.savedStateHandle[AI_REMINDER_RESULT_KEY] = AiReminderSaveResult(draftId, reminderId)
                    navController.popBackStack()
                }
            } else showMessage(R.string.reminder_error_save)
        }
    }
    val onEvent: (ReminderFormEvent) -> Unit = { event ->
        when (event) {
            is ReminderFormEvent.Title -> addViewModel.onTitleChanged(event.value)
            is ReminderFormEvent.Description -> addViewModel.onDescriptionChanged(event.value)
            is ReminderFormEvent.Prompt -> addViewModel.onNlpInputChanged(event.value)
            is ReminderFormEvent.Category -> addViewModel.onCategoryChanged(event.value)
            is ReminderFormEvent.Time -> addViewModel.onReminderTimeChanged(event.value)
            is ReminderFormEvent.Interval -> addViewModel.onRecurrenceIntervalChanged(event.value)
            is ReminderFormEvent.EndTime -> addViewModel.onRecurrenceEndTimeChanged(event.value)
            is ReminderFormEvent.Location -> addViewModel.onLocationChanged(event.latitude, event.longitude, event.radius)
            is ReminderFormEvent.Transition -> addViewModel.onGeofenceTransitionTypeChanged(event.value)
            is ReminderFormEvent.DwellTime -> addViewModel.onGeofenceDwellTimeChanged(event.value)
            is ReminderFormEvent.Priority -> addViewModel.onGeofencePriorityChanged(event.value)
            is ReminderFormEvent.Recurring -> {
                addViewModel.onIsRecurringChanged(event.enabled)
                if (event.enabled && draft.recurrenceIntervalMillis == null) addViewModel.onRecurrenceIntervalChanged(DAY_MILLIS)
            }
            is ReminderFormEvent.LocationEnabled -> {
                addViewModel.onIsLocationBasedChanged(event.enabled)
                if (event.enabled && draft.geofenceTransitionType == 0) addViewModel.onGeofenceTransitionTypeChanged(1)
            }
            is ReminderFormEvent.Save -> {
                validationRequest++
                if (draft.validate(System.currentTimeMillis()).isNotEmpty()) {
                    showMessage(R.string.reminder_error_fields)
                } else {
                    pendingCustomId = event.customId.trim().ifEmpty { UUID.randomUUID().toString().take(8) }
                    saveWithDeliveryAccess()
                }
            }
            ReminderFormEvent.Parse -> addViewModel.parseNaturalLanguageInput()
            ReminderFormEvent.Templates -> navController.navigate(Screen.Templates.route)
            ReminderFormEvent.Back -> navController.popBackStack()
            ReminderFormEvent.SaveTemplate -> {
                if (draft.title.isBlank()) showMessage(R.string.reminder_template_required)
                else showTemplateDialog = true
            }
        }
    }

    GlassTheme {
        AddReminderContent(draft, categories, errors, validationRequest, isSaving, isParsing, snackbar, onEvent)
        if (showTemplateDialog) {
            SaveReminderTemplateDialog(draft, onDismiss = { showTemplateDialog = false }) {
                showTemplateDialog = false
                showMessage(R.string.reminder_template_saved)
            }
        }
    }
}
