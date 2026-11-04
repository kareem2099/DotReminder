package com.FreeRave.remindme.screens.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class SupportAdKind { STANDARD, REWARDED }
internal enum class SupportAdPhase { IDLE, LOADING, SHOWING, FAILED, THANK_YOU }
internal data class SupportAdState(val phase: SupportAdPhase = SupportAdPhase.IDLE, val kind: SupportAdKind? = null) {
    val busy get() = phase == SupportAdPhase.LOADING || phase == SupportAdPhase.SHOWING
    val canRequest get() = phase == SupportAdPhase.IDLE || phase == SupportAdPhase.FAILED
}

/** Tokens isolate SDK callbacks from cancelled requests and screens that have already closed. */
internal class SupportAdSession {
    private val _state = MutableStateFlow(SupportAdState())
    val state = _state.asStateFlow()
    private var serial = 0L
    private var active = true
    private var earned = false
    private var closed = false
    private var requestedKind: SupportAdKind? = null

    fun request(kind: SupportAdKind): Long? {
        if (!active || !_state.value.canRequest) return null
        earned = false
        closed = false
        requestedKind = kind
        _state.value = SupportAdState(SupportAdPhase.LOADING, kind)
        return ++serial
    }

    fun loaded(token: Long): Boolean {
        if (!matches(token, SupportAdPhase.LOADING)) return false
        _state.value = _state.value.copy(phase = SupportAdPhase.SHOWING)
        return true
    }

    fun failed(token: Long) {
        if (active && token == serial && _state.value.busy) _state.value = _state.value.copy(phase = SupportAdPhase.FAILED)
    }

    fun reward(token: Long) {
        if (!active || token != serial || earned || requestedKind != SupportAdKind.REWARDED) return
        val phase = _state.value.phase
        if (phase != SupportAdPhase.SHOWING && !(closed && phase == SupportAdPhase.IDLE)) return
        earned = true
        if (closed) _state.value = SupportAdState(SupportAdPhase.THANK_YOU, SupportAdKind.REWARDED)
    }

    fun dismissed(token: Long) {
        if (!matches(token, SupportAdPhase.SHOWING)) return
        closed = true
        _state.value = if (earned && _state.value.kind == SupportAdKind.REWARDED)
            SupportAdState(SupportAdPhase.THANK_YOU, SupportAdKind.REWARDED) else SupportAdState()
    }

    fun cancelLoading() {
        if (_state.value.phase != SupportAdPhase.LOADING) return
        serial++
        _state.value = SupportAdState()
    }

    fun dismissThanks() {
        if (_state.value.phase == SupportAdPhase.THANK_YOU) _state.value = SupportAdState()
    }

    fun dispose() { active = false; serial++; _state.value = SupportAdState() }
    private fun matches(token: Long, phase: SupportAdPhase) = active && token == serial && _state.value.phase == phase
}
