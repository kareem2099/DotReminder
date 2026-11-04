package com.FreeRave.remindme.service

import androidx.activity.ComponentActivity
import com.FreeRave.remindme.model.AuthProvider
import com.google.firebase.auth.AuthCredential
import com.FreeRave.remindme.security.mfa.MfaCancelled
import com.FreeRave.remindme.model.User
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.FreeRave.remindme.security.awaitAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookSignInOnlyServiceImpl @Inject constructor(private val firebaseAuth: FirebaseAuth) : FacebookSignInOnlyService {
    private val callbackManager = CallbackManager.Factory.create()
    override fun getCallbackManager(): CallbackManager = callbackManager

    override fun requestCredential(activity: ComponentActivity, onCredential: (AuthCredential) -> Unit, onFailure: (Exception) -> Unit) {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) { onCredential(FacebookAuthProvider.getCredential(result.accessToken.token)) }
            override fun onCancel() { onFailure(MfaCancelled()) }
            override fun onError(error: FacebookException) { onFailure(error) }
        })
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    override suspend fun signIn(activity: ComponentActivity, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            val credential = suspendCancellableCoroutine<AuthCredential> { continuation ->
                requestCredential(activity,
                    onCredential = { if (continuation.isActive) continuation.resume(it) },
                    onFailure = { if (continuation.isActive) continuation.resumeWithException(it) })
            }
            val user = firebaseAuth.signInWithCredential(credential).awaitAuth().user
                ?: throw IllegalStateException()
            onSuccess(User(id = user.uid, name = user.displayName.orEmpty(), email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString(), isEmailVerified = user.isEmailVerified, provider = AuthProvider.FACEBOOK))
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) { onFailure(error) }
    }

    override fun passActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun signOut() {
        LoginManager.getInstance().logOut()
        firebaseAuth.signOut()
    }
}
