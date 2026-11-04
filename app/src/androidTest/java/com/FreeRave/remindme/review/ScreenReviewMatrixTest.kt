package com.FreeRave.remindme.review

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.*
import com.FreeRave.remindme.screens.add.AddReminderContent
import com.FreeRave.remindme.screens.ai.*
import com.FreeRave.remindme.screens.auth.*
import com.FreeRave.remindme.screens.calendar.*
import com.FreeRave.remindme.screens.category.*
import com.FreeRave.remindme.screens.home.*
import com.FreeRave.remindme.screens.settings.*
import com.FreeRave.remindme.screens.support.*
import com.FreeRave.remindme.screens.templates.*
import com.FreeRave.remindme.ui.reminder.*
import com.FreeRave.remindme.ui.theme.*
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.time.*

/** All fifteen screen contents, with fake data/callbacks; no SDK requests or account mutations. */
@RunWith(Parameterized::class)
class ScreenReviewMatrixTest(private val page: String, private val mode: String) {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() { assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner) }
    @Test fun screenFitsAndItsHeadingIsReadable() {
        compose.setContent {
            ReviewEnvironment(mode) { ReviewScreen(page) }
        }
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "review-$page-$mode.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val title = context.getString(heading(page))
        val node = compose.onAllNodesWithText(title, useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.boundsInRoot.height > 0 && it.boundsInRoot.width > 0 }
            .minByOrNull { it.boundsInRoot.top }
        assertNotNull("$page / $mode: heading is not visible", node)
        val headingNode = requireNotNull(node)
        val layouts = mutableListOf<TextLayoutResult>()
        headingNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        layouts.forEach { layout ->
            // Intrinsic text widths can exceed their rounded integer size by a fraction of a pixel.
            val widestLine = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
            assertTrue("$page / $mode: heading width clipped", widestLine <= layout.size.width + 1f)
            assertFalse("$page / $mode: heading height clipped", layout.didOverflowHeight)
            assertTrue("$page / $mode: heading text truncated", layout.getLineEnd(layout.lineCount - 1, true) >= title.length)
            assertTrue("$page / $mode: heading clipped by parent (${headingNode.boundsInRoot.height} < ${layout.size.height})",
                headingNode.boundsInRoot.height + 3 >= layout.size.height)
        }
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0} · {1}") fun cases() =
            listOf("home", "calendar", "add", "edit", "categories", "templates", "settings", "security", "sessions", "support", "login", "signup", "email", "signout", "assistant")
                .flatMap { page -> listOf("light", "compact-dark", "rtl").map { mode -> arrayOf(page, mode) } }
                .filter { values ->
                    val args = InstrumentationRegistry.getArguments()
                    (args.getString("reviewPage") == null || args.getString("reviewPage") == values[0]) &&
                        (args.getString("reviewMode") == null || args.getString("reviewMode") == values[1])
                }
    }
}

@Composable
internal fun ReviewEnvironment(mode: String, content: @Composable () -> Unit) {
    val originalDensity = LocalDensity.current
    val originalConfig = LocalConfiguration.current
    val compact = mode != "light"
    val width = if (compact) 320 else originalConfig.screenWidthDp
    val density = originalDensity.density * originalConfig.screenWidthDp / width
    val config = Configuration(originalConfig).apply {
        screenWidthDp = width
        screenHeightDp = (originalConfig.screenHeightDp * originalDensity.density / density).toInt()
        fontScale = if (compact) 1.5f else 1f
    }
    CompositionLocalProvider(LocalDensity provides Density(density, config.fontScale), LocalConfiguration provides config,
        LocalLayoutDirection provides if (mode == "rtl") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        RemindMeAppTheme(if (mode == "light") ThemeSetting.LIGHT else ThemeSetting.DARK, useDarkTheme = mode != "light") {
            GlassTheme(content)
        }
    }
}

private fun heading(page: String) = when (page) {
    "home" -> R.string.app_name; "calendar" -> R.string.calendar_title; "add" -> R.string.reminder_new; "edit" -> R.string.reminder_edit
    "categories" -> R.string.categories_title; "templates" -> R.string.templates_title; "settings" -> R.string.settings_title
    "security" -> R.string.security_title; "sessions" -> R.string.sessions_title; "support" -> R.string.support_title
    "login" -> R.string.login_welcome; "signup" -> R.string.signup_title; "email" -> R.string.email_verification_title
    "signout" -> R.string.signout_title; else -> R.string.ai_chat_title
}

