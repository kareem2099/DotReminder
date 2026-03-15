package com.FreeRave.remindme.screens.auth

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import android.content.Intent
import androidx.activity.ComponentActivity // Added for Facebook
import androidx.fragment.app.FragmentActivity // For BiometricPrompt
// Duplicates removed below
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.User
import com.FreeRave.remindme.service.BiometricAuthManager // Added import
import com.FreeRave.remindme.service.BiometricAvailability // Added import
import com.FreeRave.remindme.service.FacebookSignInOnlyService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.GoogleAuthServiceInterface // Changed from GoogleSignInOnlyService
// import com.FreeRave.remindme.service.GoogleSignInOnlyService // No longer needed directly
import com.FreeRave.remindme.service.VerificationService
import com.google.firebase.auth.FirebaseAuth // Added for direct auth state checking
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // Added import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth, // Inject FirebaseAuth directly
    val firebaseAuthService: FirebaseAuthServiceInterface, // Still needed for email/pass, etc.
    private val googleAuthService: GoogleAuthServiceInterface, // Use the new combined service
    private val facebookSignInOnlyService: FacebookSignInOnlyService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val biometricAuthManager: BiometricAuthManager, // Injected BiometricAuthManager
    val verificationService: VerificationService,
    @ApplicationContext private val applicationContext: Context // Injected ApplicationContext
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _rememberMeEnabled = MutableStateFlow(false)
    val rememberMeEnabled: StateFlow<Boolean> = _rememberMeEnabled

    private val _rememberedEmail = MutableStateFlow<String?>(null)
    val rememberedEmail: StateFlow<String?> = _rememberedEmail

    private val _biometricLoginEnabled = MutableStateFlow(false)
    val biometricLoginEnabled: StateFlow<Boolean> = _biometricLoginEnabled

    private val _biometricAuthState = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    val biometricAuthState: StateFlow<BiometricAuthState> = _biometricAuthState
    private var biometricAttemptCount = 0
    private val maxBiometricAttempts = 5


    init {
        viewModelScope.launch {
            userPreferencesRepository.rememberMeEnabled.collect { isEnabled ->
                _rememberMeEnabled.value = isEnabled
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.rememberedEmail.collect { email ->
                _rememberedEmail.value = email
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.biometricLoginEnabled.collect { isEnabled ->
                _biometricLoginEnabled.value = isEnabled
            }
        }
        checkUserLoggedIn() // Check for existing logged-in user
    }

    private fun checkUserLoggedIn() {
        viewModelScope.launch { // Ensure UI updates are on the main thread if needed
            val currentFirebaseUser = firebaseAuth.currentUser
            if (currentFirebaseUser != null) {
                // Determine the provider. This is a simplified way.
                // A more robust way would be to check currentFirebaseUser.providerData
                val providerId = currentFirebaseUser.providerData.find { it.providerId != "firebase" }?.providerId
                val authProvider = when (providerId) {
                    com.google.firebase.auth.GoogleAuthProvider.PROVIDER_ID -> com.FreeRave.remindme.model.AuthProvider.GOOGLE
                    com.google.firebase.auth.FacebookAuthProvider.PROVIDER_ID -> com.FreeRave.remindme.model.AuthProvider.FACEBOOK
                    else -> com.FreeRave.remindme.model.AuthProvider.EMAIL // Default or could be more specific
                }

                val user = User(
                    id = currentFirebaseUser.uid,
                    name = currentFirebaseUser.displayName ?: currentFirebaseUser.email?.split("@")?.get(0) ?: "User",
                    email = currentFirebaseUser.email ?: "",
                    photoUrl = currentFirebaseUser.photoUrl?.toString(),
                    isEmailVerified = currentFirebaseUser.isEmailVerified,
                    provider = authProvider
                )
                _loginState.value = LoginState.Success(user)
            } else {
                // No user logged in, _loginState remains Idle (or set explicitly if needed)
                // _loginState.value = LoginState.Idle
            }
        }
    }

    fun userToggledRememberMe(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveRememberMeEnabled(isEnabled)
        }
    }

    // Renamed from setBiometricLoginEnabled for clarity, used for direct updates (e.g., disabling)
    fun updateBiometricPreferenceDirectly(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveBiometricLoginEnabled(isEnabled)
        }
    }

    // New function for handling biometric setup from Settings
    private var biometricSetupAttemptCount = 0 // Separate counter for setup
    fun attemptToEnableBiometricLogin(activity: FragmentActivity, onResult: (success: Boolean, message: String?) -> Unit) {
        if (getBiometricAvailability() != BiometricAvailability.AVAILABLE) {
            onResult(false, "Biometric authentication is not available on this device.")
            return
        }

        biometricSetupAttemptCount = 0 // Reset for a new setup sequence
        _biometricAuthState.value = BiometricAuthState.Authenticating // Indicate process start

        biometricAuthManager.authenticate(
            activity = activity,
            title = "Enable Biometric Login",
            subtitle = "Confirm your identity to enable biometric login",
            negativeButtonText = "Cancel",
            onSuccess = {
                viewModelScope.launch {
                    userPreferencesRepository.saveBiometricLoginEnabled(true)
                    _biometricAuthState.value = BiometricAuthState.Idle // Reset state after success
                    onResult(true, "Biometric login enabled successfully.")
                }
            },
            onFailedAttempt = {
                biometricSetupAttemptCount++
                if (biometricSetupAttemptCount >= maxBiometricAttempts) {
                    viewModelScope.launch { userPreferencesRepository.saveBiometricLoginEnabled(false) }
                    _biometricAuthState.value = BiometricAuthState.Error("Max attempts reached.") // Update state
                    onResult(false, "Max attempts reached. Biometric login not enabled.")
                } else {
                    // Prompt usually stays, inform user via onResult if needed, or rely on system's "try again"
                     _biometricAuthState.value = BiometricAuthState.FailedAttempt(remaining = maxBiometricAttempts - biometricSetupAttemptCount)
                    // For settings, we might want to call onResult immediately for each failed attempt
                    // onResult(false, "Biometric attempt failed. ${maxBiometricAttempts - biometricSetupAttemptCount} attempts remaining.")
                    // However, the prompt typically handles "try again" itself. Let's let onError handle final failure.
                }
            },
            onError = { errorCode, errString ->
                viewModelScope.launch { userPreferencesRepository.saveBiometricLoginEnabled(false) } // Ensure it's off
                val userCancelled = errorCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED ||
                                  errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                val lockedOut = errorCode == androidx.biometric.BiometricPrompt.ERROR_LOCKOUT ||
                                errorCode == androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                
                val message = if (userCancelled) {
                    "Biometric setup cancelled."
                } else if (lockedOut) {
                    "Biometric locked out. Too many attempts."
                } else {
                    errString.toString()
                }
                _biometricAuthState.value = BiometricAuthState.Error(message) // Update state
                onResult(false, message)
            }
        )
    }


    fun getBiometricAvailability(): BiometricAvailability {
        return biometricAuthManager.getBiometricAvailability(applicationContext)
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            firebaseAuthService.loginWithEmail(
                email = email,
                password = password,
                onSuccess = { user: User ->
                    viewModelScope.launch { // Ensure suspend functions are called in a coroutine
                        if (_rememberMeEnabled.value) {
                            userPreferencesRepository.saveRememberedEmail(user.email)
                        }
                        // If !rememberMeEnabled, saveRememberMeEnabled(false) would have already cleared the email
                        _loginState.value = LoginState.Success(user)
                    }
                },
                onFailure = { e: Exception ->
                    _loginState.value = LoginState.Error(e.message ?: "Login failed")
                }
            )
        }
    }

    // --- Google Sign-In Methods using GoogleAuthService ---
    fun getGoogleSignInIntent(context: Context): Intent { // Renamed for clarity, was getGoogleOnlySignInIntent
        return googleAuthService.getSignInIntent(context)
    }

    fun handleGoogleSignInResult(result: ActivityResult) { // Renamed for clarity, was handleGoogleOnlySignInResult
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            googleAuthService.handleSignInResultAndAuthenticate(
                result = result,
                onSuccess = { firebaseUser -> // firebaseUser is of type User from our model
                    viewModelScope.launch {
                        if (_rememberMeEnabled.value) {
                            userPreferencesRepository.saveRememberedEmail(firebaseUser.email)
                        }
                        _loginState.value = LoginState.Success(firebaseUser)
                    }
                },
                onFailure = { exception ->
                    _loginState.value = LoginState.Error(exception.message ?: "Google Sign-In failed.")
                }
            )
        }
    }

    // --- Firebase specific methods ---
    suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebaseAuthService.sendPasswordResetEmail(email)
        } catch (e: Exception) {
            _loginState.value = LoginState.Error(e.message ?: "Failed to send reset email")
        }
    }

    // General sign out for Firebase authenticated users
    fun signOutFirebaseUser() {
        firebaseAuthService.signOut()
        viewModelScope.launch {
            userPreferencesRepository.saveRememberMeEnabled(false) // Clears flag and email
        }
        _loginState.value = LoginState.Idle // Reset state
    }

    // Sign out for Google authenticated users
    fun signOutGoogleUser(context: Context) { // Renamed for clarity, was signOutGoogleOnlyUser
        viewModelScope.launch {
            googleAuthService.signOut(context) {
                // This onComplete runs after Google and Firebase sign-out attempts
                viewModelScope.launch { // Ensure UI updates are on main thread if needed, though state update is fine
                    userPreferencesRepository.saveRememberMeEnabled(false) // Clears flag and email
                    _loginState.value = LoginState.Idle // Reset state
                }
            }
        }
    }

    // --- New Facebook-Only Sign-In Methods ---
    fun signInWithFacebook(activity: ComponentActivity) {
        _loginState.value = LoginState.Loading
        facebookSignInOnlyService.signIn(
            activity = activity,
            onSuccess = { user ->
                viewModelScope.launch { // Ensure suspend functions are called in a coroutine
                    if (_rememberMeEnabled.value) {
                        userPreferencesRepository.saveRememberedEmail(user.email)
                    }
                    _loginState.value = LoginState.Success(user)
                }
            },
            onError = { errorMsg ->
                _loginState.value = LoginState.Error(errorMsg)
            }
        )
    }

    fun passFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // This forwards the result to the Facebook SDK's CallbackManager via the service
        facebookSignInOnlyService.passActivityResult(requestCode, resultCode, data)
    }

    fun signOutFacebookUser() {
        facebookSignInOnlyService.signOut()
        viewModelScope.launch {
            userPreferencesRepository.saveRememberMeEnabled(false) // Clears flag and email
        }
        _loginState.value = LoginState.Idle // Reset state
    }

    // --- Login State ---
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    // --- Biometric Authentication Logic ---
    fun triggerBiometricAuth(activity: FragmentActivity) {
        if (!_rememberMeEnabled.value || _rememberedEmail.value == null) {
            _biometricAuthState.value = BiometricAuthState.Error("Remember Me is not active or no email is remembered.")
            return
        }
        if (getBiometricAvailability() != BiometricAvailability.AVAILABLE) {
            _biometricAuthState.value = BiometricAuthState.Error("Biometric authentication not available.")
            return
        }

        biometricAttemptCount = 0 // Reset for a new sequence
        _biometricAuthState.value = BiometricAuthState.Authenticating
        launchBiometricPrompt(activity)
    }

    private fun launchBiometricPrompt(activity: FragmentActivity) {
        biometricAuthManager.authenticate(
            activity = activity,
            onSuccess = {
                val email = _rememberedEmail.value
                // This is a placeholder for actual session restoration.
                // In a real app, biometric success would unlock a token or credentials
                // which are then used to properly authenticate and fetch user details.
                if (email != null) {
                     // Constructing a User object. In a real scenario, you might fetch full user details.
                    val user = User(id = "bio_${System.currentTimeMillis()}", email = email, name = email.split("@")[0], isEmailVerified = true) // Placeholder
                    _loginState.value = LoginState.Success(user)
                    _biometricAuthState.value = BiometricAuthState.Success
                } else {
                     // Should not happen if initial checks in triggerBiometricAuth are correct
                    _biometricAuthState.value = BiometricAuthState.Error("Failed to retrieve remembered user for biometric login.")
                }
            },
            onFailedAttempt = { // This is BiometricPrompt's onAuthenticationFailed()
                biometricAttemptCount++
                if (biometricAttemptCount >= maxBiometricAttempts) {
                    _biometricAuthState.value = BiometricAuthState.Error("Max biometric attempts reached. Please log in manually.")
                    // The BiometricPrompt itself might also call onError with ERROR_LOCKOUT
                } else {
                    // Inform UI about a failed attempt, prompt might still be visible
                    _biometricAuthState.value = BiometricAuthState.FailedAttempt(remaining = maxBiometricAttempts - biometricAttemptCount)
                }
            },
            onError = { errorCode, errString -> // This is BiometricPrompt's onAuthenticationError()
                val message = errString.toString()
                // Check for specific error codes from BiometricPrompt, like user cancellation
                if (errorCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED || errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    _biometricAuthState.value = BiometricAuthState.Idle // User cancelled
                } else if (errorCode == androidx.biometric.BiometricPrompt.ERROR_LOCKOUT || errorCode == androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    _biometricAuthState.value = BiometricAuthState.Error("Biometric locked out. Too many attempts.")
                } else {
                    _biometricAuthState.value = BiometricAuthState.Error(message)
                }
            }
        )
    }

    sealed class BiometricAuthState {
        object Idle : BiometricAuthState()
        object Authenticating : BiometricAuthState()
        object Success : BiometricAuthState()
        data class FailedAttempt(val remaining: Int) : BiometricAuthState()
        data class Error(val message: String) : BiometricAuthState()
    }

    // --- Change Password ---
    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState.Loading
            // In a real app, you'd get the current user's email securely
            val email = firebaseAuthService.getCurrentUserId() // Assuming this can be an email or used to get one
            if (email.isBlank()) { // Or if it's not an email, handle appropriately
                 _changePasswordState.value = ChangePasswordState.Error("User not found or email missing.")
                return@launch
            }

            // This is a simplified placeholder.
            // Real implementation requires firebaseAuthService.reauthenticateAndChangePassword(email, currentPassword, newPassword, onSuccess, onFailure)
            // For now, simulate success if current password is not empty, and new password is not empty.
            if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                // Placeholder for actual re-authentication and password update logic
                // firebaseAuthService.reauthenticateUser(email, currentPassword) { reauthSuccess ->
                //    if (reauthSuccess) {
                //        firebaseAuthService.updateUserPassword(newPassword) { updateSuccess, updateMessage ->
                //            if (updateSuccess) {
                //                _changePasswordState.value = ChangePasswordState.Success("Password changed successfully.")
                //            } else {
                //                _changePasswordState.value = ChangePasswordState.Error(updateMessage ?: "Failed to update password.")
                //            }
                //        }
                //    } else {
                //        _changePasswordState.value = ChangePasswordState.Error("Re-authentication failed. Incorrect current password.")
                //    }
                // }
                _changePasswordState.value = ChangePasswordState.Success("Password changed successfully (mock).")
            } else {
                _changePasswordState.value = ChangePasswordState.Error("Passwords cannot be empty (mock).")
            }
        }
    }
    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState.Idle
    }

    sealed class ChangePasswordState {
        object Idle : ChangePasswordState()
        object Loading : ChangePasswordState()
        data class Success(val message: String) : ChangePasswordState()
        data class Error(val message: String) : ChangePasswordState()
    }
}
