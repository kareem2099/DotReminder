package com.FreeRave.remindme.model

import android.os.Build
import com.google.firebase.Timestamp

data class Session(
    val sessionId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val loginTime: Timestamp = Timestamp.now(),
    val lastActive: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sessionId" to sessionId,
        "deviceName" to deviceName,
        "deviceModel" to deviceModel,
        "loginTime" to loginTime,
        "lastActive" to lastActive
    )

    companion object {
        fun createForCurrentDevice(sessionId: String) = Session(
            sessionId = sessionId,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            deviceModel = Build.MODEL,
            loginTime = Timestamp.now(),
            lastActive = Timestamp.now()
        )
    }
}