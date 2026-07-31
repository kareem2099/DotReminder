package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthServiceImpl @Inject constructor(
    private val firestoreService: FirebaseService,
    private val firebaseAuth: FirebaseAuth // Inject FirebaseAuth
) : FirebaseAuthServiceInterface {

    override fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: ""
    }

    override suspend fun refreshCurrentUser() {
        firebaseAuth.currentUser?.reload()?.await()
    }

    override suspend fun checkEmailVerification(
        onVerified: () -> Unit,
        onNotVerified: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            firebaseAuth.currentUser?.reload()?.await()
            val isVerified = firebaseAuth.currentUser?.isEmailVerified ?: false
            if (isVerified) {
                onVerified()
            } else {
                onNotVerified()
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override suspend fun updateEmailVerificationStatus(
        isVerified: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("No user logged in")
            firestoreService.updateUserField(
                userId = userId,
                field = "isEmailVerified",
                value = isVerified,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await() // Use injected firebaseAuth
    }

    override suspend fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await() // Use injected firebaseAuth
            authResult.user?.let { firebaseUser ->
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                onSuccess(user)
            } ?: onFailure(Exception("User not found"))
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut() // Use injected firebaseAuth
        // Note: Google sign out is handled by GoogleAuthServiceImpl now
    }

    override fun sendEmailVerification(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val user = firebaseAuth.currentUser // Use injected firebaseAuth
            user?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Email verification failed"))
                }
            } ?: onFailure(Exception("No user is signed in"))
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override suspend fun reauthenticateAndChangePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val user = firebaseAuth.currentUser
                ?: throw Exception("No user logged in")

            val email = user.email
                ?: throw Exception("User email not found")

            // Step 1 — Re-authenticate
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, currentPassword)

            user.reauthenticate(credential).await()

            // Step 2 — Update password
            user.updatePassword(newPassword).await()

            onSuccess()
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            onFailure(Exception("Current password is incorrect"))
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            onFailure(Exception("New password is too weak. Use at least 6 characters"))
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await() // Use injected firebaseAuth
            onSuccess()
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
