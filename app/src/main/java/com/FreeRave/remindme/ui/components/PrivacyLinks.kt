package com.FreeRave.remindme.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import com.FreeRave.remindme.R

/** Public URLs contain no account identifiers, email addresses or authentication tokens. */
internal object PrivacyLinks {
    private fun supportedLanguage(language: String): String =
        language.takeIf { it in setOf("en", "ar", "de", "fr", "ru") } ?: "en"

    fun url(language: String, deletion: Boolean = false): String =
        "https://dotsuite.dev/${supportedLanguage(language)}/privacy" +
            if (deletion) "#delete-dotreminder" else "#dotreminder"

    fun open(context: Context, language: String, deletion: Boolean = false) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url(language, deletion))))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.privacy_browser_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    fun contactSupport(context: Context, language: String) {
        val email = context.getString(R.string.privacy_support_email)
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO,
                Uri.parse("mailto:$email?subject=${Uri.encode("DotReminder support")}")))
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://dotsuite.dev/${supportedLanguage(language)}/contact")))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.privacy_browser_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
internal fun PrivacyPolicyLink() {
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    TextButton(onClick = { PrivacyLinks.open(context, language) }) {
        Text(stringResource(R.string.privacy_policy))
    }
}
