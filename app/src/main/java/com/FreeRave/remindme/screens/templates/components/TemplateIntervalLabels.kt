package com.FreeRave.remindme.screens.templates.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.templates.TemplateIntervalUnit
import com.FreeRave.remindme.screens.templates.templateInterval

internal val TemplateIntervalUnit.label: Int get() = when (this) {
    TemplateIntervalUnit.SECONDS -> R.string.templates_seconds
    TemplateIntervalUnit.MINUTES -> R.string.templates_minutes
    TemplateIntervalUnit.HOURS -> R.string.templates_hours
    TemplateIntervalUnit.DAYS -> R.string.templates_days
    TemplateIntervalUnit.WEEKS -> R.string.templates_weeks
}

@Composable
internal fun templateIntervalLabel(millis: Long?): String {
    if (millis == null || millis <= 0) return stringResource(R.string.templates_interval_missing)
    val interval = templateInterval(millis)
    val plural = when (interval.unit) {
        TemplateIntervalUnit.SECONDS -> R.plurals.templates_unit_seconds
        TemplateIntervalUnit.MINUTES -> R.plurals.templates_unit_minutes
        TemplateIntervalUnit.HOURS -> R.plurals.templates_unit_hours
        TemplateIntervalUnit.DAYS -> R.plurals.templates_unit_days
        TemplateIntervalUnit.WEEKS -> R.plurals.templates_unit_weeks
    }
    return stringResource(R.string.templates_interval_summary, interval.amount,
        pluralStringResource(plural, if (interval.amount == "1") 1 else 2))
}
