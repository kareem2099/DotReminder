package com.FreeRave.remindme.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AiReminderSaveResult(val draftId: String, val reminderId: String) : Parcelable