@Composable
private fun ReviewScreen(page: String) {
    val snackbar = remember { SnackbarHostState() }
    val now = 1_793_361_600_000L
    val reminder = Reminder(id = "review", title = "A little time to reset", description = "Step outside and clear your mind.",
        reminderTime = now + 3_600_000, category = "Personal")
    val draft = ReminderDraft(title = reminder.title, description = reminder.description, reminderTimeMillis = reminder.reminderTime, category = "Personal")
    when (page) {
        "home" -> HomeScreenContent(HomeUiState(reminders = listOf(reminder), allReminders = listOf(reminder), userName = "Kareem"), now,
            onNavigate = {}, onSearchChange = {}, onCategoryChange = {}, onQuickFilterChange = {}, onRefresh = {}, onDelete = {}, onResetFilters = {})
        "calendar" -> CalendarScreenContent(buildCalendarState(LocalDate.of(2026, 10, 30), listOf(reminder), LocalDate.of(2026, 10, 30), ZoneId.of("Africa/Cairo"), DayOfWeek.SATURDAY),
            onBack = {}, onSelectDate = {}, onChangeMonth = {}, onToday = {}, onRetry = {}, onReminderClick = {})
        "add" -> AddReminderContent(draft, emptyList(), emptySet(), 0, false, false, snackbar, onEvent = {})
        "edit" -> ReminderFormContent(draft, emptyList(), emptySet(), 0, false, false, snackbar, onEvent = {}, mode = ReminderFormMode.EDIT, referenceId = "review")
        "categories" -> CategoriesScreenContent(CategoriesUiState(isLoading = false), snackbar, onBack = {}, onCreate = {}, onEdit = {}, onDelete = {},
            onQueryChange = {}, onFilterChange = {}, onReset = {}, onRetry = {})
        "templates" -> TemplatesScreenContent(TemplatesUiState(templates = listOf(ReminderTemplate(id = "review", name = "A little time to reset", title = reminder.title)),
            isLoading = false, isAuthenticated = true), snackbar, onBack = {}, onCreate = {}, onUse = {}, onEdit = {}, onDelete = {},
            onQuery = {}, onFilter = {}, onRetry = {}, onReset = {})
        "settings" -> SettingsScreenContent(SettingsUiState(defaultHour = 9, defaultMinute = 30), "Default sound", snackbar,
            onBack = {}, onPanel = {}, onVibrationChanged = {}, onClearDefaultTime = {}, onSecurity = {}, onSignOut = {}, onSupport = {}, onAppOpenAdsChanged = {})
        "security" -> AccountSecurityScreenContent(AccountSecurityUiState(), snackbar, onBack = {}, onPassword = {}, onBiometric = {}, onDevices = {}, onMfaAdd = {}, onVerifyEmail = {})
        "sessions" -> ActiveSessionsScreenContent(ActiveSessionsUiState(sessions = listOf(Session("review", "OnePlus", "IN2025", Timestamp(1_790_000_000, 0), Timestamp(1_790_000_000, 0))),
            currentSessionId = "review", identityLoaded = true, isLoading = false), snackbar, onBack = {}, onRetry = {}, onRemove = {}, onClearOthers = {})
        "support" -> SupportScreenContent(SupportAdState(), onBack = {}, onRequest = {}, onCancel = {}, onThanked = {}, banner = { Text("Banner review placeholder") })
        "login" -> LoginScreenContent(LoginUiState(), snackbar, onEmail = {}, onPassword = {}, onRemember = {}, onSubmit = {}, onForgot = {}, onGoogle = {}, onFacebook = {}, onBiometric = {}, onSignUp = {})
        "signup" -> SignUpScreenContent(SignUpUiState(), onEdit = { _, _ -> }, onSend = {}, onCreate = {}, onEditDetails = {}, onRetry = {}, onLogin = {}, onContinue = {})
        "email" -> VerifyEmailScreenContent(EmailVerificationUiState(phase = EmailVerificationPhase.READY, account = VerificationAccount("review", "user@example.com", false)),
            onBack = {}, onCheck = {}, onSend = {}, onRetry = {}, onRetrySync = {}, onContinue = {}, onLogin = {})
        "signout" -> SignOutScreenContent(SignOutUiState(SignOutAccount("Kareem", "user@example.com")), onConfirm = {}, onCancel = {})
        "assistant" -> AiChatScreenContent(AiChatUiState(), onBack = {}, onInput = {}, onSend = {}, onRetry = {}, onClear = {}, onReview = {}, onManual = {})
    }
}
