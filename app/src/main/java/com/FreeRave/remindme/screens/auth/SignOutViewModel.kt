package com.FreeRave.remindme.screens.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.service.GoogleAuthServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignOutViewModel @Inject constructor(
    private val googleAuthService: GoogleAuthServiceInterface,
    private val application: Application
) : ViewModel() {

    private val _signOutComplete = MutableStateFlow(false)
    val signOutComplete = _signOutComplete.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            googleAuthService.signOut(application) {
                _signOutComplete.value = true
            }
        }
    }
}
