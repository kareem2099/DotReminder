package com.FreeRave.remindme.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.FreeRave.remindme.model.User
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookSignInOnlyServiceImpl @Inject constructor() : FacebookSignInOnlyService {

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    override fun getCallbackManager(): CallbackManager {
        return callbackManager
    }

    // This 'signIn' method now correctly implements the interface method.
    override fun signIn(
        activity: ComponentActivity,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val request = GraphRequest.newMeRequest(result.accessToken) { obj, response ->
                    if (obj != null) {
                        try {
                            val user = User(
                                id = obj.optString("id", ""), // Facebook User ID
                                name = obj.optString("name", ""),
                                email = obj.optString("email", ""),
                                photoUrl = obj.optJSONObject("picture")
                                    ?.optJSONObject("data")
                                    ?.optString("url", "") ?: ""
                            )
                            onSuccess(user)
                        } catch (e: Exception) {
                            onError("Error parsing Facebook user data: ${e.message}")
                        }
                    } else {
                        val errorMsg = response?.error?.errorMessage ?: "Failed to fetch Facebook user data (unknown error)."
                        onError(errorMsg)
                    }
                }
                val parameters = Bundle()
                parameters.putString("fields", "id,name,email,picture.type(large)")
                request.parameters = parameters
                request.executeAsync()
            }

            override fun onCancel() {
                onError("Facebook login cancelled.")
            }

            override fun onError(error: FacebookException) {
                onError("Facebook login error: ${error.message ?: "Unknown Facebook error"}")
            }
        })

        // Trigger the login dialog
        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            // callbackManager, // Not needed here if registerCallback is used above for this specific call
            listOf("email", "public_profile")
        )
    }

    override fun passActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun signOut() {
        LoginManager.getInstance().logOut()
    }
}
