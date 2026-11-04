package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.FreeRave.remindme.security.awaitAuth
import javax.inject.Inject
import com.FreeRave.remindme.security.mfa.FirebaseMfaChallenge
import com.FreeRave.remindme.security.mfa.MfaRequired
import com.google.firebase.auth.FirebaseAuthMultiFactorException

class FirebaseAuthServiceImpl @Inject constructor(
    private val firestoreService: FirebaseService,
    private val firebaseAuth: FirebaseAuth // Inject FirebaseAuth
) : FirebaseAuthServiceInterface {

    override fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: ""
    }

    override fun currentUserIdFlow(): Flow<String> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid ?: "")
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.uid ?: "")
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun getUserDisplayName(): String? {
        val user = firebaseAuth.currentUser ?: return null
        val rawName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.split("@")?.getOrNull(0)?.takeIf { it.isNotBlank() }
            ?: return null

        val parts = rawName.trim().split("\\s+".toRegex())
        return if (parts.size >= 2) "${parts[0]} ${parts[1]}" else parts.firstOrNull() ?: rawName
    }

    override suspend fun refreshCurrentUser() {
        firebaseAuth.currentUser?.reload()?.awaitAuth()
    }

    override suspend fun checkEmailVerification(
        onVerified: () -> Unit,
        onNotVerified: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            firebaseAuth.currentUser?.reload()?.awaitAuth()
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
        firebaseAuth.sendPasswordResetEmail(email).awaitAuth() // Use injected firebaseAuth
    }

    override suspend fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).awaitAuth() // Use injected firebaseAuth
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

            try { user.reauthenticate(credential).awaitAuth() }
            catch (e: FirebaseAuthMultiFactorException) {
                onFailure(MfaRequired(FirebaseMfaChallenge(e.resolver, user.uid, afterVerified = { verified ->
                    verified.updatePassword(newPassword).awaitAuth()
                })))
                return
            }

            // Step 2 — Update password
            user.updatePassword(newPassword).awaitAuth()

            onSuccess()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            onFailure(Exception("Current password is incorrect"))
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            onFailure(Exception("New password is too weak. Use at least 6 characters"))
        } catch (e: Exception) {
            onFailure(e)
        }
    }

}
