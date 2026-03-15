package com.FreeRave.remindme.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import com.FreeRave.remindme.utils.CacheManager
import com.FreeRave.remindme.utils.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double?, Double?, Float) -> Unit,
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialRadius: Float = 200f
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var latitude by remember { mutableStateOf(initialLatitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initialLongitude?.toString() ?: "") }
    var radius by remember { mutableStateOf(initialRadius.toString()) }
    var address by remember { mutableStateOf("") }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var geocoderRequired by remember { mutableStateOf(false) }

    // Cached reverse geocoding for current address
    fun updateAddress(lat: Double?, lng: Double?) {
        if (lat != null && lng != null) {
            isLoadingAddress = true
            coroutineScope.launch {
                try {
                    val locationUtils = LocationUtils(context)
                    val addressResult = locationUtils.reverseGeocode(lat, lng)
                    address = addressResult ?: "Unable to get address"
                } catch (e: Exception) {
                    Log.e("LocationPicker", "Reverse geocoding failed", e)
                    address = "Unable to get address"
                } finally {
                    isLoadingAddress = false
                }
            }
        }
    }

    // Function to get current location
    suspend fun getCurrentLocation() {
        isLoadingLocation = true
        locationError = null
        try {
            val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()
                updateAddress(location.latitude, location.longitude)
            } else {
                locationError = "Location not available. Try enabling GPS."
            }
        } catch (e: Exception) {
            Log.e("LocationPicker", "Failed to get current location", e)
            locationError = "Failed to get location: ${e.localizedMessage ?: "Unknown error"}"
        } finally {
            isLoadingLocation = false
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                getCurrentLocation()
            }
        } else {
            locationError = "Location permission denied. Go to settings to enable it."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Location") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current location button with loading state
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            coroutineScope.launch {
                                getCurrentLocation()
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingLocation
                ) {
                    if (isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Getting Location...")
                    } else {
                        Text("Use Current Location")
                    }
                }

                // Show any location errors
                locationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Address input (if geocoder required or manually)
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Search Address") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingAddress
                )

                // Latitude/Longitude inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                }

                // Radius input
                TextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )

                if (isLoadingAddress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    val rad = radius.toFloatOrNull() ?: 200f
                    onLocationSelected(lat, lng, rad)
                    onDismiss()
                },
                enabled = latitude.isNotBlank() && longitude.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
