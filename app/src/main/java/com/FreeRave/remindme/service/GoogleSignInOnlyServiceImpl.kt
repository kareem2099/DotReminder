package com.FreeRave.remindme.service

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInOnlyServiceImpl @Inject constructor() : GoogleSignInOnlyService {

    override suspend fun signOut(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignore if clear state fails
        }
    }
}
