package com.FreeRave.remindme.service

import androidx.activity.ComponentActivity // Or Activity, FragmentActivity
import com.FreeRave.remindme.model.User // Assuming User model can be used
import com.facebook.CallbackManager

interface FacebookSignInOnlyService {
    fun getCallbackManager(): CallbackManager // To be used by the calling Activity/Fragment/Composable
    
    // Initiates the login flow and registers internal callbacks for fetching user data
    fun signIn(
        activity: ComponentActivity,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    )

    // Helper to pass data to the CallbackManager from the Activity's onActivityResult
    fun passActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?)

    fun signOut()
}
