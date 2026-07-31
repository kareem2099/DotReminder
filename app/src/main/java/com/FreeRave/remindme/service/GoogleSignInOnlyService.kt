package com.FreeRave.remindme.service

import android.content.Context

interface GoogleSignInOnlyService {
    suspend fun signOut(context: Context)
}
