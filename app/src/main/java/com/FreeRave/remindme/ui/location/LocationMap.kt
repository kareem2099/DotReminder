package com.FreeRave.remindme.ui.location

import android.os.Bundle
import android.content.Context
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*

@Composable
internal fun LocationMap(draft: LocationDraft, onPoint: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val bundle = rememberSaveable { Bundle() }
    val view = remember { MapView(context).apply { onCreate(bundle) } }
    val binding = remember(view) { LocationMapLifecycle(view, bundle) }
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    val onPointLatest by rememberUpdatedState(onPoint)
    val overlays = remember { LocationMapOverlays() }
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    DisposableEffect(view, lifecycle) {
        lifecycle.addObserver(binding)
        val memory = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() { if (!binding.destroyed) view.onLowMemory() }
        }
        context.applicationContext.registerComponentCallbacks(memory)
        view.getMapAsync { ready ->
            if (!binding.destroyed) {
                ready.uiSettings.isMyLocationButtonEnabled = false
                ready.uiSettings.isZoomControlsEnabled = true
                ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1f))
                ready.setOnMapClickListener { onPointLatest(it.latitude, it.longitude) }
                map = ready
            }
        }
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(memory)
            lifecycle.removeObserver(binding); binding.destroy()
        }
    }
    LaunchedEffect(map, draft.parsedLatitude, draft.parsedLongitude, draft.parsedRadius, accent) {
        val activeMap = map ?: return@LaunchedEffect
        val lat = draft.parsedLatitude; val lng = draft.parsedLongitude
        overlays.marker?.remove(); overlays.circle?.remove()
        if (lat != null && lng != null) {
            val point = LatLng(lat, lng)
            overlays.marker = activeMap.addMarker(MarkerOptions().position(point))
            draft.parsedRadius?.let { radius ->
                overlays.circle = activeMap.addCircle(CircleOptions().center(point).radius(radius.toDouble())
                    .strokeColor(accent).fillColor((accent and 0x00FFFFFF) or 0x24000000).strokeWidth(2f))
            }
            if (overlays.center != point) activeMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15f))
            overlays.center = point
        }
    }
    Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        AndroidView(factory = { MapGestureFrame(it).apply { addView(view, FrameLayout.LayoutParams(-1, -1)) } }, modifier = Modifier.fillMaxSize())
        if (map == null) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

private class MapGestureFrame(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled
    }
}

private class LocationMapOverlays {
    var marker: Marker? = null
    var circle: Circle? = null
    var center: LatLng? = null
}
private class LocationMapLifecycle(private val view: MapView, private val bundle: Bundle) : DefaultLifecycleObserver {
    var destroyed = false; private set
    private var started = false
    private var resumed = false
    override fun onStart(owner: LifecycleOwner) { if (!destroyed && !started) { view.onStart(); started = true } }
    override fun onResume(owner: LifecycleOwner) { if (!destroyed && !resumed) { view.onResume(); resumed = true } }
    override fun onPause(owner: LifecycleOwner) { pause() }
    override fun onStop(owner: LifecycleOwner) { stop() }
    override fun onDestroy(owner: LifecycleOwner) { destroy() }
    private fun pause() { if (!destroyed && resumed) { view.onPause(); resumed = false } }
    private fun stop() { pause(); if (!destroyed && started) { view.onSaveInstanceState(bundle); view.onStop(); started = false } }
    fun destroy() { if (!destroyed) { stop(); view.onDestroy(); destroyed = true } }
}
