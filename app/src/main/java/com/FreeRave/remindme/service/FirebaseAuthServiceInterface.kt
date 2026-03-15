package com.FreeRave.remindme.service

import android.content.Context
import androidx.activity.result.ActivityResult
import com.FreeRave.remindme.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount // Added for the new method
import com.google.android.gms.auth.api.signin.GoogleSignInClient

interface FirebaseAuthServiceInterface {
    fun getCurrentUserId(): String
    suspend fun refreshCurrentUser()
    fun initializeGoogleSignIn(context: Context, clientId: String)
    fun getGoogleSignInClient(): GoogleSignInClient
    suspend fun handleGoogleSignInResult(result: ActivityResult): User? // This might be redundant now or serve a different purpose
    suspend fun signInWithGoogleAccount( // New method for Firebase Google Sign-In
        account: GoogleSignInAccount,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    )
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    )
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    )

    fun signOut()
    fun sendEmailVerification(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    )

    suspend fun checkEmailVerification(
        onVerified: () -> Unit,
        onNotVerified: () -> Unit,
        onFailure: (Exception) -> Unit
    )

    suspend fun updateEmailVerificationStatus(
        isVerified: Boolean,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    )
}
