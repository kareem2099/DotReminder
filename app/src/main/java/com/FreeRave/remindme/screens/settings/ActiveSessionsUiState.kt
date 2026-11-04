package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.model.Session

internal data class ActiveSessionsUiState(
    val sessions: List<Session> = emptyList(),
    val currentSessionId: String? = null,
    val identityLoaded: Boolean = false,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val removal: SessionRemoval? = null,
    val isRemoving: Boolean = false,
    val removalFailed: Boolean = false
) {
    val currentSession get() = sessions.firstOrNull { currentSessionId?.isNotBlank() == true && it.sessionId == currentSessionId }
    val otherSessions get() = sessions.filterNot { it.sessionId == currentSessionId }
    val canManageRecords get() = identityLoaded && currentSession != null && !isLoading && !hasError && !isRemoving
    val canClearOthers get() = canManageRecords && otherSessions.isNotEmpty()
}

internal sealed interface SessionRemoval {
    data class One(val session: Session) : SessionRemoval
    data object Others : SessionRemoval
}

internal enum class SessionEffect { RECORD_REMOVED, OTHER_RECORDS_REMOVED }
