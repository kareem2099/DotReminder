package com.FreeRave.remindme.screens.support

import com.FreeRave.remindme.BuildConfig

/** Google's demo units keep support-screen development traffic separate from published ads. */
internal object SupportAdUnits {
    val banner = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else BuildConfig.BANNER_AD_UNIT_ID
    val standard = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else BuildConfig.INTERSTITIAL_AD_UNIT_ID
    val rewarded = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else BuildConfig.REWARDED_AD_UNIT_ID
}
