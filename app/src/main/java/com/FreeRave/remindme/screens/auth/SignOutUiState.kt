package com.FreeRave.remindme.screens.auth

internal data class SignOutAccount(val name: String = "", val email: String = "")
internal enum class SignOutStatus { READY, BUSY, COMPLETE, ERROR }
internal data class SignOutUiState(val account: SignOutAccount? = null, val status: SignOutStatus = SignOutStatus.READY) {
    val busy get() = status == SignOutStatus.BUSY || status == SignOutStatus.COMPLETE
}
