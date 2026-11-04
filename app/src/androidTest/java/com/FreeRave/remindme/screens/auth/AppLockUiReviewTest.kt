package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.review.ReviewEnvironment
import com.FreeRave.remindme.security.lock.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Tests the gate with synthetic sessions; never changes the device's biometric preference. */
@RunWith(AndroidJUnit4::class)
class AppLockUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() { assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner) }

    @Test fun checkingAndCancelledUnlockNeverShowProtectedContent() {
        var attempts = 0
        compose.setContent {
            ReviewEnvironment("light") {
                ProtectedAppContent(false, locked = {
                    AppLockScreenContent(AppLockState(AppLockPhase.LOCKED, "fake-user", true, error = AppLockError.CANCELLED),
                        { attempts++ }, {}, {})
                }) { Text("Private reminders") }
            }
        }
        compose.onNodeWithText("Private reminders").assertDoesNotExist()
        node(R.string.app_lock_cancelled).assertIsDisplayed()
        node(R.string.app_lock_unlock).performClick()
        compose.runOnIdle { assertEquals(1, attempts) }
        compose.onNodeWithText("Private reminders").assertDoesNotExist()
    }
    @Test fun lockingAlsoRemovesNativeDialogsAndTheirAccessibilityText() {
        var unlocked by mutableStateOf(true)
        compose.setContent {
            ReviewEnvironment("light") {
                ProtectedAppContent(unlocked, locked = { Text("Account locked") }) {
                    Text("Private reminders")
                    AlertDialog({}, confirmButton = {}, title = { Text("Private account dialog") })
                }
            }
        }
        compose.onNodeWithText("Private account dialog").assertIsDisplayed()
        compose.runOnIdle { unlocked = false }
        compose.onNodeWithText("Account locked").assertIsDisplayed()
        compose.onNodeWithText("Private reminders").assertDoesNotExist()
        compose.onNodeWithText("Private account dialog").assertDoesNotExist()
    }
    class PendingViewModel : ViewModel() { var cleared = false; override fun onCleared() { cleared = true } }
    @Test fun aSavedFormAndItsNavigationViewModelSurviveAnAppLock() {
        var unlocked by mutableStateOf(true)
        lateinit var nav: NavHostController
        lateinit var pending: PendingViewModel
        compose.setContent {
            nav = rememberNavController()
            ReviewEnvironment("light") {
                ProtectedAppContent(unlocked, locked = { Text("Account locked") }) {
                    NavHost(nav, "home") {
                        composable("home") { Button({ nav.navigate("form") }) { Text("Open form") } }
                        composable("form") {
                            pending = viewModel()
                            var draft by rememberSaveable { mutableStateOf("") }
                            OutlinedTextField(draft, { draft = it }, label = { Text("Draft title") })
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Open form").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Keep this draft")
        val original = pending
        compose.runOnIdle { unlocked = false }
        compose.onNodeWithText("Keep this draft").assertDoesNotExist()
        compose.runOnIdle { assertFalse(original.cleared); unlocked = true }
        compose.onNode(hasSetTextAction()).assertTextContains("Keep this draft")
        compose.runOnIdle { assertSame(original, pending); assertEquals("form", nav.currentDestination?.route) }
    }
    @Test fun unavailableBiometricsOfferAnExplicitRealSignInFallback() {
        var requests = 0
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                AppLockScreenContent(AppLockState(AppLockPhase.LOCKED, "fake", true, error = AppLockError.UNAVAILABLE), {}, { requests++ }, {})
            }
        }
        node(R.string.app_lock_sign_in).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, requests) }
        screenshot("app-lock-compact.png")
    }
    @Test fun lockedScreenSupportsLargeRtlText() {
        compose.setContent {
            ReviewEnvironment("rtl") { AppLockScreenContent(AppLockState(AppLockPhase.LOCKED, "fake", true), {}, {}, {}) }
        }
        node(R.string.app_lock_unlock).performScrollTo().assertIsDisplayed()
        node(R.string.app_lock_sign_in).performScrollTo().assertIsDisplayed()
        screenshot("app-lock-rtl.png")
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun screenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
