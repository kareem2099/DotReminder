package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.User
interface FirebaseAuthServiceInterface {
    fun getCurrentUserId(): String
    suspend fun refreshCurrentUser()
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
    suspend fun reauthenticateAndChangePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    )
}
