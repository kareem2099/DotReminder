package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Fake callbacks and a separate navigation graph: never signs out the real account. */
@RunWith(AndroidJUnit4::class)
class SignOutUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val ready = SignOutUiState(SignOutAccount("Kareem", "user@example.com"))
    @Before fun isolatedRunnerOnly() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
    }
    @Test fun lightLayoutShowsAccountAndBothActions() {
        render(false)
        compose.onNodeWithText("user@example.com").assertIsDisplayed()
        confirm().performScrollTo().assertIsEnabled()
        node(R.string.signout_cancel).performScrollTo().assertIsEnabled()
        save("signout-light-review.png")
    }
    @Test fun darkLayoutReview() {
        render(true)
        node(R.string.signout_cancel).performScrollTo().assertIsDisplayed()
        save("signout-dark-review.png")
    }
    @Test fun busyStateDisablesConfirmCancelAndToolbarBack() {
        render(true, ready.copy(status = SignOutStatus.BUSY))
        node(R.string.signout_loading).performScrollTo().assertIsNotEnabled()
        node(R.string.signout_cancel).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.signout_back)).assertIsNotEnabled()
    }
    @Test fun failureOffersRetryAndAllowsCancel() {
        render(false, ready.copy(status = SignOutStatus.ERROR))
        node(R.string.signout_error).performScrollTo().assertIsDisplayed()
        node(R.string.signout_retry).performScrollTo().assertIsEnabled()
        node(R.string.signout_cancel).performScrollTo().assertIsEnabled()
    }
    @Test fun cancelDoesNotSubmitTheSignOutCallback() {
        var confirms = 0
        var cancels = 0
        render(true, onConfirm = { confirms++ }, onCancel = { cancels++ })
        node(R.string.signout_cancel).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, confirms); assertEquals(1, cancels) }
    }
    @Test fun completionShowsRedirectWithoutAnotherSignOutAction() {
        render(true, ready.copy(status = SignOutStatus.COMPLETE))
        node(R.string.signout_complete).assertIsDisplayed()
        node(R.string.signout_cancel).assertDoesNotExist()
        confirm().assertDoesNotExist()
    }
    @Test fun largeTextAndLongEmailCanReachBothActions() {
        render(false, ready.copy(account = SignOutAccount("Long Account Name", "long.email.for.layout.review@example.com")), largeText = true)
        confirm().performScrollTo().assertIsDisplayed()
        node(R.string.signout_cancel).performScrollTo().assertIsDisplayed()
    }
    @Test fun accountWithoutDisplayNameUsesEmailOnce() {
        render(false, ready.copy(account = SignOutAccount(email = "user@example.com")))
        compose.onAllNodesWithText("user@example.com").assertCountEquals(1)
    }
    @Test fun successfulNavigationClearsHomeSettingsAndConfirmationFromHistory() {
        lateinit var nav: NavHostController
        compose.setContent {
            ReviewTheme(false) {
                nav = rememberNavController()
                NavHost(nav, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) { Text("Review home") }
                    composable(Screen.Settings.route) { Text("Review settings") }
                    composable(Screen.SignOut.route) {
                        SignOutScreenContent(ready, onConfirm = { navigateAfterSignOut(nav) }, onCancel = { nav.popBackStack() })
                    }
                    composable(Screen.Login.route) { Text("Review sign in") }
                }
            }
        }
        compose.runOnIdle { nav.navigate(Screen.Settings.route); nav.navigate(Screen.SignOut.route) }
        confirm().performScrollTo().performClick()
        compose.onNodeWithText("Review sign in").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(Screen.Login.route, nav.currentDestination?.route)
            assertNull(nav.previousBackStackEntry)
            assertFalse(nav.popBackStack(Screen.Home.route, false))
        }
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun confirm() = compose.onNode(hasText(context.getString(R.string.signout_confirm)) and hasClickAction())
    private fun render(dark: Boolean, state: SignOutUiState = ready, largeText: Boolean = false,
        onConfirm: () -> Unit = {}, onCancel: () -> Unit = {}) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
                ReviewTheme(dark) { SignOutScreenContent(state, onConfirm, onCancel) }
            }
        }
    }
    @Composable private fun ReviewTheme(dark: Boolean, content: @Composable () -> Unit) {
        RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) { GlassTheme(content) }
    }
    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
