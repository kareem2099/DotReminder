package com.FreeRave.remindme.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.FreeRave.remindme.BuildConfig
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(navController: NavController) {
    val context = LocalContext.current
    var showRewardDialog by remember { mutableStateOf(false) }
    var interstitialLoadError by remember { mutableStateOf(false) }
    var rewardedLoadError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Developer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { windowSizeClass, containerModifier ->
            AdaptiveColumn(
                modifier = containerModifier.padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdaptiveFormContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(16.dp))
                    ) {
                        Text(
                            text = "Thank you for supporting our app!\nView ads to help us.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = getResponsiveSpacing(16.dp))
                        )

                        // Banner Ad
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            factory = { ctx ->
                                AdView(ctx).apply {
                                    setAdSize(AdSize.BANNER)
                                    setAdUnitId(BuildConfig.BANNER_AD_UNIT_ID)
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(getResponsiveSpacing(16.dp)))

                        // Interstitial Ad Button
                        PrimaryButton(
                            text = if (interstitialLoadError) "Try Again - Interstitial Ad" else "Watch Interstitial Ad",
                            onClick = {
                                interstitialLoadError = false
                                InterstitialAd.load(context, BuildConfig.INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(),
                                    object : InterstitialAdLoadCallback() {
                                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                            interstitialAd.show(context as android.app.Activity)
                                        }
                                        override fun onAdFailedToLoad(adError: LoadAdError) {
                                            interstitialLoadError = true
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = getResponsiveSpacing(4.dp))
                        )

                        // Rewarded Ad Button
                        PrimaryButton(
                            text = if (rewardedLoadError) "Try Again - Rewarded Ad" else "Watch Rewarded Ad",
                            onClick = {
                                rewardedLoadError = false
                                RewardedAd.load(context, BuildConfig.REWARDED_AD_UNIT_ID, AdRequest.Builder().build(),
                                    object : RewardedAdLoadCallback() {
                                        override fun onAdLoaded(ad: RewardedAd) {
                                            ad.show(context as android.app.Activity) { rewardItem: RewardItem ->
                                                showRewardDialog = true
                                            }
                                        }
                                        override fun onAdFailedToLoad(adError: LoadAdError) {
                                            rewardedLoadError = true
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = getResponsiveSpacing(4.dp))
                        )
                    }
                }
            }
        }
    }

    // Reward Dialog
    if (showRewardDialog) {
        AlertDialog(
            onDismissRequest = { showRewardDialog = false },
            title = { Text("Thank You!") },
            text = { Text("Thank you for watching! Your support helps us improve the app. ❤️") },
            confirmButton = {
                TextButton(onClick = { showRewardDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}
