package com.FreeRave.remindme.service

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialException
import com.FreeRave.remindme.model.AuthProvider
import com.FreeRave.remindme.model.User
import com.google.firebase.auth.FirebaseAuth
import com.FreeRave.remindme.security.awaitAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialSource: GoogleCredentialSource
) : GoogleAuthServiceInterface {

    override suspend fun signInWithGoogleCredentialManager(
        context: Context,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val authCredential = credentialSource.credential(context)
            val authResult = firebaseAuth.signInWithCredential(authCredential).awaitAuth()

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
        } catch (e: GetCredentialException) {
            onFailure(e)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override suspend fun signOut(context: Context, onComplete: () -> Unit) {
        // Local authentication must end even if the credential provider is unavailable.
        firebaseAuth.signOut()
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { /* Firebase is already signed out. */ }
        onComplete()
    }
}
