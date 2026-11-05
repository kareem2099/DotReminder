package com.FreeRave.remindme.review

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.*
import com.FreeRave.remindme.screens.add.AddReminderContent
import com.FreeRave.remindme.screens.ai.*
import com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
import com.FreeRave.remindme.screens.calendar.*
import com.FreeRave.remindme.screens.home.*
import com.FreeRave.remindme.screens.settings.*
import com.FreeRave.remindme.screens.templates.*
import com.FreeRave.remindme.security.mfa.*
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Renders production Compose content with local fixtures; never signs in or saves demo records. */
@RunWith(Parameterized::class)
class StoreListingScreenshotTest(private val page: String, private val tablet: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Before fun requireIsolatedRenderer() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
    }

    @Test fun renderStoreAsset() {
        compose.setContent {
            val original = LocalConfiguration.current
            val physicalDensity = LocalDensity.current.density
            val width = if (tablet) 840 else original.screenWidthDp
            val height = if (tablet) 1120 else original.screenHeightDp
            val density = physicalDensity * original.screenWidthDp / width
            val config = Configuration(original).apply { screenWidthDp = width; screenHeightDp = height; fontScale = 1f }
            val dark = page in listOf("add", "assistant", "security")
            CompositionLocalProvider(LocalConfiguration provides config,
                LocalDensity provides Density(density, 1f), LocalLayoutDirection provides LayoutDirection.Ltr) {
                RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
                    GlassTheme {
                        Box(Modifier.requiredSize(width.dp, height.dp).testTag("store-capture")) { StorePage(page) }
                    }
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("store-capture").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "store-${if (tablet) "tablet" else "phone"}-$page.png"
        File(context.cacheDir, filename).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0} tablet={1}") fun cases() =
            listOf("home", "add", "calendar", "assistant", "templates", "security").map { arrayOf(it, false) } +
                listOf("home", "add", "calendar", "templates").map { arrayOf(it, true) }
    }
}

private val storeDate = LocalDate.of(2026, 11, 4)
private val storeZone = ZoneId.of("Africa/Cairo")
private fun storeTime(hour: Int, minute: Int = 0, days: Long = 0) =
    storeDate.plusDays(days).atTime(hour, minute).atZone(storeZone).toInstant().toEpochMilli()
private val storeCategories = listOf(
    Category(id = "work", name = "Work", colorHex = "#269F91", icon = "💼"),
    Category(id = "personal", name = "Personal", colorHex = "#6481D8", icon = "☀️"),
    Category(id = "wellness", name = "Wellness", colorHex = "#D69C51", icon = "🌿")
)
private val storeReminders = listOf(
    Reminder(id = "coffee", title = "A moment to reset", description = "Step outside and enjoy a little fresh air.", category = "Wellness", reminderTime = storeTime(10, 30)),
    Reminder(id = "project", title = "Review the project plan", description = "Bring the notes from yesterday.", category = "Work", reminderTime = storeTime(14)),
    Reminder(id = "family", title = "Call Mum", category = "Personal", reminderTime = storeTime(18)),
    Reminder(id = "walk", title = "Evening walk", category = "Wellness", reminderTime = storeTime(19), isRecurring = true, recurrenceIntervalMillis = 86_400_000),
    Reminder(id = "read", title = "Read a few pages", category = "Personal", reminderTime = storeTime(20, days = 1))
)

@Composable private fun StorePage(page: String) {
    val snackbar = remember { SnackbarHostState() }
    when (page) {
        "home" -> HomeScreenContent(HomeUiState(reminders = storeReminders, allReminders = storeReminders,
            categories = storeCategories, userName = "Alex"), storeTime(9), onNavigate = {}, onSearchChange = {},
            onCategoryChange = {}, onQuickFilterChange = {}, onRefresh = {}, onDelete = {}, onResetFilters = {})
        "add" -> AddReminderContent(ReminderDraft(title = "Evening walk", description = "A little fresh air after a busy day.",
            category = "Wellness", reminderTimeMillis = storeTime(19), isRecurring = true, recurrenceIntervalMillis = 86_400_000),
            storeCategories, emptySet(), 0, false, false, snackbar, onEvent = {})
        "calendar" -> CalendarScreenContent(buildCalendarState(storeDate, storeReminders, storeDate, storeZone, DayOfWeek.SUNDAY),
            onBack = {}, onSelectDate = {}, onChangeMonth = {}, onToday = {}, onRetry = {}, onReminderClick = {})
        "assistant" -> AiChatScreenContent(AiChatUiState(messages = listOf(
            ChatMessage(id = "example-request", text = "Remind me to call Mum tomorrow at 6 pm", sender = SenderType.USER),
            ChatMessage(id = "example-reply", text = "Here is your reminder draft. Review the details before saving.", sender = SenderType.AI),
            ChatMessage(id = "example-draft", sender = SenderType.AI,
                draft = AiReminderDraft("Call Mum", storeTime(18, days = 1), category = "Personal", id = "example"))
        )), onBack = {}, onInput = {}, onSend = {}, onRetry = {}, onClear = {}, onReview = {}, onManual = {})
        "templates" -> TemplatesScreenContent(TemplatesUiState(templates = listOf(
            ReminderTemplate(id = "daily", name = "Daily reset", title = "A moment to reset", description = "Make a little time for yourself.", category = "Wellness", isRecurring = true, recurrenceIntervalMillis = 86_400_000),
            ReminderTemplate(id = "weekly", name = "Weekly planning", title = "Plan the week ahead", category = "Work", isRecurring = true, recurrenceIntervalMillis = 604_800_000),
            ReminderTemplate(id = "read", name = "Reading time", title = "Read a few pages", category = "Personal")
        ), isLoading = false, isAuthenticated = true), snackbar, onBack = {}, onCreate = {}, onUse = {}, onEdit = {},
            onDelete = {}, onQuery = {}, onFilter = {}, onRetry = {}, onReset = {})
        "security" -> AccountSecurityScreenContent(AccountSecurityUiState(biometricEnabled = true,
            biometricAvailability = BiometricAvailability.AVAILABLE, mfa = MfaSettingsState(loading = false,
                account = MfaAccount("store-demo", "demo@example.com", true, listOf(MfaProvider.GOOGLE), listOf(MfaFactor("demo", "Authenticator"))))),
            snackbar, onBack = {}, onPassword = {}, onBiometric = {}, onDevices = {}, onMfaAdd = {}, onVerifyEmail = {})
    }
}
