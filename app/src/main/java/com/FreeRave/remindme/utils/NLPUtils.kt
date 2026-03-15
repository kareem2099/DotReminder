package com.FreeRave.remindme.utils

import com.joestelmach.natty.Parser
import java.util.regex.Pattern

data class ParsedReminderInfo(
    val title: String,
    val reminderTime: Long?, // Start time of the first occurrence
    val matchedDateText: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceIntervalMillis: Long? = null,
    val recurrenceEndTime: Long? = null
)

object NLPUtils {

    // private val nattyParser = Parser() // Remove from object scope

    // Regex for "in X minutes" or "after X minutes"
    private val inXMinutesPattern = Pattern.compile(
        "\\b(?:in|after)\\s+(\\d+)\\s+minute(s)?\\b",
        Pattern.CASE_INSENSITIVE
    )
    // Regex for "in X hours" or "after X hours"
    private val inXHoursPattern = Pattern.compile(
        "\\b(?:in|after)\\s+(\\d+)\\s+hour(s)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Regex to find "for X minutes/hours" or "every X minutes/hours for Y minutes/hours"
    // This is a simplified example and might need to be more robust
    private val durationPattern = Pattern.compile(
        "\\b(?:for|every)\\s+(\\d+)\\s+(minute|minutes|hour|hours)(?:\\s+for\\s+(\\d+)\\s+(minute|minutes|hour|hours))?",
        Pattern.CASE_INSENSITIVE
    )
    private val everyXMinutesPattern = Pattern.compile(
        "\\bevery\\s+(\\d+)\\s+(minute|minutes)",
        Pattern.CASE_INSENSITIVE
    )
    private val everyXHoursPattern = Pattern.compile(
        "\\bevery\\s+(\\d+)\\s+(hour|hours)",
        Pattern.CASE_INSENSITIVE
    )


    fun parseReminderText(inputText: String): ParsedReminderInfo {
        val nattyParser = Parser() // Initialize Natty parser for each call to ensure statelessness
        var mutableInputText = inputText.trim()
        var firstReminderTime: Long? = null
        var preProcessedDateString: String? = null // To store the string matched by our regex

        // Priority 1: Check for "in X minutes/hours" or "after X minutes/hours"
        val inMinutesMatcher = inXMinutesPattern.matcher(mutableInputText)
        if (inMinutesMatcher.find()) {
            val minutes = inMinutesMatcher.group(1)?.toLongOrNull()
            if (minutes != null) {
                firstReminderTime = System.currentTimeMillis() + minutes * 60 * 1000
                preProcessedDateString = inMinutesMatcher.group(0)
                mutableInputText = mutableInputText.replace(preProcessedDateString!!, "", ignoreCase = true).trim()
            }
        }

        if (firstReminderTime == null) { // Only if "in X minutes" wasn't found
            val inHoursMatcher = inXHoursPattern.matcher(mutableInputText)
            if (inHoursMatcher.find()) {
                val hours = inHoursMatcher.group(1)?.toLongOrNull()
                if (hours != null) {
                    firstReminderTime = System.currentTimeMillis() + hours * 60 * 60 * 1000
                    preProcessedDateString = inHoursMatcher.group(0)
                    mutableInputText = mutableInputText.replace(preProcessedDateString!!, "", ignoreCase = true).trim()
                }
            }
        }

        // Now, let Natty parse the (potentially modified) input text
        val dateGroups = nattyParser.parse(if (preProcessedDateString != null) mutableInputText else inputText.trim())
        
        var titleText = mutableInputText // Start with the text possibly cleaned by our regexes
        var matchedDateString: String? = null // This will be from Natty on the remaining string
        var isRec = false
        var intervalMillis: Long? = null
        var recEndTime: Long? = null

        if (!dateGroups.isNullOrEmpty()) {
            val firstGroup = dateGroups.first()
            if (!firstGroup.dates.isNullOrEmpty()) {
                // If our pre-processing didn't set a time, use Natty's.
                // If our pre-processing DID set a time, Natty is now parsing for recurrence or other date info.
                if (firstReminderTime == null) {
                    firstReminderTime = firstGroup.dates.first().time
                }
                // Natty's matched text on the (possibly modified) input
                matchedDateString = firstGroup.text 
                // Remove Natty's matched text from the current titleText
                titleText = titleText.replace(matchedDateString, "", ignoreCase = true).trim()

                // Use Natty's recurrence info, it's usually good
                isRec = firstGroup.isRecurring

                if (isRec && firstGroup.dates.size >= 2) {
                    // Natty often provides at least two dates for recurring events.
                    // The interval can be derived from them.
                    intervalMillis = firstGroup.dates[1].time - firstGroup.dates[0].time
                }

                if (firstGroup.recursUntil != null) {
                    recEndTime = firstGroup.recursUntil.time
                }
            }
        }
        
        // Custom handling for "every X minutes/hours" if Natty doesn't set interval well or for "for Y duration"
        // This is a basic attempt and can be improved.
        
        // Try to parse "every X minutes" from the potentially modified mutableInputText
        // This custom recurrence parsing should operate on what's left of the string
        val currentTextForRecurrence = titleText 

        val minutesMatcher = everyXMinutesPattern.matcher(currentTextForRecurrence) // Use currentTextForRecurrence
        if (minutesMatcher.find()) {
            val value = minutesMatcher.group(1)?.toLongOrNull()
            if (value != null) {
                isRec = true 
                intervalMillis = value * 60 * 1000
                if (firstReminderTime == null) { // If no time at all was found yet
                    firstReminderTime = System.currentTimeMillis() + intervalMillis // Start with the first interval from now
                }
                // Remove the matched "every X minutes" part from title
                titleText = titleText.replace(minutesMatcher.group(0) ?: "", "", ignoreCase = true).trim()
            }
        }

        // Try to parse "every X hours" - similar logic to minutes
        val hoursMatcher = everyXHoursPattern.matcher(currentTextForRecurrence) // Use currentTextForRecurrence
        if (hoursMatcher.find()) {
            val value = hoursMatcher.group(1)?.toLongOrNull()
            if (value != null) {
                isRec = true
                intervalMillis = value * 60 * 60 * 1000
                if (firstReminderTime == null) { // If no time at all was found yet
                     firstReminderTime = System.currentTimeMillis() + intervalMillis
                }
                // Remove the matched "every X hours" part from title
                titleText = titleText.replace(hoursMatcher.group(0) ?: "", "", ignoreCase = true).trim()
            }
        }

        // Attempt to parse "for X duration" to set recurrenceEndTime from the remaining titleText
        val forDurationPattern = Pattern.compile("\\bfor\\s+(\\d+)\\s+(minute|minutes|hour|hours)\\b", Pattern.CASE_INSENSITIVE)
        val durationMatcherForRec = forDurationPattern.matcher(titleText) // Use titleText
        if (durationMatcherForRec.find() && firstReminderTime != null && isRec && intervalMillis != null) {
            val valueString = durationMatcherForRec.group(1)
            val unitString = durationMatcherForRec.group(2)
            if (valueString != null && unitString != null) {
                val value = valueString.toLong()
                val unit = unitString.lowercase()
                var durationMillis = 0L
                when (unit) {
                    "minute", "minutes" -> durationMillis = value * 60 * 1000
                    "hour", "hours" -> durationMillis = value * 60 * 60 * 1000
                }
                if (durationMillis > 0) {
                    recEndTime = firstReminderTime + durationMillis // This assumes duration is from the *start* of the first reminder
                    // Remove the "for X duration" part from title
                    titleText = titleText.replace(durationMatcherForRec.group(0) ?: "", "", ignoreCase = true).trim()
                }
            }
        }
        
        // Final cleanup of title, now that all date/time/recurrence phrases should have been handled
            titleText = titleText.trim() // General trim
            .replace("\\s+".toRegex(), " ") // Consolidate multiple spaces

        // If titleText became empty after stripping all date/time info, use a default or original.
        // If preProcessedDateString is not null, it means our initial regex matched.
        // matchedDateString is from Natty on the remainder.
        // The goal is to have a clean title.
        if (titleText.isEmpty() && (firstReminderTime != null || isRec)) {
             // If original input was ONLY date/time info, titleText would be empty.
            if (preProcessedDateString != null && inputText.trim().equals(preProcessedDateString, ignoreCase = true)) {
                titleText = "Reminder" // e.g., input was "in 5 minutes"
            } else if (matchedDateString != null && mutableInputText.trim().equals(matchedDateString, ignoreCase = true)) {
                 titleText = "Reminder" // e.g., input (after our regex) was "tomorrow"
            } else if (inputText.isNotBlank()) {
                // Fallback to a cleaned version of original input if title is empty but input wasn't purely date stuff
                // This part is tricky; for now, let's stick to "Reminder" if it's empty.
                titleText = "Reminder"
            }
        }
        
        // If it's recurring (either by Natty or our regexes) but no specific interval was parsed by custom logic,
        // and Natty gave one, ensure it's positive.
        if (isRec && intervalMillis != null && intervalMillis <= 0) {
            // This might happen if Natty's dates are too close or parsing is odd.
            // Default to a sensible minimum or mark as not recurring if interval is invalid.
            // For now, let's nullify if non-positive.
            intervalMillis = null 
            isRec = false // If interval is invalid, treat as non-recurring.
        }
        
        // If not recurring, interval and end time should be null
        if (!isRec) {
            intervalMillis = null
            recEndTime = null
        }


        // Use preProcessedDateString if our initial regexes matched, otherwise use Natty's matchedDateString
        val finalMatchedDateText = preProcessedDateString ?: matchedDateString

        return ParsedReminderInfo(
            title = titleText.ifEmpty { mutableInputText.ifEmpty {inputText.trim()} }, // Ensure title is not blank
            reminderTime = firstReminderTime,
            matchedDateText = finalMatchedDateText,
            isRecurring = isRec,
            recurrenceIntervalMillis = intervalMillis,
            recurrenceEndTime = recEndTime
        )
    }
}
