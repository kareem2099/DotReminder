package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.settings.ThemeOption
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.FreeRave.remindme.BuildConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeBottomSheet(
    showSheet: Boolean,
    availableThemes: List<ThemeOption>,
    selectedTheme: ThemeSetting,
    oceanBlueAdsWatched: Int,
    forestGreenAdsWatched: Int,
    sunsetOrangeAdsWatched: Int,
    midnightPurpleAdsWatched: Int,
    candyAdsWatched: Int,
    onThemeSelected: (ThemeSetting) -> Unit,
    onOceanBlueAdWatched: () -> Unit,
    onForestGreenAdWatched: () -> Unit,
    onSunsetOrangeAdWatched: () -> Unit,
    onMidnightPurpleAdWatched: () -> Unit,
    onCandyAdWatched: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Dialog states
    var showAdLoadingDialog by remember { mutableStateOf(false) }
    var showAdErrorDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showUnlockSuccessDialog by remember { mutableStateOf(false) }
    var unlockingTheme by remember { mutableStateOf<ThemeSetting?>(null) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Theme",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                availableThemes.forEach { themeOption ->
                    val isPremiumTheme = themeOption.setting == ThemeSetting.OCEAN_BLUE || themeOption.setting == ThemeSetting.FOREST_GREEN || themeOption.setting == ThemeSetting.SUNSET_ORANGE || themeOption.setting == ThemeSetting.MIDNIGHT_PURPLE || themeOption.setting == ThemeSetting.CANDY
                    val isUnlocked = when (themeOption.setting) {
                        ThemeSetting.OCEAN_BLUE -> oceanBlueAdsWatched >= 4
                        ThemeSetting.FOREST_GREEN -> forestGreenAdsWatched >= 4
                        ThemeSetting.SUNSET_ORANGE -> sunsetOrangeAdsWatched >= 4
                        ThemeSetting.MIDNIGHT_PURPLE -> midnightPurpleAdsWatched >= 4
                        ThemeSetting.CANDY -> candyAdsWatched >= 4
                        else -> true
                    }
                    val displayText = if (isPremiumTheme) {
                        if (isUnlocked) {
                            themeOption.displayName
                        } else {
                            val adsWatched = when (themeOption.setting) {
                                ThemeSetting.OCEAN_BLUE -> oceanBlueAdsWatched
                                ThemeSetting.FOREST_GREEN -> forestGreenAdsWatched
                                ThemeSetting.SUNSET_ORANGE -> sunsetOrangeAdsWatched
                                ThemeSetting.MIDNIGHT_PURPLE -> midnightPurpleAdsWatched
                                ThemeSetting.CANDY -> candyAdsWatched
                                else -> 0
                            }
                            "${themeOption.displayName} (${adsWatched}/4 ads watched)"
                        }
                    } else {
                        themeOption.displayName
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPremiumTheme && !isUnlocked) {
                                    // Show rewarded ad for premium theme
                                    showAdLoadingDialog = true
                                    RewardedAd.load(context, BuildConfig.REWARDED_AD_UNIT_ID, AdRequest.Builder().build(),
                                        object : RewardedAdLoadCallback() {
                                            override fun onAdLoaded(ad: RewardedAd) {
                                                showAdLoadingDialog = false
                                                ad.show(context as android.app.Activity) { rewardItem: RewardItem ->
                                                    // Ad watched successfully
                                                    unlockingTheme = themeOption.setting
                                                    when (themeOption.setting) {
                                                        ThemeSetting.OCEAN_BLUE -> {
                                                            onOceanBlueAdWatched()
                                                            val newCount = oceanBlueAdsWatched + 1
                                                            if (newCount >= 4) {
                                                                // Theme unlocked!
                                                                onThemeSelected(ThemeSetting.OCEAN_BLUE)
                                                                showUnlockSuccessDialog = true
                                                            } else {
                                                                // Show progress
                                                                showProgressDialog = true
                                                            }
                                                        }
                                                        ThemeSetting.FOREST_GREEN -> {
                                                            onForestGreenAdWatched()
                                                            val newCount = forestGreenAdsWatched + 1
                                                            if (newCount >= 4) {
                                                                // Theme unlocked!
                                                                onThemeSelected(ThemeSetting.FOREST_GREEN)
                                                                showUnlockSuccessDialog = true
                                                            } else {
                                                                // Show progress
                                                                showProgressDialog = true
                                                            }
                                                        }
                                                        ThemeSetting.SUNSET_ORANGE -> {
                                                            onSunsetOrangeAdWatched()
                                                            val newCount = sunsetOrangeAdsWatched + 1
                                                            if (newCount >= 4) {
                                                                // Theme unlocked!
                                                                onThemeSelected(ThemeSetting.SUNSET_ORANGE)
                                                                showUnlockSuccessDialog = true
                                                            } else {
                                                                // Show progress
                                                                showProgressDialog = true
                                                            }
                                                        }
                                                        ThemeSetting.MIDNIGHT_PURPLE -> {
                                                            onMidnightPurpleAdWatched()
                                                            val newCount = midnightPurpleAdsWatched + 1
                                                            if (newCount >= 4) {
                                                                // Theme unlocked!
                                                                onThemeSelected(ThemeSetting.MIDNIGHT_PURPLE)
                                                                showUnlockSuccessDialog = true
                                                            } else {
                                                                // Show progress
                                                                showProgressDialog = true
                                                            }
                                                        }
                                                        ThemeSetting.CANDY -> {
                                                            onCandyAdWatched()
                                                            val newCount = candyAdsWatched + 1
                                                            if (newCount >= 4) {
                                                                // Theme unlocked!
                                                                onThemeSelected(ThemeSetting.CANDY)
                                                                showUnlockSuccessDialog = true
                                                            } else {
                                                                // Show progress
                                                                showProgressDialog = true
                                                            }
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }
                                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                                showAdLoadingDialog = false
                                                showAdErrorDialog = true
                                            }
                                        }
                                    )
                                } else {
                                    // Regular theme selection
                                    onThemeSelected(themeOption.setting)
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(displayText, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))

                        if (!isUnlocked) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (themeOption.setting == selectedTheme) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Ad Loading Dialog
    if (showAdLoadingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Loading Ad") },
            text = { Text("Please wait while we load your rewarded ad...") },
            confirmButton = { }
        )
    }

    // Ad Error Dialog
    if (showAdErrorDialog) {
        AlertDialog(
            onDismissRequest = { showAdErrorDialog = false },
            title = { Text("Ad Not Available") },
            text = { Text("Sorry, we couldn't load a rewarded ad right now. Please try again later.") },
            confirmButton = {
                TextButton(onClick = { showAdErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Progress Dialog
    if (showProgressDialog) {
        val (newCount, themeName) = when (unlockingTheme) {
            ThemeSetting.OCEAN_BLUE -> oceanBlueAdsWatched + 1 to "Ocean Blue"
            ThemeSetting.FOREST_GREEN -> forestGreenAdsWatched + 1 to "Forest Green"
            ThemeSetting.SUNSET_ORANGE -> sunsetOrangeAdsWatched + 1 to "Sunset Orange"
            ThemeSetting.MIDNIGHT_PURPLE -> midnightPurpleAdsWatched + 1 to "Midnight Purple"
            ThemeSetting.CANDY -> candyAdsWatched + 1 to "Candy"
            else -> 1 to "theme"
        }
        AlertDialog(
            onDismissRequest = { showProgressDialog = false },
            title = { Text("Great Progress!") },
            text = { Text("You've watched $newCount/4 ads! Watch ${4 - newCount} more to unlock the $themeName theme.") },
            confirmButton = {
                TextButton(onClick = { showProgressDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }

    // Unlock Success Dialog
    if (showUnlockSuccessDialog) {
        val themeName = when (unlockingTheme) {
            ThemeSetting.OCEAN_BLUE -> "Ocean Blue"
            ThemeSetting.FOREST_GREEN -> "Forest Green"
            ThemeSetting.SUNSET_ORANGE -> "Sunset Orange"
            ThemeSetting.MIDNIGHT_PURPLE -> "Midnight Purple"
            ThemeSetting.CANDY -> "Candy"
            else -> "theme"
        }
        AlertDialog(
            onDismissRequest = {
                showUnlockSuccessDialog = false
                onDismiss()
            },
            title = { Text("Theme Unlocked! 🎉") },
            text = { Text("Congratulations! You've unlocked the $themeName theme. It has been applied to your app.") },
            confirmButton = {
                TextButton(onClick = {
                    showUnlockSuccessDialog = false
                    onDismiss()
                }) {
                    Text("Awesome!")
                }
            }
        )
    }
}
