package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Phone input field with integrated Send OTP button
 *
 * @param phone Current phone number value
 * @param onPhoneChange Callback when phone number changes
 * @param countryCode Current country code (e.g., "+20")
 * @param onCountryCodeChange Callback when country code changes
 * @param phoneError Whether there's a validation error
 * @param phoneFocusRequester FocusRequester for this field
 * @param onSendOTP Callback when Send OTP button is clicked
 * @param isLoading Whether OTP is currently being sent
 * @param otpSent Whether OTP has been successfully sent
 * @param modifier Modifier for the container
 */
@Composable
fun PhoneFieldWithOTP(
    phone: String,
    onPhoneChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    phoneError: Boolean,
    phoneFocusRequester: FocusRequester,
    onSendOTP: () -> Unit,
    isLoading: Boolean,
    otpSent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            placeholder = { Text("1234567890") },
            isError = phoneError,
            supportingText = {
                if (phoneError) {
                    Text(
                        "Please enter a valid phone number",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (otpSent) {
                    Text(
                        "OTP sent successfully!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            leadingIcon = {
                // Country Code Display
                Text(
                    text = countryCode,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            trailingIcon = if (!otpSent) {
                // Send OTP Button inside the field
                {
                    TextButton(
                        onClick = onSendOTP,
                        enabled = !isLoading && phone.length >= 10,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Send OTP",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(phoneFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                errorIndicatorColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )
    }
}
