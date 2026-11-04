package com.FreeRave.remindme.screens.auth

import android.app.Application
import android.app.Activity
import android.content.Context
import android.content.ComponentName
import android.os.Bundle
import android.view.WindowManager
import androidx.test.runner.AndroidJUnitRunner
import com.FreeRave.remindme.MainActivity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

/** Isolates component review from app-start ads and authentication side effects. */
class LoginUiReviewRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        val app = super.newApplication(cl, Application::class.java.name, context)
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) {
                if (activity is ComponentActivity) activity.enableEdgeToEdge()
                // Apply the real host policy before its first window layout, including IME resize.
                val policy = activity.packageManager.getActivityInfo(ComponentName(activity, MainActivity::class.java), 0).softInputMode
                activity.window.setSoftInputMode(policy)
                // Only fake component-review activities appear over the lock screen; the device stays locked.
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        return app
    }
}
