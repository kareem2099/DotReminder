package com.FreeRave.remindme.screens.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.auth.SignUpField
import com.FreeRave.remindme.screens.auth.SignUpUiState

@Composable
internal fun SignUpPhoneFields(state: SignUpUiState, onEdit: (SignUpField, String) -> Unit, error: Boolean) {
    val focus = LocalFocusManager.current
    // Stacked inputs keep international numbers and enlarged text readable on small screens.
    OutlinedTextField(state.countryCode, { onEdit(SignUpField.COUNTRY_CODE, it) }, Modifier.fillMaxWidth(),
        enabled = !state.busy, singleLine = true, shape = RoundedCornerShape(16.dp), isError = error,
        label = { Text(stringResource(R.string.signup_country_code)) }, placeholder = { Text("+20") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
    OutlinedTextField(state.phone, { onEdit(SignUpField.PHONE, it) }, Modifier.fillMaxWidth(),
        enabled = !state.busy, singleLine = true, shape = RoundedCornerShape(16.dp), isError = error,
        label = { Text(stringResource(R.string.signup_phone)) },
        supportingText = { Text(stringResource(if (error) R.string.signup_phone_error else R.string.signup_phone_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
}
