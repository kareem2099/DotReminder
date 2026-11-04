package com.FreeRave.remindme.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/** A suggestion passed to the normal reminder form; it has not been saved. */
@Parcelize
internal data class AiReminderDraft(
    val title: String,
    val reminderTime: Long,
    val category: String = "Other",
    val isRecurring: Boolean = false,
    val recurrenceIntervalMillis: Long? = null,
    val recurrenceEndTime: Long? = null,
    val sourceText: String = "",
    val id: String = UUID.randomUUID().toString()
) : Parcelable
