package com.FreeRave.remindme.service

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Added missing imports based on errors

@Singleton // Good practice for services
class GoogleSignInOnlyServiceImpl @Inject constructor() : GoogleSignInOnlyService {

    private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // The Web Client ID from your google-services.json
        val webClientId = "12141681876-qr8p7spogfpkqb7q49o3ohas2l5cdf42.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId) // Request ID token for Firebase
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    override fun getSignInIntent(context: Context): Intent {
        return getGoogleSignInClient(context).signInIntent
    }

    override suspend fun handleSignInResult(result: ActivityResult): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.getResult(ApiException::class.java) // Returns GoogleSignInAccount or throws ApiException
        } catch (e: ApiException) {
            // Log error or handle specific status codes, e.g., e.statusCode
            // Consider logging e.statusCode and e.message for better debugging
            null
        }
    }

    override suspend fun signOut(context: Context) {
        getGoogleSignInClient(context).signOut().await()
        // Also consider revoking access if you want a full sign-out,
        // but signOut is usually sufficient for most app sessions.
        // getGoogleSignInClient(context).revokeAccess().await()
    }
}
