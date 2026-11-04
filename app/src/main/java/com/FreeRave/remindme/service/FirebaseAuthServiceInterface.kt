package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.User
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthServiceInterface {
    fun getCurrentUserId(): String
    fun currentUserIdFlow(): Flow<String>
    fun getUserDisplayName(): String?
    suspend fun refreshCurrentUser()
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
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
