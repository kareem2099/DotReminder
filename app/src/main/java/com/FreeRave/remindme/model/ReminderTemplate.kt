package com.FreeRave.remindme.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class ReminderTemplate(
    val id: String = "",
    val userId: String = "",
    val name: String = "", // Template name (e.g., "Meeting", "Medication")
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val isRecurring: Boolean = false,
    val recurrenceIntervalMillis: Long? = null,
    val isLocationBased: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable { // Implement Parcelable here
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", "", "", "", "", false, null, false, 0L, 0L)

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "title" to title,
        "description" to description,
        "category" to category,
        "isRecurring" to isRecurring,
        "recurrenceIntervalMillis" to recurrenceIntervalMillis,
        "isLocationBased" to isLocationBased,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>, id: String = ""): ReminderTemplate = ReminderTemplate(
            id = id,
            userId = map["userId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            category = map["category"] as? String ?: "",
            isRecurring = map["isRecurring"] as? Boolean ?: false,
            recurrenceIntervalMillis = map["recurrenceIntervalMillis"] as? Long,
            isLocationBased = map["isLocationBased"] as? Boolean ?: false,
            createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
            updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
