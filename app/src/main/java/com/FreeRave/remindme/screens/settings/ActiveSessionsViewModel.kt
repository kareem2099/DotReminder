package com.FreeRave.remindme.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveSessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveSessionsUiState())
    internal val uiState = _uiState.asStateFlow()
    private val _effects = Channel<SessionEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()
    private var loadJob: Job? = null
    private var removalJob: Job? = null
    private var activeUserId: String? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.currentSessionId.collect { id ->
                _uiState.update { it.copy(currentSessionId = id?.takeIf(String::isNotBlank), identityLoaded = true) }
            }
        }
        viewModelScope.launch {
            authService.currentUserIdFlow().distinctUntilChanged().collectLatest { loadForUser(it) }
        }
    }

    fun retry() = loadForUser(authService.getCurrentUserId())

    private fun loadForUser(userId: String?) {
        loadJob?.cancel()
        if (activeUserId != userId) {
            removalJob?.cancel()
            activeUserId = userId
            _uiState.update { ActiveSessionsUiState(currentSessionId = it.currentSessionId, identityLoaded = it.identityLoaded) }
        }
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(sessions = emptyList(), isLoading = false, hasError = true) }
            return
        }
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            try {
                sessionRepository.getSessions(userId).collect { sessions ->
                    val records = orderSessionRecords(sessions)
                    _uiState.update { state ->
                        val target = state.removal
                        val targetMissing = target is SessionRemoval.One && records.none { it.sessionId == target.session.sessionId }
                        state.copy(sessions = records, isLoading = false, hasError = false,
                            removal = if (targetMissing && !state.isRemoving) null else target)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }

    internal fun requestRemoval(session: Session) {
        val state = _uiState.value
        if (state.canRemoveSession(session.sessionId)) {
            _uiState.update { it.copy(removal = SessionRemoval.One(session), removalFailed = false) }
        }
    }

    internal fun requestClearOthers() {
        if (_uiState.value.canClearOthers) _uiState.update { it.copy(removal = SessionRemoval.Others, removalFailed = false) }
    }

    fun dismissRemoval() {
        _uiState.update { if (it.isRemoving) it else it.copy(removal = null, removalFailed = false) }
    }

    fun confirmRemoval() {
        val state = _uiState.value
        if (state.isRemoving) return
        val userId = authService.getCurrentUserId()
        if (!state.canConfirmRemoval() || userId.isBlank() || userId != activeUserId) {
            _uiState.update { it.copy(removalFailed = true) }
            return
        }
        val target = state.removal ?: return
        val currentId = state.currentSessionId ?: return
        _uiState.update { it.copy(isRemoving = true, removalFailed = false) }
        removalJob = viewModelScope.launch {
            try {
                val success = when (target) {
                    is SessionRemoval.One -> sessionRepository.deleteSession(target.session.sessionId)
                    SessionRemoval.Others -> sessionRepository.deleteAllSessionsExcept(currentId)
                }
                ensureActive()
                if (activeUserId != userId) return@launch
                if (success) {
                    // The snapshot listener owns the list, including sessions created during this request.
                    _uiState.update { it.copy(removal = null, isRemoving = false, removalFailed = false) }
                    _effects.send(if (target is SessionRemoval.One) SessionEffect.RECORD_REMOVED else SessionEffect.OTHER_RECORDS_REMOVED)
                } else _uiState.update { it.copy(isRemoving = false, removalFailed = true) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (activeUserId == userId) _uiState.update { it.copy(isRemoving = false, removalFailed = true) }
            }
        }
    }
}
