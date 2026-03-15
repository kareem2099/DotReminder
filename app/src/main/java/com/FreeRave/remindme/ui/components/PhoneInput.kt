package com.FreeRave.remindme.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.FreeRave.remindme.utils.LocationUtils

@Composable
fun PhoneInput(
    phone: String,
    onPhoneChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    phoneFocusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    context: Context = LocalContext.current
) {
    val phoneNumberHintIntentResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val phoneNumber = Identity.getSignInClient(context)
                .getPhoneNumberFromIntent(result.data)
            phoneNumber?.let { number ->
                val phoneNumberUtil = PhoneNumberUtil.getInstance()
                val parsedNumber = phoneNumberUtil.parse(number, null)
                onCountryCodeChange("+" + parsedNumber.countryCode)
                onPhoneChange(parsedNumber.nationalNumber.toString())
            }
        } catch (e: Exception) {
            Log.e("PhoneInput", "Failed to get phone number", e)
        }
    }

    // Use LocationUtils static functions

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            LocationUtils.getCountryCode(context) { countryCode: String? ->
                countryCode?.let { code: String ->
                    val phoneUtil = PhoneNumberUtil.getInstance()
                    val countryCallingCode = phoneUtil.getCountryCodeForRegion(code)
                    if (countryCallingCode > 0) {
                        onCountryCodeChange("+$countryCallingCode")
                    }
                }
            }
        }
    }

    // Check location permission and get country code if granted
    LaunchedEffect(Unit) {
        when {
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED -> {
                LocationUtils.getCountryCode(context) { countryCode: String? ->
                    countryCode?.let { code: String ->
                        val phoneUtil = PhoneNumberUtil.getInstance()
                        val countryCallingCode = phoneUtil.getCountryCodeForRegion(code)
                        if (countryCallingCode > 0) {
                            onCountryCodeChange("+$countryCallingCode")
                        }
                    }
                }
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        // Also try to get phone number hint
        try {
            val hintRequest = GetPhoneNumberHintIntentRequest.builder().build()
            val intent = Identity.getSignInClient(context)
                .getPhoneNumberHintIntent(hintRequest)
            intent.addOnSuccessListener { result ->
                phoneNumberHintIntentResultLauncher.launch(
                    IntentSenderRequest.Builder(result.intentSender).build()
                )
            }.addOnFailureListener { e ->
                Log.e("PhoneInput", "Failed to get phone hint intent", e)
            }
        } catch (e: Exception) {
            Log.e("PhoneInput", "Failed to get phone hint", e)
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Country Code Picker
        TextFieldInput(
            modifier = Modifier.weight(0.3f),
            value = countryCode,
            onValueChange = { input ->
                when {
                    input.isEmpty() -> onCountryCodeChange("")
                    input == "+" -> onCountryCodeChange("+")
                    input.startsWith("+") -> {
                        val digits = input.substring(1)
                        if (digits.all { it.isDigit() } && input.length <= 5) {
                            // Validate with libphonenumber
                            try {
                                val phoneUtil = PhoneNumberUtil.getInstance()
                                // Check if this is a valid country code
                                phoneUtil.getRegionCodeForCountryCode(digits.toInt())
                                onCountryCodeChange(input)
                            } catch (e: Exception) {
                                // Keep previous valid code if new one is invalid
                            }
                        }
                    }
                }
            },
            label = "Country Code",
            placeholder = "+XXX",
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { phoneFocusRequester.requestFocus() }
            )
            // Consider adding error state if needed for country code
        )

        // Phone Number Input
        TextFieldInput(
            modifier = Modifier
                .weight(0.7f) // Adjust weight as needed
                .focusRequester(phoneFocusRequester),
            value = phone,
            onValueChange = { rawInput ->
                 // Allow only digits and basic formatting characters if desired
                 // Or handle formatting more robustly elsewhere
                 onPhoneChange(rawInput.filter { it.isDigit() }) // Simple digit filter for now
                 /*
                 // Previous formatting logic (can be complex and might interfere with typing)
                 val util = PhoneNumberUtil.getInstance()
                 try {
                     // Attempt to parse only if country code is valid
                     val regionCode = if (countryCode.length > 1) {
                         util.getRegionCodeForCountryCode(countryCode.substring(1).toInt())
                     } else {
                         null // Or a default region
                     }

                     if (regionCode != null) {
                         val number = util.parse(rawInput, regionCode)
                         // Format nationally, but this might jump cursor/change input unexpectedly
                         val formatted = util.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
                         onPhoneChange(formatted)
                     } else {
                         onPhoneChange(rawInput) // Fallback if country code is invalid/incomplete
                     }
                 } catch (e: Exception) {
                     // Handle parsing errors, maybe just update with raw input
                     onPhoneChange(rawInput)
                 }
                 */
            },
            label = "Phone Number",
            placeholder = "Enter phone number",
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Please enter a valid phone number")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { nextFocusRequester.requestFocus() }
            )
        )
    }
}
