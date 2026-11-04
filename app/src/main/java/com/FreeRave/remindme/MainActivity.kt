package com.FreeRave.remindme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.service.FacebookSignInOnlyService
import dagger.hilt.android.AndroidEntryPoint
import com.FreeRave.remindme.navigation.AppNavGraph
import com.FreeRave.remindme.navigation.Screen
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.core.view.WindowCompat
import kotlin.coroutines.resume

// In-App Update imports
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.InstallStateUpdatedListener
import androidx.appcompat.app.AppCompatActivity

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var facebookSignInOnlyService: FacebookSignInOnlyService

    private lateinit var consentInformation: ConsentInformation
    private var isConsentFlowComplete: Boolean = false

    // 🔥 In-App Update Manager
    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
         //Enable edge-to-edge and handle system bars properly
        enableEdgeToEdge()

        // This is the key line - ensures content draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

         //Set the status bar and navigation bar to transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // 🔥 Initialize In-App Update Manager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()
        registerUpdateListener()

        // Initialize Consent SDK
        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        if (!isConsentFlowComplete) {
            lifecycleScope.launch {
                requestConsentInfoUpdateAndShowForm()
                isConsentFlowComplete = true
                setupComposeContent()
            }
        } else {
            setupComposeContent()
        }
    }

    // 🔥 Check for updates
    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val flexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (updateAvailable && flexibleAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    // 🔥 Listener to detect when update is downloaded
    private fun registerUpdateListener() {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateReadySnackbar()
            }
        }
        appUpdateManager.registerListener(listener)
    }

    // 🔥 Show snackbar asking user to restart
    private fun showUpdateReadySnackbar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Update downloaded. Restart to apply.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Restart") {
            appUpdateManager.completeUpdate()
        }.show()
    }

    // Ensure snackbar shows if update finished while activity paused
    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateReadySnackbar()
            }
        }
    }

    // -------------------------------
    // Consent Form Logic (UNCHANGED)
    // -------------------------------

    private suspend fun requestConsentInfoUpdateAndShowForm() {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        suspendCancellableCoroutine<Unit> { continuation ->
            consentInformation.requestConsentInfoUpdate(
                this,
                params,
                { continuation.resume(Unit) },
                { formError ->
                    Log.w("UMP", "Consent info update failed: ${formError.message}")
                    continuation.resume(Unit)
                }
            )
        }

        if (consentInformation.isConsentFormAvailable) {
            loadAndShowConsentForm()
        }
    }

    private suspend fun loadAndShowConsentForm() {
        val consentForm = suspendCancellableCoroutine<ConsentForm?> { continuation ->
            UserMessagingPlatform.loadConsentForm(
                this,
                { form -> continuation.resume(form) },
                { error ->
                    Log.w("UMP", "Failed to load consent form: ${error.message}")
                    continuation.resume(null)
                }
            )
        }

        consentForm?.show(this) { error ->
            if (error != null) {
                Log.w("UMP", "Consent form error: ${error.errorCode} ${error.message}")
            }
        }
    }

    private fun setupComposeContent() {
        val navigateTo = intent?.getStringExtra("navigate_to")
        val reminderId = intent?.getStringExtra("reminder_id")
        val requestedDestination = intent?.getStringExtra("destination")

        val intentDestination = when {
            navigateTo == "add_reminder" -> Screen.Add.route
            navigateTo == "edit_reminder" && reminderId != null -> Screen.Edit.createRoute(reminderId)
            requestedDestination == "home" -> Screen.Home.route
            else -> null
        }

        setContent {

            val mainViewModel: MainViewModel = viewModel()
            val currentTheme by mainViewModel.currentTheme.collectAsState()

            val useDarkTheme = when (currentTheme) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
                ThemeSetting.OCEAN_BLUE -> isSystemInDarkTheme()
                ThemeSetting.FOREST_GREEN -> isSystemInDarkTheme()
                ThemeSetting.SUNSET_ORANGE -> isSystemInDarkTheme()
                ThemeSetting.MIDNIGHT_PURPLE -> true
                ThemeSetting.CANDY -> isSystemInDarkTheme()
            }

            RemindMeAppTheme(themeSetting = currentTheme, useDarkTheme = useDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
//                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    AppNavGraph(
                        startDestination = intentDestination ?: Screen.Splash.route,
                        modifier = Modifier
                    )
                }
            }
        }
    }

    @Deprecated("Use Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        facebookSignInOnlyService.passActivityResult(requestCode, resultCode, data)
    }
}
