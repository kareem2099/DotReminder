package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.model.Session

/** Sort by recorded activity and give equal timestamps a stable order. */
internal fun orderSessionRecords(sessions: List<Session>): List<Session> = sessions
    .filter { it.sessionId.isNotBlank() }.distinctBy { it.sessionId }
    .sortedWith(compareByDescending<Session> { it.lastActive }.thenBy { it.sessionId })

internal fun ActiveSessionsUiState.canRemoveSession(sessionId: String): Boolean = canManageRecords &&
    sessionId.isNotBlank() && sessionId != currentSessionId && sessions.any { it.sessionId == sessionId }

internal fun ActiveSessionsUiState.canConfirmRemoval(): Boolean = when (val target = removal) {
    is SessionRemoval.One -> canRemoveSession(target.session.sessionId)
    SessionRemoval.Others -> canClearOthers
    null -> false
}
