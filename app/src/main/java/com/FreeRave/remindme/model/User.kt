package com.FreeRave.remindme.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude

@Keep
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val isEmailVerified: Boolean = false,
    val photoUrl: String? = null,
    val provider: AuthProvider = AuthProvider.EMAIL
) {
    // Required empty constructor for Firebase
    constructor() : this("", "", "", null, false, null, AuthProvider.EMAIL)

    @Exclude
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id
        map["name"] = name
        map["email"] = email
        map["phone"] = phone ?: ""
        map["isEmailVerified"] = isEmailVerified
        photoUrl?.let { map["photoUrl"] = it }
        map["provider"] = provider.name
        return map
    }

    companion object {
        fun fromMap(map: Map<String, Any>): User = User(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            email = map["email"] as? String ?: "",
            phone = map["phone"] as? String,
            isEmailVerified = map["isEmailVerified"] as? Boolean ?: false,
            photoUrl = map["photoUrl"] as? String,
            provider = AuthProvider.valueOf(map["provider"] as? String ?: "EMAIL")
        )
    }
}

enum class AuthProvider {
    EMAIL,
    GOOGLE,
    FACEBOOK
}
