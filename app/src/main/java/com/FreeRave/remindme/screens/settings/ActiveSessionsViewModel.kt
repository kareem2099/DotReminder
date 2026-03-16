package com.FreeRave.remindme.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.service.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveSessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            userPreferencesRepository.currentSessionId.collect { id ->
                _currentSessionId.value = id
            }
        }
        loadSessions()
    }

    private fun loadSessions() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            sessionRepository.getSessions(userId)
                .catch { _isLoading.value = false }
                .collect { list ->
                    _sessions.value = list
                    _isLoading.value = false
                }
        }
    }

    fun signOutSession(sessionId: String) {
        viewModelScope.launch {
            val success = sessionRepository.deleteSession(sessionId)
            _message.value = if (success) "Device signed out." else "Failed to sign out device."
        }
    }

    fun signOutAllOtherSessions() {
        val current = _currentSessionId.value ?: return
        viewModelScope.launch {
            val success = sessionRepository.deleteAllSessionsExcept(current)
            _message.value = if (success) "All other devices signed out." else "Failed to sign out other devices."
        }
    }

    fun clearMessage() { _message.value = null }
}