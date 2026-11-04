package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.review.ReviewEnvironment
import com.FreeRave.remindme.screens.settings.AccountSecurityScreenContent
import com.FreeRave.remindme.screens.settings.AccountSecurityUiState
import com.FreeRave.remindme.screens.settings.components.DeleteAccountRequestDialog
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Fake callbacks only: no browser/email handoff, real requests or account deletion in these tests. */
@RunWith(AndroidJUnit4::class)
class PrivacyUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() { assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner) }

    @Test fun accountSettingsExposePrivacyAndDeletionWithoutSubmittingARequest() {
        var privacy = 0
        var deletion = 0
        var support = 0
        compose.setContent {
            ReviewEnvironment("light") {
                AccountSecurityScreenContent(AccountSecurityUiState(), remember { SnackbarHostState() },
                    {}, {}, {}, {}, {}, {}, onPrivacy = { privacy++ }, onDeleteAccount = { deletion++ }, onContactSupport = { support++ })
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(context.getString(R.string.privacy_policy)))
        node(R.string.privacy_policy).performClick()
        node(R.string.privacy_contact_support).performScrollTo().performClick()
        node(R.string.deletion_title).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, privacy); assertEquals(1, deletion); assertEquals(1, support) }
        save("privacy-account-settings.png")
    }

    @Test fun requestExplainsScopeAndNeedsAnExplicitContinue() {
        var continues = 0
        compose.setContent {
            ReviewEnvironment("light") { DeleteAccountRequestDialog({ continues++ }, {}) }
        }
        node(R.string.deletion_scope).performScrollTo().assertIsDisplayed()
        node(R.string.deletion_timing).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, continues) }
        node(R.string.deletion_continue).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, continues) }
        save("privacy-deletion-request.png")
    }

    @Test fun largeRtlDialogCanBeCancelledWithoutAnyRequest() {
        var continues = 0
        var dismisses = 0
        compose.setContent {
            ReviewEnvironment("rtl") { DeleteAccountRequestDialog({ continues++ }, { dismisses++ }) }
        }
        node(R.string.deletion_warning).performScrollTo().assertIsDisplayed()
        node(R.string.reminder_cancel).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0, continues); assertEquals(1, dismisses) }
        save("privacy-deletion-request-rtl.png")
    }

    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun save(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
