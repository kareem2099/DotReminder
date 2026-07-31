package com.FreeRave.remindme.service

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.FreeRave.remindme.model.AuthProvider
import com.FreeRave.remindme.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : GoogleAuthServiceInterface {

    // Retrieved from google-services.json (client_type: 3)
    private val webClientId = "12141681876-qr8p7spogfpkqb7q49o3ohas2l5cdf42.apps.googleusercontent.com"

    override suspend fun signInWithGoogleCredentialManager(
        context: Context,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()

                authResult.user?.let { firebaseUser ->
                    val user = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: firebaseUser.email?.split("@")?.get(0) ?: "User",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        isEmailVerified = firebaseUser.isEmailVerified,
                        provider = AuthProvider.GOOGLE
                    )
                    onSuccess(user)
                } ?: onFailure(Exception("Firebase user not found after Google Sign-In."))
            } else {
                onFailure(Exception("Unrecognized credential type returned."))
            }
        } catch (e: GetCredentialException) {
            onFailure(e)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override suspend fun signOut(context: Context, onComplete: () -> Unit) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // Log error
        } finally {
            onComplete()
        }
    }
}
