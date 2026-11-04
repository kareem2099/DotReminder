package com.FreeRave.remindme.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(auth: FirebaseAuth, firestore: FirebaseFirestore) : ViewModel() {
    private val controller = EmailVerificationController(FirebaseEmailVerificationGateway(auth, firestore))
    internal val state = controller.state
    init {
        viewModelScope.launch {
            state.map { it.resendSeconds > 0 }.distinctUntilChanged().collectLatest { active ->
                while (active) { delay(1_000); controller.tick() }
            }
        }
    }
    fun refresh(showPending: Boolean = false) { viewModelScope.launch { controller.refresh(showPending) } }
    fun sendLink() { viewModelScope.launch { controller.sendLink() } }
    fun retrySync() { viewModelScope.launch { controller.retrySync() } }
}
