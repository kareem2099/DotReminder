package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Stateless review only: never sends SMS, creates accounts or changes the signed-in session. */
@RunWith(AndroidJUnit4::class)
class SignUpUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
    }
    @Test fun lightDetailsReviewAndBottomActionsAreReachable() {
        render(false)
        save("signup-light-review.png")
        node(R.string.signup_send).performScrollTo().assertIsDisplayed()
        node(R.string.signup_confirm).assertIsDisplayed()
        save("signup-light-bottom-review.png")
        node(R.string.signup_sign_in).performScrollTo().assertIsDisplayed()
    }
    @Test fun darkDetailsReviewAndPasswordVisibility() {
        render(true)
        save("signup-dark-review.png")
        node(R.string.signup_confirm).performScrollTo()
        compose.onAllNodesWithContentDescription(context.getString(R.string.login_show_password))[0].performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.login_hide_password)).assertIsDisplayed()
        save("signup-dark-bottom-review.png")
    }
    @Test fun busyCodeStepDisablesEditsAndNavigation() {
        render(true, SignUpUiState(step = SignUpStep.CODE, busy = true, recipient = "+201012345678"))
        node(R.string.signup_code).assertIsNotEnabled()
        node(R.string.signup_edit).assertIsNotEnabled()
        node(R.string.signup_creating).performScrollTo().assertIsNotEnabled()
        node(R.string.signup_sign_in).performScrollTo().assertIsNotEnabled()
    }
    @Test fun codeStepShowsDestinationAndValidation() {
        render(true, SignUpUiState(step = SignUpStep.CODE, recipient = "+201012345678", submitted = true))
        node(R.string.signup_code_error).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.signup_code_intro, "+201012345678")).assertIsDisplayed()
        save("signup-code-review.png")
    }
    @Test fun profileFailureOffersRetryWithoutCreatingAnotherAccount() {
        render(false, SignUpUiState(step = SignUpStep.PROFILE, error = context.getString(R.string.signup_profile_error)))
        node(R.string.signup_profile_retry).performScrollTo().assertIsEnabled()
        node(R.string.signup_create).assertDoesNotExist()
        node(R.string.signup_sign_in).assertDoesNotExist()
    }
    @Test fun automaticallyVerifiedPhoneDoesNotAskForACode() {
        render(true, SignUpUiState(step = SignUpStep.CODE, recipient = "+201012345678", phoneVerified = true, submitted = true))
        node(R.string.signup_phone_verified).assertIsDisplayed()
        node(R.string.signup_code).assertDoesNotExist()
        node(R.string.signup_code_error).assertDoesNotExist()
        node(R.string.signup_create_verified).performScrollTo().assertIsEnabled()
    }
    @Test fun largeTextCanReachSubmitAndSignIn() {
        render(false, largeText = true)
        node(R.string.signup_send).performScrollTo().assertIsDisplayed()
        node(R.string.signup_sign_in).performScrollTo().assertIsDisplayed()
    }
    @Test fun completeStateShowsContinueWithoutFlipOrDelay() {
        render(true, SignUpUiState(step = SignUpStep.COMPLETE))
        node(R.string.signup_complete).assertIsDisplayed()
        node(R.string.signup_continue).performScrollTo().assertIsEnabled()
        save("signup-complete-review.png")
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun render(dark: Boolean, state: SignUpUiState = SignUpUiState(), largeText: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
                RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
                    GlassTheme {
                        SignUpScreenContent(state, onEdit = { _, _ -> }, onSend = {}, onCreate = {}, onEditDetails = {},
                            onRetry = {}, onLogin = {}, onContinue = {})
                    }
                }
            }
        }
    }
    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
