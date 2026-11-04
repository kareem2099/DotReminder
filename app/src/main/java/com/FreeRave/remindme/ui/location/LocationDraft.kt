package com.FreeRave.remindme.ui.location

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LocationDraft(val latitude: String = "", val longitude: String = "", val radius: String = "200", val address: String = "") : Parcelable {
    val parsedLatitude get() = locationNumber(latitude)?.takeIf { it in -90.0..90.0 }
    val parsedLongitude get() = locationNumber(longitude)?.takeIf { it in -180.0..180.0 }
    val parsedRadius get() = locationNumber(radius)?.toFloat()?.takeIf { it.isFinite() && it > 0 }
    val valid get() = parsedLatitude != null && parsedLongitude != null && parsedRadius != null
}
internal fun locationNumber(value: String): Double? = buildString {
    value.trim().forEach { character ->
        val digit = Character.digit(character, 10)
        append(when { digit >= 0 -> ('0'.code + digit).toChar(); character == ',' || character == '٫' -> '.'; character == '−' -> '-'; else -> character })
    }
}.toDoubleOrNull()?.takeIf { it.isFinite() }
