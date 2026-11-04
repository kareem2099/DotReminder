package com.FreeRave.remindme.screens.support

import org.junit.Assert.*
import org.junit.Test

class SupportAdSessionTest {
    @Test fun `a pending request blocks both buttons`() {
        val session = SupportAdSession()
        assertNotNull(session.request(SupportAdKind.STANDARD))
        assertTrue(session.state.value.busy)
        assertNull(session.request(SupportAdKind.STANDARD))
        assertNull(session.request(SupportAdKind.REWARDED))
    }
    @Test fun `cancelling prevents a late load from opening an ad`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.STANDARD)!!
        session.cancelLoading()
        assertFalse(session.loaded(token))
        assertEquals(SupportAdPhase.IDLE, session.state.value.phase)
    }
    @Test fun `a retry ignores callbacks from the cancelled request`() {
        val session = SupportAdSession()
        val old = session.request(SupportAdKind.STANDARD)!!
        session.cancelLoading()
        val next = session.request(SupportAdKind.REWARDED)!!
        assertNotEquals(old, next)
        session.failed(old)
        assertFalse(session.loaded(old))
        assertEquals(SupportAdPhase.LOADING, session.state.value.phase)
        assertTrue(session.loaded(next))
    }
    @Test fun `load failure enables retry and identifies the failed choice`() {
        val session = SupportAdSession()
        session.failed(session.request(SupportAdKind.REWARDED)!!)
        assertEquals(SupportAdPhase.FAILED, session.state.value.phase)
        assertEquals(SupportAdKind.REWARDED, session.state.value.kind)
        assertTrue(session.state.value.canRequest)
        assertNotNull(session.request(SupportAdKind.STANDARD))
    }
    @Test fun `a visible ad is not cancelled by the activity pause it causes`() {
        val session = SupportAdSession()
        assertTrue(session.loaded(session.request(SupportAdKind.STANDARD)!!))
        session.cancelLoading()
        assertEquals(SupportAdPhase.SHOWING, session.state.value.phase)
        assertNull(session.request(SupportAdKind.REWARDED))
    }
    @Test fun `closing a standard ad returns to the page without a reward message`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.STANDARD)!!
        session.loaded(token)
        session.reward(token)
        session.dismissed(token)
        assertEquals(SupportAdPhase.IDLE, session.state.value.phase)
    }
    @Test fun `the thank you waits until an earned rewarded ad closes`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.REWARDED)!!
        session.loaded(token)
        session.reward(token)
        assertEquals(SupportAdPhase.SHOWING, session.state.value.phase)
        session.dismissed(token)
        assertEquals(SupportAdPhase.THANK_YOU, session.state.value.phase)
        assertFalse(session.state.value.canRequest)
    }
    @Test fun `skipping a rewarded ad does not create a thank you`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.REWARDED)!!
        session.loaded(token)
        session.dismissed(token)
        assertEquals(SupportAdPhase.IDLE, session.state.value.phase)
    }
    @Test fun `mediation may deliver the earned callback after dismissal`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.REWARDED)!!
        session.loaded(token)
        session.dismissed(token)
        session.reward(token)
        assertEquals(SupportAdPhase.THANK_YOU, session.state.value.phase)
    }
    @Test fun `duplicate reward callbacks cannot reopen a dismissed thank you`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.REWARDED)!!
        session.loaded(token)
        session.reward(token)
        session.dismissed(token)
        session.dismissThanks()
        session.reward(token)
        session.dismissed(token)
        assertEquals(SupportAdPhase.IDLE, session.state.value.phase)
    }
    @Test fun `rewards received before an ad opens are ignored`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.REWARDED)!!
        session.reward(token)
        session.loaded(token)
        session.dismissed(token)
        assertEquals(SupportAdPhase.IDLE, session.state.value.phase)
    }
    @Test fun `disposing a screen prevents late callbacks and new requests`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.REWARDED)!!
        session.dispose()
        assertFalse(session.loaded(token))
        session.reward(token)
        session.failed(token)
        session.dismissed(token)
        assertEquals(SupportAdPhase.IDLE, session.state.value.phase)
        assertNull(session.request(SupportAdKind.STANDARD))
    }
    @Test fun `a show failure unlocks the page for another request`() {
        val session = SupportAdSession()
        val token = session.request(SupportAdKind.STANDARD)!!
        session.loaded(token)
        session.failed(token)
        assertEquals(SupportAdPhase.FAILED, session.state.value.phase)
        assertNotNull(session.request(SupportAdKind.REWARDED))
    }
}
