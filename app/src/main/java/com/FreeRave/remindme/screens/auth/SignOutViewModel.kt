package com.FreeRave.remindme.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.service.GoogleSignInOnlyService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignOutViewModel @Inject constructor(
    auth: FirebaseAuth,
    googleSession: GoogleSignInOnlyService,
    @ApplicationContext context: Context
) : ViewModel() {
    private val controller = SignOutController(FirebaseSignOutGateway(auth, googleSession, context),
        auth.currentUser?.let { SignOutAccount(it.displayName.orEmpty(), it.email.orEmpty()) })
    internal val state = controller.state
    fun signOut() { viewModelScope.launch { controller.signOut() } }
}
