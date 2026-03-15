package com.FreeRave.remindme.service

import android.content.Context
import androidx.activity.result.ActivityResult
import com.FreeRave.remindme.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount // Ensure this is imported for the new method
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthServiceImpl @Inject constructor(
    private val firestoreService: FirebaseService,
    private val firebaseAuth: FirebaseAuth // Inject FirebaseAuth
) : FirebaseAuthServiceInterface {
    // private val auth: FirebaseAuth = Firebase.auth // Remove, use injected firebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient // This is for a different Google Sign-In flow, might be removable later

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

    override fun initializeGoogleSignIn(context: Context, clientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    override fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient

    override suspend fun handleGoogleSignInResult(result: ActivityResult): User? {
        // This method might need to be re-evaluated or removed if
        // the new signInWithGoogleAccount covers all Google Sign-In to Firebase cases.
        // For now, keeping its existing implementation.
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = task.await() // account is GoogleSignInAccount
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await() // Use injected firebaseAuth
        return authResult.user?.let { firebaseUser ->
            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                isEmailVerified = firebaseUser.isEmailVerified // Assuming User model has this
            )
        }
    }

    override suspend fun signInWithGoogleAccount(
        account: GoogleSignInAccount,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null) // idToken is non-null here in previous logic
            val authResult = firebaseAuth.signInWithCredential(credential).await() // Use injected firebaseAuth
            authResult.user?.let { firebaseUser ->
                // Optionally: Check if user exists in Firestore and create/update if necessary
                // For example, using firestoreService.saveUser(newUser)
                val user = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: firebaseUser.email?.split("@")?.get(0) ?: "User",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    isEmailVerified = firebaseUser.isEmailVerified // Assuming User model has this field
                )
                // Example: Save or update user in Firestore
                // firestoreService.saveOrUpdateUser(user, onSuccess = { onSuccess(user) }, onFailure = onFailure)
                // For simplicity now, just call onSuccess directly
                onSuccess(user)
            } ?: onFailure(Exception("Firebase user not found after Google Sign-In."))
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
