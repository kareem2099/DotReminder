package com.FreeRave.remindme.screens.ai

import com.FreeRave.remindme.model.AiReminderDraft
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigDecimal

private const val REMINDER_COMMAND = "ACTION_PARSE_REMINDER:"
internal fun reminderCommand(response: String): String? = response.trim().takeIf { it.startsWith(REMINDER_COMMAND) }
    ?.removePrefix(REMINDER_COMMAND)?.trim()?.takeIf { it.isNotBlank() }

internal fun conversationForAi(messages: List<ChatMessage>): String {
    val turns = messages.takeLast(12).map { message ->
        val content = if (message.savedReminderId != null) "The user reviewed and saved this draft in the app. Current saved details are not provided."
            else message.draft?.let { "Proposed reminder draft, not saved: ${it.title}; time=${it.reminderTime}; recurring=${it.isRecurring}" }
            ?: message.text
        mapOf("role" to if (message.sender == SenderType.USER) "user" else "assistant", "content" to content.take(AI_MESSAGE_LIMIT))
    }
    // Keep whole recent turns rather than cutting through a JSON string.
    var bounded = turns
    val gson = Gson()
    while (bounded.size > 1 && gson.toJson(bounded).length > 24_000) bounded = bounded.drop(1)
    return gson.toJson(bounded)
}

internal fun parseAiReminder(json: String?, command: String, now: Long): AiReminderDraft? = try {
    if (json.isNullOrBlank() || json.length > 32_000) null
    else {
        val clean = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val objectValue = JsonParser.parseString(clean).asJsonObject
        val title = objectValue.string("title")?.trim().orEmpty()
        val time = objectValue.exactLong("reminderTime")
        if (title.isBlank() || title.length > 500 || time == null || time <= now) null
        else {
            val recurringValue = objectValue.get("isRecurring")
            require(recurringValue == null || recurringValue.isJsonNull ||
                recurringValue.isJsonPrimitive && recurringValue.asJsonPrimitive.isBoolean)
            val recurring = recurringValue?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean ?: false
            val interval = if (recurring) objectValue.exactLong("recurrenceIntervalMillis")?.takeIf { it > 0 } else null
            val endValue = objectValue.get("recurrenceEndTime")
            val end = if (recurring && endValue != null && !endValue.isJsonNull) {
                objectValue.exactLong("recurrenceEndTime").also { require(it != null && it > time) }
            } else null
            val category = objectValue.string("category")?.trim()?.takeIf { it in setOf("Work", "Personal", "Health", "Other") } ?: "Other"
            AiReminderDraft(title, time, category, recurring, interval, end, command)
        }
    }
} catch (_: Exception) { null }

private fun JsonObject.string(key: String) = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
private fun JsonObject.exactLong(key: String): Long? = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
    ?.let { try { BigDecimal(it.asString).longValueExact() } catch (_: ArithmeticException) { null } }
