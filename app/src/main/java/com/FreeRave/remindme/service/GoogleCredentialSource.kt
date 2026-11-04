package com.FreeRave.remindme.service

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.FreeRave.remindme.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject

/** Shared by sign-in and account-bound reauthentication. Never signs a Firebase user in. */
class GoogleCredentialSource @Inject constructor() {
    suspend fun credential(context: Context): AuthCredential {
        val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id)).setAutoSelectEnabled(false).build()
        val result = CredentialManager.create(context).getCredential(context,
            GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
        check(result is CustomCredential && result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        return GoogleAuthProvider.getCredential(GoogleIdTokenCredential.createFrom(result.data).idToken, null)
    }
}
