package com.FreeRave.remindme.service

import android.content.Context
import com.FreeRave.remindme.model.User

interface GoogleAuthServiceInterface {
    /**
     * Authenticates with Google using modern Credential Manager API.
     */
    suspend fun signInWithGoogleCredentialManager(
        context: Context,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    )

    /**
     * Signs out the current user from CredentialManager and Firebase.
     */
    suspend fun signOut(context: Context, onComplete: () -> Unit)
}
