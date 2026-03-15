package com.FreeRave.remindme.model

import com.google.firebase.firestore.Exclude

data class Category(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "", // e.g., "#FF42A5F5"
    val description: String = "",
    val icon: String = "📁", // Default emoji icon
    val isPredefined: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "colorHex" to colorHex,
        "description" to description,
        "icon" to icon,
        "isPredefined" to isPredefined,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>, id: String = ""): Category = Category(
            id = id,
            userId = map["userId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            colorHex = map["colorHex"] as? String ?: "",
            description = map["description"] as? String ?: "",
            icon = map["icon"] as? String ?: "📁",
            isPredefined = map["isPredefined"] as? Boolean ?: false,
            createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
            updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
