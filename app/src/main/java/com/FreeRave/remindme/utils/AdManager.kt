package com.FreeRave.remindme.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.FreeRave.remindme.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AdManager(private val app: Application, private val canShow: () -> Boolean = { true }) : DefaultLifecycleObserver {
    private var enabled: Boolean = true

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
    private var currentActivity: Activity? = null

    companion object {
        const val LOG_TAG = "AdManager"
        const val AD_UNIT_ID = BuildConfig.APP_OPEN_AD_UNIT_ID
    }

    private val prefs: SharedPreferences = app.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)

    init {
        enabled = prefs.getBoolean("app_open_enabled", true)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        app.registerActivityLifecycleCallbacks(ActivityLifecycleCallback())
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        showAdIfAvailable()
        Log.d(LOG_TAG, "onStart")
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean("app_open_enabled", enabled).apply()
    }

    fun showAdIfAvailable() {
        if (enabled && canShow() && !isShowingAd && isAdAvailable()) {
            Log.d(LOG_TAG, "Will show ad.")
            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd(app)
                    Log.d(LOG_TAG, "Ad dismissed fullscreen content.")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(LOG_TAG, "Ad failed to show fullscreen content.")
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d(LOG_TAG, "Ad showed fullscreen content.")
                }
            }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            currentActivity?.let { appOpenAd?.show(it) }
        } else {
            Log.d(LOG_TAG, "Ad is not ready yet.")
        }
    }

    private fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    Log.d(LOG_TAG, "Ad was loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(LOG_TAG, "Ad failed to load with error: $loadAdError")
                    isLoadingAd = false
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    private inner class ActivityLifecycleCallback : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            currentActivity = null
        }
    }
}
