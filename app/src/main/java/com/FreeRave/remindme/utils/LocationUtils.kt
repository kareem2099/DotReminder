package com.FreeRave.remindme.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "LocationUtils"

@Composable
fun RequestLocationAndUpdateCountryCode(
    context: Context = LocalContext.current,
    snackbarHostState: SnackbarHostState,
    onCountryCodeDetected: (String?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            // Try SIM card country first (doesn't need location permission)
            val simCountry = getSimCountryCode(context)
            if (simCountry != null) {
                onCountryCodeDetected(simCountry)
                return@LaunchedEffect
            }

            // Fall back to location-based detection
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Allow location access to auto-detect your country code"
                    )
                }
                onCountryCodeDetected(null)
                return@LaunchedEffect
            }

            LocationUtils(context).getCountryCodeInternal { countryCode ->
                coroutineScope.launch {
                    if (countryCode == null) {
                        snackbarHostState.showSnackbar(
                            "Could not detect country. Please select manually"
                        )
                    }
                    onCountryCodeDetected(countryCode)
                }
            }
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Error detecting country: ${e.message}"
                )
                onCountryCodeDetected(null)
            }
        }
    }
}

private fun getSimCountryCode(context: Context): String? {
    return try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountry = tm.simCountryIso
        if (!simCountry.isNullOrEmpty()) {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numericCode = phoneUtil.getCountryCodeForRegion(simCountry.uppercase())
            if (numericCode != 0) "+$numericCode" else null
        } else null
    } catch (e: Exception) {
        Log.e(TAG, "Error getting SIM country", e)
        null
    }
}

class LocationUtils(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val cacheManager = CacheManager(context)

    companion object {
        @JvmStatic
        fun getCountryCode(context: Context, callback: (String?) -> Unit) {
            LocationUtils(context).getCountryCodeInternal(callback)
        }

        /**
         * Check if GPS/location services are enabled
         */
        @JvmStatic
        fun isGpsEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        /**
         * Prompt user to enable GPS/location services
         */
        @JvmStatic
        fun openLocationSettings(context: Context) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            if (context is Activity) {
                context.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    internal fun getCountryCodeInternal(callback: (code: String?) -> Unit) {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val geocoder = Geocoder(context, Locale.getDefault())

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                if (location != null) {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )
                        if (addresses?.isNotEmpty() == true) {
                            val countryIso = addresses[0].countryCode
                            if (!countryIso.isNullOrEmpty()) {
                                val numericCode = phoneUtil.getCountryCodeForRegion(countryIso)
                                if (numericCode != 0) {
                                    callback("+$numericCode")
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting country code", e)
                    }
                }
                callback(null)
            }.addOnFailureListener { e: Exception ->
                Log.e(TAG, "Failed to get location", e)
                callback(null)
            }
        } else {
            callback(null)
        }
    }

    // Cached geocoding methods
    suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        // Check cache first
        val cacheKey = "address:$address"
        val cached = cacheManager.getGeocodingResult(cacheKey)
        if (cached != null) {
            return Pair(cached.latitude, cached.longitude)
        }

        // Perform geocoding
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses?.isNotEmpty() == true) {
                val location = addresses[0]
                val latLng = Pair(location.latitude, location.longitude)

                // Cache the result
                cacheManager.putGeocodingResult(cacheKey, latLng.first, latLng.second, address)

                latLng
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed for address: $address", e)
            null
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        // Check cache first
        val cacheKey = "reverse:$latitude,$longitude"
        val cached = cacheManager.getGeocodingResult(cacheKey)
        if (cached != null) {
            return cached.address
        }

        // Perform reverse geocoding
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val addressString = buildString {
                    (0..2).forEach { index ->
                        if (!address.getAddressLine(index).isNullOrEmpty()) {
                            append(address.getAddressLine(index))
                            if (index < 2 && !address.getAddressLine(index + 1).isNullOrEmpty()) {
                                append(", ")
                            }
                        }
                    }
                }

                // Cache the result
                cacheManager.putGeocodingResult(cacheKey, latitude, longitude, addressString)

                addressString
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed for: $latitude,$longitude", e)
            null
        }
    }

    // Location history methods
    suspend fun getLocationHistory(): List<LocationHistoryEntry> {
        return cacheManager.getLocationHistory()
    }

    suspend fun addLocationToHistory(latitude: Double, longitude: Double, address: String? = null, name: String? = null) {
        cacheManager.addLocationToHistory(latitude, longitude, address, name)
    }

    // Cache management
    suspend fun getCacheStats(): CacheStats {
        return cacheManager.getCacheStats()
    }

    suspend fun clearLocationCache() {
        cacheManager.clearAllCaches()
    }
}

@Composable
fun GpsEnableDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Enable Location Services",
    message: String = "GPS is required for location-based reminders. Would you like to enable it?"
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("Enable GPS")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
