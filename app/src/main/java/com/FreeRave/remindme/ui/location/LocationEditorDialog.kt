package com.FreeRave.remindme.ui.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassFormDialog

internal enum class LocationWork { CURRENT, ADDRESS }

@Composable
internal fun LocationEditorDialog(
    draft: LocationDraft, onChange: (LocationDraft) -> Unit, onSelected: (Double, Double, Float) -> Unit,
    onDismiss: () -> Unit, onCurrent: () -> Unit, onSearch: () -> Unit,
    working: LocationWork? = null, error: String? = null, map: (@Composable () -> Unit)? = null
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val enabled = working == null
    val confirm = {
        submitted = true
        val lat = draft.parsedLatitude; val lng = draft.parsedLongitude; val radius = draft.parsedRadius
        if (enabled && lat != null && lng != null && radius != null) onSelected(lat, lng, radius)
    }
    GlassFormDialog(stringResource(if (map == null) R.string.polish_location_title else R.string.polish_map_title),
        stringResource(R.string.polish_location_save), confirm, onDismiss,
        subtitle = stringResource(if (map == null) R.string.polish_location_hint else R.string.polish_map_hint),
        confirmEnabled = enabled, error = error) {
        map?.invoke()
        OutlinedButton(onClick = onCurrent, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(if (working == LocationWork.CURRENT) R.string.polish_location_loading else R.string.polish_current_location))
        }
        OutlinedTextField(draft.address, { onChange(draft.copy(address = it)) }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.polish_address)) }, enabled = enabled, singleLine = true, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (enabled && draft.address.isNotBlank()) { focus.clearFocus(); onSearch() } }))
        TextButton(onClick = { focus.clearFocus(); onSearch() }, enabled = enabled && draft.address.isNotBlank()) {
            Text(stringResource(if (working == LocationWork.ADDRESS) R.string.polish_address_loading else R.string.polish_search_address))
        }
        if (!enabled) LinearProgressIndicator(Modifier.fillMaxWidth())
        CoordinateField(draft.latitude, { onChange(draft.copy(latitude = it)) }, R.string.polish_latitude,
            submitted && draft.parsedLatitude == null, R.string.polish_latitude_error, enabled) { focus.moveFocus(FocusDirection.Down) }
        CoordinateField(draft.longitude, { onChange(draft.copy(longitude = it)) }, R.string.polish_longitude,
            submitted && draft.parsedLongitude == null, R.string.polish_longitude_error, enabled) { focus.moveFocus(FocusDirection.Down) }
        CoordinateField(draft.radius, { onChange(draft.copy(radius = it)) }, R.string.polish_radius,
            submitted && draft.parsedRadius == null, R.string.polish_radius_error, enabled, done = true, onIme = { confirm() })
    }
}

@Composable
private fun CoordinateField(value: String, onChange: (String) -> Unit, label: Int, error: Boolean, errorLabel: Int,
    enabled: Boolean, done: Boolean = false, onIme: () -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        shape = RoundedCornerShape(16.dp), label = { Text(stringResource(label)) }, isError = error,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        supportingText = if (error) {{ Text(stringResource(errorLabel)) }} else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (done) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onIme() }, onDone = { if (enabled) onIme() }))
}
