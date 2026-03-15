package com.FreeRave.remindme.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude

@Keep
data class Reminder(
    val id: String = "",         // Firestore doc ID
    val customId: String = "",   // User-defined readable ID
    val title: String = "",
    val description: String = "",
    val time: Long = System.currentTimeMillis(), // Creation/update time
    val reminderTime: Long = System.currentTimeMillis(), // Time for the (first) notification
    val userId: String = "",
    val category: String = "",   // Category for organization
    // Fields for recurrence
    val isRecurring: Boolean = false,
    val recurrenceIntervalMillis: Long? = null, // e.g., 5 * 60 * 1000 for 5 minutes
    val recurrenceEndTime: Long? = null, // Optional: timestamp when recurrence stops
    // Fields for location-based reminders
    val isLocationBased: Boolean = false,
    val latitude: Double? = null, // Geographic latitude for geofence center
    val longitude: Double? = null, // Geographic longitude for geofence center
    val geofenceRadius: Float = 200f, // Radius in meters
    val geofenceTransitionType: Int = 0, // Bitmask: 1=ENTER, 2=EXIT, 3=BOTH
    val geofenceDwellTimeMillis: Long? = null, // Time to dwell before triggering (null = immediate)
    val geofencePriority: Int = 1 // Priority for geofence management (1=low, 5=high)
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", "", "", 0L, 0L, "", "", false, null, null, false, null, null, 200f, 0, null, 1)

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "customId" to customId,
        "title" to title,
        "description" to description,
        "time" to time,
        "reminderTime" to reminderTime,
        "userId" to userId,
        "category" to category,
        "isRecurring" to isRecurring,
        "recurrenceIntervalMillis" to recurrenceIntervalMillis,
        "recurrenceEndTime" to recurrenceEndTime,
        "isLocationBased" to isLocationBased,
        "latitude" to latitude,
        "longitude" to longitude,
        "geofenceRadius" to geofenceRadius,
        "geofenceTransitionType" to geofenceTransitionType,
        "geofenceDwellTimeMillis" to geofenceDwellTimeMillis,
        "geofencePriority" to geofencePriority
    )

    companion object {
        fun fromMap(map: Map<String, Any>, id: String = ""): Reminder = Reminder(
            id = id,
            customId = map["customId"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            time = map["time"] as? Long ?: 0L,
            reminderTime = map["reminderTime"] as? Long ?: 0L,
            userId = map["userId"] as? String ?: "",
            category = map["category"] as? String ?: "",
            isRecurring = map["isRecurring"] as? Boolean ?: false,
            recurrenceIntervalMillis = map["recurrenceIntervalMillis"] as? Long,
            recurrenceEndTime = map["recurrenceEndTime"] as? Long,
            isLocationBased = map["isLocationBased"] as? Boolean ?: false,
            latitude = map["latitude"] as? Double,
            longitude = map["longitude"] as? Double,
            geofenceRadius = map["geofenceRadius"] as? Float ?: 200f,
            geofenceTransitionType = map["geofenceTransitionType"] as? Int ?: 0,
            geofenceDwellTimeMillis = map["geofenceDwellTimeMillis"] as? Long,
            geofencePriority = map["geofencePriority"] as? Int ?: 1
        )
    }
}
