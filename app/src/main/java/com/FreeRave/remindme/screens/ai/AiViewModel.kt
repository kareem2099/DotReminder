package com.FreeRave.remindme.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.service.GeminiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(geminiService: GeminiService) : ViewModel() {
    private val controller = AiChatController(GeminiChatGateway(geminiService))
    internal val state = controller.state
    fun onUserInputChange(text: String) = controller.updateInput(text)
    fun sendMessage() { viewModelScope.launch { controller.send() } }
    fun retry() { viewModelScope.launch { controller.retry() } }
    fun clearConversation() = controller.clearConversation()
    fun markSaved(draftId: String, reminderId: String) = controller.markSaved(draftId, reminderId)
}
