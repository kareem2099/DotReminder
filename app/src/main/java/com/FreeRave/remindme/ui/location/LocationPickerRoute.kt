package com.FreeRave.remindme.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.FreeRave.remindme.BuildConfig
import com.FreeRave.remindme.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume

@Composable
internal fun LocationPickerRoute(onDismiss: () -> Unit, onSelected: (Double?, Double?, Float) -> Unit,
    latitude: Double?, longitude: Double?, radius: Float, useMap: Boolean) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf(LocationDraft(latitude?.toString().orEmpty(), longitude?.toString().orEmpty(), radius.toString())) }
    var working by remember { mutableStateOf<LocationWork?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun locate() {
        if (working != null) return
        working = LocationWork.CURRENT; error = null
        scope.launch {
            val token = CancellationTokenSource()
            try {
                val result = withTimeout(15_000) {
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
                }
                if (result == null) error = context.getString(R.string.polish_location_unavailable)
                else draft = draft.copy(latitude = result.latitude.toString(), longitude = result.longitude.toString())
            } catch (_: TimeoutCancellationException) { error = context.getString(R.string.polish_location_unavailable) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = context.getString(R.string.polish_location_unavailable) }
            finally { token.cancel(); working = null }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) locate() else error = context.getString(R.string.polish_location_permission)
    }
    val current = {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) locate() else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    val mapContent: (@Composable () -> Unit)? = if (!useMap) null else ({
        if (BuildConfig.GOOGLE_MAPS_API_KEY.isBlank()) Text(stringResource(R.string.polish_map_unavailable))
        else LocationMap(draft) { lat, lng -> if (working == null) { draft = draft.copy(latitude = lat.toString(), longitude = lng.toString()); error = null } }
    })
    LocationEditorDialog(draft, onChange = { if (working == null) { draft = it; error = null } },
        onSelected = { lat, lng, distance -> onSelected(lat, lng, distance); onDismiss() }, onDismiss, onCurrent = current,
        onSearch = {
            if (working == null && draft.address.isNotBlank()) {
                working = LocationWork.ADDRESS; error = null
                scope.launch {
                    try {
                        val found = withTimeout(15_000) { findAddress(context, draft.address) }
                        if (found == null) error = context.getString(R.string.polish_address_unavailable)
                        else draft = draft.copy(latitude = found.latitude.toString(), longitude = found.longitude.toString())
                    } catch (_: TimeoutCancellationException) { error = context.getString(R.string.polish_address_unavailable) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { error = context.getString(R.string.polish_address_unavailable) }
                    finally { working = null }
                }
            }
        }, working, error, mapContent)
}

private suspend fun findAddress(context: Context, query: String): Address? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    return if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) { if (continuation.isActive) continuation.resume(addresses.firstOrNull()) }
            override fun onError(errorMessage: String?) { if (continuation.isActive) continuation.resume(null) }
        })
    } else withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(query, 1)?.firstOrNull()
    }
}
