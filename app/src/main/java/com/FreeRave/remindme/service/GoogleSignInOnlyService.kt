package com.FreeRave.remindme.service

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignInAccount // Changed from User model

interface GoogleSignInOnlyService {
    fun getSignInIntent(context: Context): Intent
    suspend fun handleSignInResult(result: ActivityResult): GoogleSignInAccount? // Changed return type
    suspend fun signOut(context: Context)
}
