package com.FreeRave.remindme.screens.auth

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import android.content.Intent
import androidx.activity.ComponentActivity 
import androidx.fragment.app.FragmentActivity 

import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.User
import com.FreeRave.remindme.R
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CancellationException
import com.FreeRave.remindme.service.BiometricAuthManager 
import com.FreeRave.remindme.service.BiometricAvailability 
import com.FreeRave.remindme.service.FacebookSignInOnlyService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.GoogleAuthServiceInterface 

import com.google.firebase.auth.FirebaseAuth 
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.FreeRave.remindme.service.AuthSessionRecorder
import com.FreeRave.remindme.security.mfa.*
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.FreeRave.remindme.security.lock.AppLockSession
import com.FreeRave.remindme.security.lock.AppLockError
import com.FreeRave.remindme.security.AuthNetworkTimeout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth, // Inject FirebaseAuth directly
    val firebaseAuthService: FirebaseAuthServiceInterface, // Still needed for email/pass, etc.
    private val googleAuthService: GoogleAuthServiceInterface, // Use the new combined service
    private val facebookSignInOnlyService: FacebookSignInOnlyService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val biometricAuthManager: BiometricAuthManager, // Injected BiometricAuthManager
    private val sessionRecorder: AuthSessionRecorder,
    private val appLockSession: AppLockSession,
    @ApplicationContext private val applicationContext: Context // Injected ApplicationContext
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    private val _activeLoginMethod = MutableStateFlow<LoginMethod?>(null)
    internal val activeLoginMethod: StateFlow<LoginMethod?> = _activeLoginMethod
    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    internal val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState
    private val _preferenceError = MutableStateFlow<String?>(null)
    internal val preferenceError: StateFlow<String?> = _preferenceError

    private enum class MfaPurpose { LOGIN, PASSWORD_CHANGE }
    private var mfaPurpose: MfaPurpose? = null
    private val secondFactor = MfaChallengeController(viewModelScope,
        onVerified = { user ->
            val purpose = mfaPurpose
            mfaPurpose = null
            if (purpose == MfaPurpose.LOGIN) acceptAuthenticatedUser(user)
            else if (purpose == MfaPurpose.PASSWORD_CHANGE) _changePasswordState.value = ChangePasswordState.Success(
                applicationContext.getString(R.string.mfa_password_changed))
        }, onCancelled = {
            if (mfaPurpose == MfaPurpose.PASSWORD_CHANGE) _changePasswordState.value = ChangePasswordState.Idle
            else cancelLogin()
            mfaPurpose = null
        })
    internal val secondFactorState = secondFactor.state
    internal fun selectSecondFactor(id: String) = secondFactor.select(id)
    internal fun verifySecondFactor(code: String) = secondFactor.verify(code)
    internal fun editSecondFactor() = secondFactor.edit()
    internal fun cancelSecondFactor() = secondFactor.cancel()
    internal fun clearSecondFactor() {
        secondFactor.clear()
        if (mfaPurpose == MfaPurpose.PASSWORD_CHANGE) _changePasswordState.value = ChangePasswordState.Idle
        if (mfaPurpose == MfaPurpose.LOGIN) cancelLogin()
        mfaPurpose = null
    }

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
            userPreferencesRepository.rememberMeEnabled.catch { _rememberMeEnabled.value = false }.collect { isEnabled ->
                _rememberMeEnabled.value = isEnabled
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.rememberedEmail.catch { _rememberedEmail.value = null }.collect { email ->
                _rememberedEmail.value = email
            }
        }
        viewModelScope.launch {
            combine(firebaseAuthService.currentUserIdFlow(), userPreferencesRepository.biometricLockPreference) { uid, preference ->
                preference.protects(uid)
            }.catch { _biometricLoginEnabled.value = false }.collect { isEnabled ->
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
        _preferenceError.value = null
        viewModelScope.launch {
            try { userPreferencesRepository.saveRememberMeEnabled(isEnabled) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { _preferenceError.value = applicationContext.getString(R.string.login_preference_error) }
        }
    }

    fun updateBiometricPreferenceDirectly(isEnabled: Boolean, onResult: (Boolean) -> Unit = {}) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                if (isEnabled) { onResult(false); return@launch }
                userPreferencesRepository.saveBiometricLock(uid, false)
                onResult(true)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { onResult(false) }
        }
    }

    private var setupPrompt: androidx.biometric.BiometricPrompt? = null
    fun cancelBiometricSetup() { setupPrompt?.cancelAuthentication(); setupPrompt = null }

    fun attemptToEnableBiometricLogin(activity: FragmentActivity, onResult: (Boolean, String?) -> Unit) {
        if (_biometricAuthState.value is BiometricAuthState.Authenticating) return
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (getBiometricAvailability() != BiometricAvailability.AVAILABLE) {
            onResult(false, applicationContext.getString(R.string.app_lock_unavailable)); return
        }
        val attempt = appLockSession.controller.begin(uid) ?: return
        _biometricAuthState.value = BiometricAuthState.Authenticating
        try {
            setupPrompt = biometricAuthManager.authenticate(activity,
                title = applicationContext.getString(R.string.app_lock_setting),
                subtitle = applicationContext.getString(R.string.app_lock_prompt),
                onSuccess = {
                    setupPrompt = null
                    if (firebaseAuth.currentUser?.uid != uid || !appLockSession.controller.accept(attempt)) {
                        _biometricAuthState.value = BiometricAuthState.Idle
                        onResult(false, applicationContext.getString(R.string.app_lock_cancelled))
                    } else viewModelScope.launch {
                        try {
                            userPreferencesRepository.saveBiometricLock(uid, true)
                            _biometricAuthState.value = BiometricAuthState.Idle
                            onResult(true, applicationContext.getString(R.string.app_lock_enabled))
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) {
                            _biometricAuthState.value = BiometricAuthState.Idle
                            onResult(false, applicationContext.getString(R.string.app_lock_failed))
                        }
                    }
                }, onFailedAttempt = { }, onError = { _, _ ->
                    setupPrompt = null
                    appLockSession.controller.reject(attempt, AppLockError.CANCELLED)
                    _biometricAuthState.value = BiometricAuthState.Idle
                    onResult(false, applicationContext.getString(R.string.app_lock_cancelled))
                })
        } catch (_: RuntimeException) {
            appLockSession.controller.reject(attempt, AppLockError.UNAVAILABLE)
            _biometricAuthState.value = BiometricAuthState.Idle
            onResult(false, applicationContext.getString(R.string.app_lock_unavailable))
        }
    }

    fun getBiometricAvailability(): BiometricAvailability {
        return biometricAuthManager.getBiometricAvailability(applicationContext)
    }

    fun loginWithEmail(email: String, password: String) {
        if (!validateLogin(email, password).valid || !beginLogin(LoginMethod.EMAIL)) return
        viewModelScope.launch {
            try {
                firebaseAuthService.loginWithEmail(email.trim(), password,
                    onSuccess = ::acceptAuthenticatedUser, onFailure = ::failLogin)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failLogin(e) }
        }
    }

    fun signInWithGoogleCredentialManager(context: Context) {
        if (!beginLogin(LoginMethod.GOOGLE)) return
        viewModelScope.launch {
            try {
                googleAuthService.signInWithGoogleCredentialManager(context,
                    onSuccess = ::acceptAuthenticatedUser, onFailure = ::failLogin)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failLogin(e) }
        }
    }

    // --- Firebase specific methods ---
    fun sendPasswordResetEmail(email: String) {
        if (_passwordResetState.value is PasswordResetState.Loading || _loginState.value is LoginState.Loading || mfaPurpose != null) return
        if (!com.FreeRave.remindme.utils.ValidationUtils.isValidEmail(email.trim())) {
            _passwordResetState.value = PasswordResetState.Error(applicationContext.getString(R.string.login_email_error))
            return
        }
        _passwordResetState.value = PasswordResetState.Loading
        viewModelScope.launch {
            try {
                firebaseAuthService.sendPasswordResetEmail(email.trim())
                _passwordResetState.value = PasswordResetState.Sent(email.trim())
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _passwordResetState.value = PasswordResetState.Error(authError(e, reset = true)) }
        }
    }

    fun resetPasswordResetState() {
        if (_passwordResetState.value !is PasswordResetState.Loading) _passwordResetState.value = PasswordResetState.Idle
    }

    fun clearLoginError() {
        _preferenceError.value = null
        if (_loginState.value is LoginState.Error) _loginState.value = LoginState.Idle
        if (_biometricAuthState.value is BiometricAuthState.Error) _biometricAuthState.value = BiometricAuthState.Idle
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
        if (!beginLogin(LoginMethod.FACEBOOK)) return
        viewModelScope.launch {
            try {
                facebookSignInOnlyService.signIn(activity, onSuccess = ::acceptAuthenticatedUser, onFailure = ::failLogin)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { failLogin(error) }
        }
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

    private fun beginLogin(method: LoginMethod): Boolean {
        if (_loginState.value is LoginState.Loading || _passwordResetState.value is PasswordResetState.Loading || mfaPurpose != null) return false
        _activeLoginMethod.value = method
        _loginState.value = LoginState.Loading
        return true
    }

    private fun acceptAuthenticatedUser(user: User) {
        val current = firebaseAuth.currentUser
        if (current == null || current.uid != user.id) { failLogin(IllegalStateException()); return }
        appLockSession.controller.verifiedSignIn(current.uid)
        _activeLoginMethod.value = null
        _loginState.value = LoginState.Success(user.copy(isEmailVerified = current.isEmailVerified))
        sessionRecorder.record(current.uid, (current.email ?: user.email).takeIf { _rememberMeEnabled.value })
    }

    private fun cancelLogin() {
        _activeLoginMethod.value = null
        _loginState.value = LoginState.Idle
    }

    private fun failLogin(error: Exception) {
        if (error is CancellationException) throw error
        if (error is GetCredentialCancellationException || error is MfaCancelled) { cancelLogin(); return }
        if (error is FirebaseAuthMultiFactorException) {
            mfaPurpose = MfaPurpose.LOGIN
            _activeLoginMethod.value = null
            _loginState.value = LoginState.SecondFactor
            secondFactor.present(FirebaseMfaChallenge(error.resolver))
            return
        }
        _activeLoginMethod.value = null
        _loginState.value = LoginState.Error(authError(error))
    }

    private fun authError(error: Exception, reset: Boolean = false): String = applicationContext.getString(when (error) {
        is FirebaseNetworkException -> R.string.login_error_network
        is AuthNetworkTimeout -> R.string.login_error_network
        is FirebaseTooManyRequestsException -> R.string.login_error_rate_limit
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
            if (reset) R.string.login_reset_error else R.string.login_error_credentials
        else -> if (reset) R.string.login_reset_error else R.string.login_error_generic
    })

    fun hasBiometricSession(): Boolean = canUnlockBiometricSession(
        _rememberMeEnabled.value, _rememberedEmail.value, _biometricLoginEnabled.value,
        getBiometricAvailability() == BiometricAvailability.AVAILABLE,
        firebaseAuth.currentUser?.uid, firebaseAuth.currentUser?.email)

    // --- Login State ---
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object SecondFactor : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    // --- Biometric Authentication Logic ---
    fun triggerBiometricAuth(activity: FragmentActivity) {
        if (!hasBiometricSession()) {
            _biometricAuthState.value = BiometricAuthState.Error(applicationContext.getString(R.string.login_error_biometric))
            return
        }
        if (!beginLogin(LoginMethod.BIOMETRIC)) return
        biometricAttemptCount = 0
        _biometricAuthState.value = BiometricAuthState.Authenticating
        try { launchBiometricPrompt(activity) }
        catch (_: RuntimeException) {
            cancelLogin()
            _biometricAuthState.value = BiometricAuthState.Error(applicationContext.getString(R.string.login_biometric_unavailable))
        }
    }

    private fun launchBiometricPrompt(activity: FragmentActivity) {
        biometricAuthManager.authenticate(activity = activity,
            onSuccess = {
                val current = firebaseAuth.currentUser
                if (current != null && hasBiometricSession()) {
                    _biometricAuthState.value = BiometricAuthState.Success
                    acceptAuthenticatedUser(User(id = current.uid, email = current.email.orEmpty(),
                        name = current.displayName ?: current.email.orEmpty().substringBefore('@'),
                        photoUrl = current.photoUrl?.toString(), isEmailVerified = current.isEmailVerified))
                } else {
                    cancelLogin()
                    _biometricAuthState.value = BiometricAuthState.Error(applicationContext.getString(R.string.login_error_biometric))
                }
            },
            onFailedAttempt = {
                biometricAttemptCount++
                _biometricAuthState.value = BiometricAuthState.FailedAttempt((maxBiometricAttempts - biometricAttemptCount).coerceAtLeast(0))
            },
            onError = { errorCode, message ->
                cancelLogin()
                val cancelled = errorCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                _biometricAuthState.value = if (cancelled) BiometricAuthState.Idle else BiometricAuthState.Error(message.toString())
            })
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
        if (_changePasswordState.value is ChangePasswordState.Loading || mfaPurpose != null) return
        _changePasswordState.value = ChangePasswordState.Loading
        viewModelScope.launch {

            firebaseAuthService.reauthenticateAndChangePassword(
                currentPassword = currentPassword,
                newPassword = newPassword,
                onSuccess = {
                    _changePasswordState.value =
                        ChangePasswordState.Success("Password changed successfully.")
                },
                onFailure = { e ->
                    if (e is MfaRequired) {
                        mfaPurpose = MfaPurpose.PASSWORD_CHANGE
                        _changePasswordState.value = ChangePasswordState.SecondFactor
                        secondFactor.present(e.challenge)
                    } else _changePasswordState.value = ChangePasswordState.Error(
                        applicationContext.getString(R.string.mfa_password_change_failed))
                }
            )
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState.Idle
    }

    sealed class ChangePasswordState {
        object Idle : ChangePasswordState()
        object Loading : ChangePasswordState()
        object SecondFactor : ChangePasswordState()
        data class Success(val message: String) : ChangePasswordState()
        data class Error(val message: String) : ChangePasswordState()
    }

    override fun onCleared() { cancelBiometricSetup(); secondFactor.clear() }
}
