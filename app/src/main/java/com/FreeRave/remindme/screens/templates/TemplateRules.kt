package com.FreeRave.remindme.screens.templates

import com.FreeRave.remindme.model.ReminderTemplate
import java.math.BigDecimal
import java.util.Locale

internal enum class TemplateIntervalUnit(val millis: Long) {
    SECONDS(1_000), MINUTES(60_000), HOURS(3_600_000), DAYS(86_400_000), WEEKS(604_800_000)
}

internal data class TemplateInterval(val amount: String = "1", val unit: TemplateIntervalUnit = TemplateIntervalUnit.DAYS) {
    fun toMillis(): Long? {
        val input = amount.trim()
        if (!Regex("(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)").matches(input)) return null
        return try {
            BigDecimal(input.replace(',', '.')).multiply(BigDecimal.valueOf(unit.millis)).longValueExact().takeIf { it > 0 }
        } catch (_: ArithmeticException) { null }
        catch (_: NumberFormatException) { null }
    }
}

/** Decimal seconds preserve legacy intervals exactly, including values below a minute. */
internal fun templateInterval(millis: Long?): TemplateInterval {
    if (millis == null || millis <= 0) return TemplateInterval(amount = "")
    val unit = TemplateIntervalUnit.entries.reversed().firstOrNull { millis % it.millis == 0L } ?: TemplateIntervalUnit.SECONDS
    val amount = BigDecimal.valueOf(millis).divide(BigDecimal.valueOf(unit.millis)).stripTrailingZeros().toPlainString()
    return TemplateInterval(amount, unit)
}

internal data class TemplateDraft(
    val name: String = "", val title: String = "", val description: String = "", val category: String = "",
    val isRecurring: Boolean = false, val interval: TemplateInterval = TemplateInterval(), val isLocationBased: Boolean = false
)
internal enum class TemplateFieldError { NAME, TITLE, INTERVAL }

internal fun TemplateDraft.validate(): Set<TemplateFieldError> = buildSet {
    if (name.isBlank()) add(TemplateFieldError.NAME)
    if (title.isBlank()) add(TemplateFieldError.TITLE)
    if (isRecurring && interval.toMillis() == null) add(TemplateFieldError.INTERVAL)
}

internal fun ReminderTemplate.toDraft() = TemplateDraft(name, title, description, category, isRecurring,
    if (isRecurring) templateInterval(recurrenceIntervalMillis) else TemplateInterval(), isLocationBased)

internal fun TemplateDraft.toTemplate(original: ReminderTemplate?, userId: String, now: Long): ReminderTemplate =
    (original ?: ReminderTemplate(userId = userId, createdAt = now)).copy(
        name = name.trim(), title = title.trim(), description = description.trim(), category = category.trim(),
        isRecurring = isRecurring, recurrenceIntervalMillis = if (isRecurring) interval.toMillis() else null,
        isLocationBased = isLocationBased, updatedAt = now)

internal fun TemplateDraft.hasChanges(original: ReminderTemplate?): Boolean = original == null ||
    toTemplate(original, original.userId, original.updatedAt) != original

internal fun orderTemplates(templates: List<ReminderTemplate>) = templates.filter { it.id.isNotBlank() }.distinctBy { it.id }
    .sortedWith(compareBy<ReminderTemplate> { it.name.lowercase(Locale.ROOT) }.thenBy { it.id })
