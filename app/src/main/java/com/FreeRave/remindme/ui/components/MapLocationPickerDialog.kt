package com.FreeRave.remindme.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.FreeRave.remindme.BuildConfig
import com.FreeRave.remindme.utils.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerDialog(
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
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var circle by remember { mutableStateOf<Circle?>(null) }

    // Update selected location and UI
    fun updateSelectedLocation(latLng: LatLng) {
        selectedLocation = latLng
        latitude = latLng.latitude.toString()
        longitude = latLng.longitude.toString()

        // Update marker
        marker?.remove()
        marker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location")
                .draggable(true)
        )

        // Update circle
        circle?.remove()
        val radiusValue = radius.toFloatOrNull() ?: 200f
        circle = googleMap?.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(radiusValue.toDouble())
                .strokeColor(0xFF4285F4.toInt()) // Blue stroke
                .fillColor(0x1F4285F4.toInt()) // Semi-transparent blue fill
                .strokeWidth(2f)
        )

        // Move camera to location
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    // Setup map with current location
    fun setupMapWithCurrentLocation() {
        coroutineScope.launch {
            try {
                val locationUtils = LocationUtils(context)
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        updateSelectedLocation(currentLatLng)
                    }
                }
            } catch (e: Exception) {
                Log.e("MapLocationPicker", "Failed to get current location", e)
            }
        }
    }

    // Initialize map
    LaunchedEffect(Unit) {
        if (BuildConfig.GOOGLE_MAPS_API_KEY.isBlank()) {
            Log.w("MapLocationPicker", "Google Maps API key not configured")
        }
    }

    // Permission launcher for location access
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupMapWithCurrentLocation()
        }
    }

    // Update radius circle
    fun updateRadiusCircle() {
        selectedLocation?.let { latLng ->
            circle?.remove()
            val radiusValue = radius.toFloatOrNull() ?: 200f
            circle = googleMap?.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(radiusValue.toDouble())
                    .strokeColor(0xFF4285F4.toInt())
                    .fillColor(0x1F4285F4.toInt())
                    .strokeWidth(2f)
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Location on Map") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Map View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                mapView = this
                                onCreate(null)
                                getMapAsync { map ->
                                    googleMap = map
                                    map.uiSettings.isZoomControlsEnabled = true
                                    map.uiSettings.isMyLocationButtonEnabled = true

                                    // Enable location if permission granted
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        map.isMyLocationEnabled = true
                                    }

                                    // Set initial location or default
                                    val initialLatLng = if (initialLatitude != null && initialLongitude != null) {
                                        LatLng(initialLatitude, initialLongitude)
                                    } else {
                                        LatLng(37.7749, -122.4194) // Default to San Francisco
                                    }

                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 12f))

                                    // Handle map clicks
                                    map.setOnMapClickListener { latLng ->
                                        updateSelectedLocation(latLng)
                                    }

                                    // Handle marker drag
                                    map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                                        override fun onMarkerDragStart(marker: Marker) {}
                                        override fun onMarkerDrag(marker: Marker) {}
                                        override fun onMarkerDragEnd(marker: Marker) {
                                            updateSelectedLocation(marker.position)
                                        }
                                    })
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Current location button overlay
                    FloatingActionButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                setupMapWithCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(48.dp)
                    ) {
                        Text("📍", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Instructions
                Text(
                    text = "Tap on the map to select a location. Drag the marker to adjust.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Coordinate inputs (for fine-tuning)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = latitude,
                        onValueChange = {
                            latitude = it
                            val lat = it.toDoubleOrNull()
                            val lng = longitude.toDoubleOrNull()
                            if (lat != null && lng != null) {
                                updateSelectedLocation(LatLng(lat, lng))
                            }
                        },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = longitude,
                        onValueChange = {
                            longitude = it
                            val lat = latitude.toDoubleOrNull()
                            val lng = it.toDoubleOrNull()
                            if (lat != null && lng != null) {
                                updateSelectedLocation(LatLng(lat, lng))
                            }
                        },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                }

                // Radius input with live update
                TextField(
                    value = radius,
                    onValueChange = {
                        radius = it
                        updateRadiusCircle()
                    },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )

                // Selected location info
                selectedLocation?.let { latLng ->
                    Text(
                        text = "Selected: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                Text("Save Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Cleanup map when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }
}