package com.FreeRave.remindme.screens.auth

import android.content.Context
import com.FreeRave.remindme.service.GoogleSignInOnlyService
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth

internal class FirebaseSignOutGateway(
    private val auth: FirebaseAuth,
    private val googleSession: GoogleSignInOnlyService,
    private val context: Context
) : SignOutGateway {
    override suspend fun endSession() { auth.signOut() }
    override fun hasSession() = auth.currentUser != null
    override suspend fun clearProviderSessions() {
        // Attempt each provider independently, after the app session has ended.
        try { LoginManager.getInstance().logOut() }
        catch (_: RuntimeException) { /* Continue with Credential Manager cleanup. */ }
        googleSession.signOut(context)
    }
}
