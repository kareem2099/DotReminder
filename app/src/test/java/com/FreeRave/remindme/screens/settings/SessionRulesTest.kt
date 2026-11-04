package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.model.Session
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class SessionRulesTest {
    private fun session(id: String, seconds: Long = 100, nanos: Int = 0) = Session(
        sessionId = id, deviceName = "Same phone", deviceModel = "IN2025",
        loginTime = Timestamp(1, 0), lastActive = Timestamp(seconds, nanos))
    private fun ready() = ActiveSessionsUiState(sessions = listOf(session("this"), session("other")),
        currentSessionId = "this", identityLoaded = true, isLoading = false)

    @Test fun `this session cannot be removed`() {
        assertFalse(ready().canRemoveSession("this"))
    }

    @Test fun `a known other record can be removed`() {
        assertTrue(ready().canRemoveSession("other"))
    }

    @Test fun `a blank or unknown target cannot be removed`() {
        assertFalse(ready().canRemoveSession(""))
        assertFalse(ready().canRemoveSession("missing"))
    }

    @Test fun `removal waits for current identity to load`() {
        val state = ready().copy(identityLoaded = false)
        assertFalse(state.canRemoveSession("other"))
        assertFalse(state.canClearOthers)
    }

    @Test fun `a missing current id cannot enable bulk removal`() {
        listOf(null, "", "missing").forEach { id ->
            val state = ready().copy(currentSessionId = id)
            assertFalse(state.canManageRecords)
            assertFalse(state.canClearOthers)
        }
    }

    @Test fun `loading errors and pending writes disable new removal requests`() {
        listOf(ready().copy(isLoading = true), ready().copy(hasError = true), ready().copy(isRemoving = true)).forEach {
            assertFalse(it.canRemoveSession("other"))
            assertFalse(it.canClearOthers)
        }
    }

    @Test fun `only this session leaves nothing to clear`() {
        val state = ready().copy(sessions = listOf(session("this")))
        assertFalse(state.canClearOthers)
        assertTrue(state.otherSessions.isEmpty())
    }

    @Test fun `bulk removal requires a known current session and other records`() {
        val state = ready().copy(removal = SessionRemoval.Others)
        assertTrue(state.canClearOthers)
        assertTrue(state.canConfirmRemoval())
    }

    @Test fun `confirmation cannot remove a record that disappeared`() {
        val target = SessionRemoval.One(session("other"))
        assertTrue(ready().copy(removal = target).canConfirmRemoval())
        assertFalse(ready().copy(sessions = listOf(session("this")), removal = target).canConfirmRemoval())
    }

    @Test fun `confirmation requires a target and never accepts this session`() {
        assertFalse(ready().canConfirmRemoval())
        assertFalse(ready().copy(removal = SessionRemoval.One(session("this"))).canConfirmRemoval())
    }

    @Test fun `activity ordering includes nanoseconds and a stable tie breaker`() {
        val input = listOf(session("b", 100), session("old", 1), session("a", 100), session("new", 100, 1))
        assertEquals(listOf("new", "a", "b", "old"), orderSessionRecords(input).map { it.sessionId })
    }

    @Test fun `blank and duplicate record ids cannot create unstable lazy list keys`() {
        val records = orderSessionRecords(listOf(session(""), session("a"), session("a"), session("b")))
        assertEquals(listOf("a", "b"), records.map { it.sessionId })
    }

    @Test fun `repeated hardware names remain separate session records`() {
        val state = ready()
        assertEquals(2, state.sessions.size)
        assertEquals("this", state.currentSession?.sessionId)
        assertEquals(listOf("other"), state.otherSessions.map { it.sessionId })
    }
}
