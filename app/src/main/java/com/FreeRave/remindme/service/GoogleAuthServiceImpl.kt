package com.FreeRave.remindme.service

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.FreeRave.remindme.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth // Using injected FirebaseAuth
    // If you have a FirestoreService to save/update user, inject it here too
    // private val firestoreService: FirebaseService
) : GoogleAuthServiceInterface {

    // Retrieved from google-services.json (client_type: 3)
    private val webClientId = "12141681876-qr8p7spogfpkqb7q49o3ohas2l5cdf42.apps.googleusercontent.com"

    private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    override fun getSignInIntent(context: Context): Intent {
        return getGoogleSignInClient(context).signInIntent
    }

    override suspend fun handleSignInResultAndAuthenticate(
        result: ActivityResult,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)

            if (account?.idToken != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken!!, null) // idToken is non-null here
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                authResult.user?.let { firebaseUser ->
                    val user = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: firebaseUser.email?.split("@")?.get(0) ?: "User",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        isEmailVerified = firebaseUser.isEmailVerified,
                        provider = com.FreeRave.remindme.model.AuthProvider.GOOGLE // Set provider
                    )
                    // Optionally: Save/update user in Firestore here
                    // firestoreService.saveOrUpdateUser(user, { onSuccess(user) }, onFailure)
                    onSuccess(user)
                } ?: onFailure(Exception("Firebase user not found after Google Sign-In."))
            } else {
                // Handle case where account is null or idToken is null
                val message = if (account == null) "Google Sign-In failed to retrieve account." else "Google Sign-In failed, ID Token missing."
                // Construct Status object first, then ApiException
                val status = com.google.android.gms.common.api.Status(com.google.android.gms.common.api.CommonStatusCodes.INTERNAL_ERROR, message)
                onFailure(ApiException(status))
            }
        } catch (e: ApiException) {
            onFailure(e) // Forward API exceptions (e.g., user cancelled)
        } catch (e: Exception) {
            onFailure(e) // Forward other exceptions
        }
    }

    override suspend fun signOut(context: Context, onComplete: () -> Unit) {
        try {
            // It's important that getGoogleSignInClient is called with a valid context
            // that was used for sign-in, or at least an application context.
            getGoogleSignInClient(context).signOut().await()
            firebaseAuth.signOut() // Sign out from Firebase
        } catch (e: Exception) {
            // Log error or handle if necessary, e.g., Timber.e(e, "Error during sign out")
        } finally {
            onComplete()
        }
    }
}
