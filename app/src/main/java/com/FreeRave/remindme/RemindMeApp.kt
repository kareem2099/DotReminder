package com.FreeRave.remindme

import android.app.Activity
import android.app.Application
import android.app.AlertDialog
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.utils.AdManager
import com.FreeRave.remindme.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
// AppCoins related imports are removed.
// import java.util.Locale // Keep if used elsewhere, remove if only for AppCoins

@HiltAndroidApp
class RemindMeApp : Application() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var adManager: AdManager

    companion object {
        lateinit var instance: RemindMeApp
            private set
        private const val TAG = "RemindMeApp" // Changed TAG slightly as it's no longer just for Billing
    }

    // All AppCoins Billing related properties and listeners are removed.
    // private lateinit var cab: AppcoinsBillingClient
    // private var appcoinsBillingKeyFromConfig: String = ""
    // private val appCoinsBillingStateListener: AppCoinsBillingStateListener = ...
    // private fun mapResponseCode(code: Int): ResponseCode? = ...
    // private val purchasesUpdatedListener = ...
    // private val consumeResponseListener = ...
    // private val skuDetailsResponseListener = ...

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Google Mobile Ads
        MobileAds.initialize(this) { status ->
            Log.d(TAG, "Ads Initialization completed: $status")
        }

        // Initialize Ad Manager for app-open ads
        adManager = AdManager(this)

        // Load user preferences (example: try to load selected theme to trigger datastore init)
        applicationScope.launch {
            try {
                userPreferencesRepository.selectedTheme.first()
                Log.d(TAG, "User preferences initialized (attempted to load theme)")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing preferences: ${e.message}", e)
            }
        }

        // Initialize AppCoins Billing - REMOVED
        // initAppCoinsBilling()
        Log.d(TAG, "AppCoins Billing initialization skipped as it's being removed.")
    }

    // All AppCoins Billing related methods are removed.
    // private fun initAppCoinsBilling() { ... }
    // fun checkPendingConsumables() { ... }
    // fun checkSubscriptions() { ... }
    // private fun queryInapps() { ... }
    // private fun querySubs() { ... }
    // fun startPurchase(...) { ... }
    // private fun isFreeTrialSubscription(...) { ... }
    // private fun handlePurchaseSuccess(purchase: Purchase) { ... }
    // private suspend fun handleActiveSubscription(purchase: Purchase) { ... }
    // private fun isConsumableProduct(sku: String): Boolean { ... }
    // private fun checkUserEligibleForFreeTrial(...): Boolean { ... }

    // Public utility methods related to AppCoins Billing are removed or stubbed.
    fun setAppOpenAdsEnabled(enabled: Boolean) {
        adManager.setEnabled(enabled)
    }

    fun isBillingReady(): Boolean {
        Log.d(TAG, "isBillingReady called - AppCoins Billing removed, returning false.")
        return false // Billing is no longer integrated
    }

    // fun getBillingClient(): AppcoinsBillingClient? {
    //     Log.d(TAG, "getBillingClient called - AppCoins Billing removed, returning null.")
    //     return null // Billing client is no longer available
    // }

    override fun onTerminate() {
        super.onTerminate()
        // AppCoins Billing connection termination removed.
        // try {
        //     if (::cab.isInitialized) {
        //         cab.endConnection()
        //         Log.d(TAG, "AppCoins Billing connection terminated")
        //     }
        // } catch (e: Exception) {
        //     Log.e(TAG, "Error terminating billing connection: ${e.message}", e)
        // }
        Log.d(TAG, "onTerminate called.")
    }
}
