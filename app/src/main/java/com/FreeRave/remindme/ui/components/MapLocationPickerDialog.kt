package com.FreeRave.remindme.ui.components

import androidx.compose.runtime.Composable
import com.FreeRave.remindme.ui.location.LocationPickerRoute

@Composable
fun MapLocationPickerDialog(onDismiss: () -> Unit, onLocationSelected: (Double?, Double?, Float) -> Unit,
    initialLatitude: Double? = null, initialLongitude: Double? = null, initialRadius: Float = 200f) =
    LocationPickerRoute(onDismiss, onLocationSelected, initialLatitude, initialLongitude, initialRadius, useMap = true)
