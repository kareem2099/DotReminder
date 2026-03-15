package com.FreeRave.remindme.screens.ai

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel // Already here, but ensure it's the correct one
import androidx.lifecycle.viewModelScope // Already here
import dagger.hilt.android.lifecycle.HiltViewModel // Already here
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log // Added for logging
import javax.inject.Inject
import com.FreeRave.remindme.service.GeminiService
import com.FreeRave.remindme.utils.NLPUtils
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface // Added for userId
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// ChatMessage and SenderType are in the same package, should be resolved.

sealed class AiActionEffect {
    data class NavigateToEditReminder(val reminderId: String) : AiActionEffect()
    data class NavigateToAddReminder(
        val title: String?,
        val reminderTime: Long?,
        val isRecurring: Boolean?,
        val recurrenceIntervalMillis: Long?,
        val recurrenceEndTime: Long?
    ) : AiActionEffect()
    data class ShowToast(val message: String) : AiActionEffect()
}

@HiltViewModel
class AiViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val reminderService: ReminderService,
    private val firebaseAuthService: FirebaseAuthServiceInterface // Inject FirebaseAuthService
    // NLPUtils is an object, can be called directly
) : ViewModel() {

    private val _actionEffect = MutableSharedFlow<AiActionEffect>()
    val actionEffect = _actionEffect.asSharedFlow()

    private val _chatMessages = mutableStateListOf<ChatMessage>()
    val chatMessages: List<ChatMessage> = _chatMessages

    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Add an initial greeting message from the AI
        _chatMessages.add(ChatMessage(text = "Hello! How can I assist you today?", sender = SenderType.AI))
    }

    fun onUserInputChange(text: String) {
        _userInput.value = text
    }

    fun sendMessage() {
        val userMessageText = _userInput.value.trim()
        if (userMessageText.isBlank()) {
            return
        }

        // Add user message to the list
        _chatMessages.add(ChatMessage(text = userMessageText, sender = SenderType.USER))
        _userInput.value = "" // Clear input field

        // Simulate AI response (replace with actual Gemini call)
        Log.d("AiViewModel", "sendMessage called with: $userMessageText")
        viewModelScope.launch {
            _isLoading.value = true
            _chatMessages.add(ChatMessage(text = "Thinking...", sender = SenderType.AI))
            Log.d("AiViewModel", "Added 'Thinking...' message.")

            try {
                // --- Simulate network delay ---
                // kotlinx.coroutines.delay(1500) // Temporarily remove to isolate geminiService call
                // --- End simulation ---

                Log.d("AiViewModel", "Calling geminiService.generateContent...")
                val aiResponseText = geminiService.generateContent(userMessageText)
                // Log the raw response from Gemini to see exactly what it's sending
                Log.d("AiViewModel_GeminiRawResponse", "Raw AI Response: '$aiResponseText'")
                Log.d("AiViewModel", "geminiService.generateContent returned (processed by VM): $aiResponseText")

                // Remove "Thinking..." message
                val thinkingMessageIndex = _chatMessages.indexOfLast { it.text == "Thinking..." && it.sender == SenderType.AI }
                if (thinkingMessageIndex != -1) {
                    _chatMessages.removeAt(thinkingMessageIndex)
                }

                // Process AI response
                if (aiResponseText.startsWith("ACTION_PARSE_REMINDER: ")) {
                    val command = aiResponseText.removePrefix("ACTION_PARSE_REMINDER: ").trim()
                    Log.d("AiViewModel", "Extracted command for NLPUtils: $command")
                    try {
                        val parsedInfo = NLPUtils.parseReminderText(command)
                        Log.d("AiViewModel", "NLPUtils parsed: $parsedInfo")

                        if (parsedInfo.title.isNotBlank() && parsedInfo.reminderTime != null) {
                            // For simplicity, let's try to directly add the reminder if title and time are present.
                            // More complex logic would be needed for edit/delete or navigating to AddReminderScreen with pre-filled data.
                            val newReminder = Reminder(
                                id = UUID.randomUUID().toString(), // Generate new ID
                                customId = "", // AI doesn't set this yet
                                title = parsedInfo.title,
                                description = "", // AI doesn't set this yet
                                time = System.currentTimeMillis(), // Creation time
                                reminderTime = parsedInfo.reminderTime,
                                isRecurring = parsedInfo.isRecurring,
                                recurrenceIntervalMillis = parsedInfo.recurrenceIntervalMillis,
                                recurrenceEndTime = parsedInfo.recurrenceEndTime,
                                userId = "" // TODO: Need to get current user ID
                            )
                            // TODO: Get current user ID. For now, this will fail if userId is strictly required by Firestore rules.
                            val currentUserId = firebaseAuthService.getCurrentUserId()

                            if (currentUserId.isBlank()) {
                                Log.e("AiViewModel", "User ID is blank. User might not be logged in. Reminder not saved. Parsed: $parsedInfo")
                                _chatMessages.add(ChatMessage(text = "I understood: ${parsedInfo.title}. To save this reminder, please make sure you are logged in.", sender = SenderType.AI))
                                viewModelScope.launch { _actionEffect.emit(AiActionEffect.ShowToast("Login required to save reminders.")) }
                            } else {
                                val reminderToSave = newReminder.copy(userId = currentUserId)
                                try {
                                    val savedReminder = reminderService.addReminder(reminderToSave) 
                                    Log.d("AiViewModel", "Reminder saved: $savedReminder")
                                    _chatMessages.add(ChatMessage(text = "OK, reminder '${savedReminder.title}' added!", sender = SenderType.AI))
                                    viewModelScope.launch { _actionEffect.emit(AiActionEffect.ShowToast("Reminder '${savedReminder.title}' added!")) }
                                } catch (e: Exception) {
                                    Log.e("AiViewModel", "Error saving reminder for user '$currentUserId': ${e.message}", e)
                                    _chatMessages.add(ChatMessage(text = "I understood the reminder, but there was an error trying to save it.", sender = SenderType.AI))
                                    viewModelScope.launch { _actionEffect.emit(AiActionEffect.ShowToast("Error saving reminder.")) }
                                }
                            }
                            // Example: Navigate to AddReminderScreen with pre-filled data
//                            viewModelScope.launch {
//                                _actionEffect.emit(AiActionEffect.NavigateToAddReminder(
//                                    title = parsedInfo.title,
//                                    reminderTime = parsedInfo.reminderTime,
//                                    isRecurring = parsedInfo.isRecurring,
//                                    recurrenceIntervalMillis = parsedInfo.recurrenceIntervalMillis,
//                                    recurrenceEndTime = parsedInfo.recurrenceEndTime
//                                ))
//                            }


                        } else {
                            _chatMessages.add(ChatMessage(text = "I understood the command, but some details like title or time are missing. Could you clarify?", sender = SenderType.AI))
                        }
                    } catch (e: Exception) {
                        Log.e("AiViewModel", "Error parsing command with NLPUtils: ${e.message}", e)
                        _chatMessages.add(ChatMessage(text = "I had trouble understanding the details of that reminder request.", sender = SenderType.AI))
                    }
                } else {
                    // It's a regular chat message from AI (e.g., a question)
                    _chatMessages.add(ChatMessage(text = aiResponseText, sender = SenderType.AI))
                }

            } catch (e: Exception) {
                Log.e("AiViewModel", "Error calling GeminiService or updating chat: ${e.message}", e)
                val thinkingMessageIndexOnError = _chatMessages.indexOfLast { it.text == "Thinking..." && it.sender == SenderType.AI }
                if (thinkingMessageIndexOnError != -1) {
                    _chatMessages.removeAt(thinkingMessageIndexOnError)
                }
                _chatMessages.add(ChatMessage(text = "Sorry, an error occurred.", sender = SenderType.AI))
            } finally {
                _isLoading.value = false
                Log.d("AiViewModel", "sendMessage finished.")
            }
        }
    }
}
