package com.FreeRave.remindme.service

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.FreeRave.remindme.model.User

interface GoogleAuthServiceInterface {
    /**
     * Gets the Intent required to start the Google Sign-In flow.
     */
    fun getSignInIntent(context: Context): Intent

    /**
     * Handles the result from the Google Sign-In Intent, authenticates with Firebase,
     * and invokes the appropriate callback.
     */
    suspend fun handleSignInResultAndAuthenticate(
        result: ActivityResult,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    )

    /**
     * Signs out the current user from Google and Firebase.
     * It's important to ensure this is only called if the user was signed in with Google.
     */
    suspend fun signOut(context: Context, onComplete: () -> Unit)
}
