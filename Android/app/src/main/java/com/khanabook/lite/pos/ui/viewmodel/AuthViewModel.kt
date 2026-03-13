package com.khanabook.lite.pos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.khanabook.lite.pos.BuildConfig
import com.khanabook.lite.pos.R
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import com.khanabook.lite.pos.data.local.entity.UserEntity
import com.khanabook.lite.pos.data.remote.*
import com.khanabook.lite.pos.data.repository.RestaurantRepository
import com.khanabook.lite.pos.data.repository.UserRepository
import com.khanabook.lite.pos.domain.manager.AuthManager
import com.khanabook.lite.pos.domain.manager.SyncManager
import com.khanabook.lite.pos.domain.util.findActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel"

// Maximum consecutive failed login attempts before lockout
private const val MAX_FAILED_ATTEMPTS = 5

@HiltViewModel
class AuthViewModel
@Inject
constructor(
        private val userRepository: UserRepository,
        private val whatsAppApiService: WhatsAppApiService,
        private val restaurantRepository: RestaurantRepository,
        private val syncManager: SyncManager,
        private val sessionManager: com.khanabook.lite.pos.domain.manager.SessionManager,
        private val authManager: AuthManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            userRepository.loadPersistedUser()
        }
    }

    val currentUser: StateFlow<UserEntity?> = userRepository.currentUser

    private val _loginStatus = MutableStateFlow<LoginResult?>(null)
    val loginStatus: StateFlow<LoginResult?> = _loginStatus

    private val _signUpStatus = MutableStateFlow<SignUpResult?>(null)
    val signUpStatus: StateFlow<SignUpResult?> = _signUpStatus

    private val _resetPasswordStatus = MutableStateFlow<ResetPasswordResult?>(null)
    val resetPasswordStatus: StateFlow<ResetPasswordResult?> = _resetPasswordStatus

    private val _otpVerificationStatus = MutableStateFlow<OtpVerificationResult?>(null)
    val otpVerificationStatus: StateFlow<OtpVerificationResult?> = _otpVerificationStatus

    // OTP stored privately in ViewModel memory — never exposed through StateFlow
    private var generatedOtp: String? = null

    // â”€â”€ Brute-force protection
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private var failedLoginAttempts = 0
    private var lockoutUntilMs: Long = 0L

    fun login(email: String, password: String) {
        // Brute-force lockout check
        val now = System.currentTimeMillis()
        if (now < lockoutUntilMs) {
            val remainingSeconds = (lockoutUntilMs - now) / 1000
            _loginStatus.value =
                    LoginResult.Error(
                            "Too many failed attempts. Try again in $remainingSeconds seconds."
                    )
            return
        }

        viewModelScope.launch {
            _loginStatus.value = null // Clear previous status

            val localHash = authManager.hashPassword(password)
            val result = userRepository.remoteLogin(email, password, localHash)

            result.onSuccess { user ->
                Log.d(TAG, "Remote login success for: $email")
                failedLoginAttempts = 0
                lockoutUntilMs = 0L

                // Reset sync state only on successful login
                sessionManager.saveLastSyncTimestamp(0L)
                sessionManager.setInitialSyncCompleted(false)

                // Trigger immediate sync
                syncManager.performMasterPull()

                // Sync WhatsApp number to Shop Configuration
                user.whatsappNumber?.let { number ->
                    viewModelScope.launch {
                        val currentProfile = restaurantRepository.getProfile()
                        if (currentProfile != null) {
                            if (currentProfile.whatsappNumber != number) {
                                restaurantRepository.saveProfile(currentProfile.copy(whatsappNumber = number))
                            }
                        } else {
                            restaurantRepository.saveProfile(
                                RestaurantProfileEntity(
                                    id = 1,
                                    shopName = user.name,
                                    shopAddress = "",
                                    whatsappNumber = number,
                                    upiMobile = number,
                                    lastResetDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                            )
                        }
                    }
                }

                _loginStatus.value = LoginResult.Success(user)
            }.onFailure { e ->
                Log.e(TAG, "Remote login failed: ${e.message}. Falling back to local.", e)
                // Fallback to local login if offline or server error
                val user = userRepository.getUserByEmail(email)
                if (user != null) {
                    val verified = authManager.verifyPassword(password, user.passwordHash.orEmpty())
                    if (verified) {
                        if (user.isActive) {
                            failedLoginAttempts = 0
                            lockoutUntilMs = 0L
                            userRepository.setCurrentUser(user)
                            _loginStatus.value = LoginResult.Success(user)
                        } else {
                            _loginStatus.value = LoginResult.Error("Account is inactive")
                        }
                    } else {
                        failedLoginAttempts++
                        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                            lockoutUntilMs = System.currentTimeMillis() + 5 * 60 * 1000L
                            _loginStatus.value = LoginResult.Error("Too many failed attempts. Locked for 5 minutes.")
                        } else {
                            val remaining = MAX_FAILED_ATTEMPTS - failedLoginAttempts
                            _loginStatus.value = LoginResult.Error("Incorrect password. $remaining attempt(s) remaining.")
                        }
                    }
                } else {
                    failedLoginAttempts++
                    if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                        lockoutUntilMs = System.currentTimeMillis() + 5 * 60 * 1000L
                        _loginStatus.value = LoginResult.Error("Too many failed attempts. Locked for 5 minutes.")
                    } else {
                        _loginStatus.value = LoginResult.Error("No account found with this number or server is offline.")
                    }
                }
            }

        }
    }

    fun sendOtp(phoneNumber: String, purpose: String = "signup") {
        viewModelScope.launch {
            // Generate OTP â€” stored privately, never exposed in state
            val otp = (100000..999999).random().toString()
            generatedOtp = otp

            try {
                // Check if user exists for reset password
                if (purpose == "reset") {
                    val user = userRepository.getUserByEmail(phoneNumber)
                    if (user == null) {
                        _resetPasswordStatus.value =
                                ResetPasswordResult.Error("No account found with this number")
                        generatedOtp = null
                        return@launch
                    }
                }

                val formattedPhone = if (phoneNumber.length == 10) "91$phoneNumber" else phoneNumber

                val request =
                        WhatsAppRequest(
                                to = formattedPhone,
                                template =
                                        WhatsAppTemplate(
                                                name = BuildConfig.WHATSAPP_OTP_TEMPLATE_NAME,
                                                language = Language(),
                                                components =
                                                        listOf(
                                                                Component(
                                                                        type = "body",
                                                                        parameters =
                                                                                listOf(
                                                                                        Parameter(
                                                                                                text =
                                                                                                        otp
                                                                                        ) // Maps to
                                                                                        // {{1}}
                                                                                        )
                                                                ),
                                                                Component(
                                                                        type = "button",
                                                                        sub_type = "url",
                                                                        index = "0",
                                                                        parameters =
                                                                                listOf(
                                                                                        Parameter(
                                                                                                text =
                                                                                                        otp
                                                                                        ) // Value
                                                                                        // for
                                                                                        // Copy
                                                                                        // Code
                                                                                        // button
                                                                                        )
                                                                )
                                                        )
                                        )
                        )

                val response =
                        whatsAppApiService.sendOtp(
                                phoneNumberId = BuildConfig.WHATSAPP_PHONE_NUMBER_ID,
                                token = "Bearer ${BuildConfig.META_ACCESS_TOKEN}",
                                request = request
                        )

                if (response.isSuccessful) {
                    // âœ… OTP is NOT included in the state â€” only a "sent" signal
                    when (purpose) {
                        "reset" -> _resetPasswordStatus.value = ResetPasswordResult.OtpSent
                        "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.OtpSent
                        else -> _signUpStatus.value = SignUpResult.OtpSent
                    }
                } else {
                    val errorMsg = "Failed to send WhatsApp OTP. Please try again."
                    generatedOtp = null
                    when (purpose) {
                        "reset" -> _resetPasswordStatus.value = ResetPasswordResult.Error(errorMsg)
                        "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.Error(errorMsg)
                        else -> _signUpStatus.value = SignUpResult.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Network error. Please check your connection."
                generatedOtp = null
                when (purpose) {
                    "reset" -> _resetPasswordStatus.value = ResetPasswordResult.Error(errorMsg)
                    "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.Error(errorMsg)
                    else -> _signUpStatus.value = SignUpResult.Error(errorMsg)
                }
            }
        }
    }

    fun verifyOtp(enteredOtp: String): Boolean {
        val valid = enteredOtp.isNotBlank() && enteredOtp == generatedOtp
        if (valid) {
            generatedOtp = null // Invalidate after successful verification
        }
        return valid
    }

    fun signUp(name: String, phoneNumber: String, password: String) {
        viewModelScope.launch {
            try {
                // 1. Create User remotely to get JWT Token
                val localHash = authManager.hashPassword(password)
                val result = userRepository.remoteSignup(name, phoneNumber, password, localHash)
                
                result.onSuccess {
                    // Reset sync state only after successful signup
                    sessionManager.saveLastSyncTimestamp(0L)
                    sessionManager.setInitialSyncCompleted(false)

                    // 2. Update Shop Profile with signup details
                    val currentProfile = restaurantRepository.getProfile()
                    val updatedProfile =
                            if (currentProfile != null) {
                                currentProfile.copy(
                                        shopName = name,
                                        whatsappNumber = phoneNumber,
                                        upiMobile = phoneNumber
                                )
                            } else {
                                RestaurantProfileEntity(
                                        id = 1,
                                        shopName = name,
                                        shopAddress = "",
                                        whatsappNumber = phoneNumber,
                                        upiMobile = phoneNumber,
                                        lastResetDate =
                                                java.text.SimpleDateFormat(
                                                                "yyyy-MM-dd",
                                                                java.util.Locale.getDefault()
                                                        )
                                                        .format(java.util.Date())
                                )
                            }
                    restaurantRepository.saveProfile(updatedProfile)
                    
                    // 3. Trigger immediate sync
                    syncManager.performMasterPull()

                    _signUpStatus.value = SignUpResult.Success
                }.onFailure { e ->
                    _signUpStatus.value = SignUpResult.Error(e.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                _signUpStatus.value = SignUpResult.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun resetPassword(phoneNumber: String, newPassword: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserByEmail(phoneNumber)
                if (user != null) {
                    val newHash = authManager.hashPassword(newPassword)
                    userRepository.updatePasswordHash(user.id, newHash)
                    _resetPasswordStatus.value = ResetPasswordResult.Success
                } else {
                    _resetPasswordStatus.value = ResetPasswordResult.Error("User not found")
                }
            } catch (e: Exception) {
                _resetPasswordStatus.value =
                        ResetPasswordResult.Error(e.message ?: "Failed to reset password")
            }
        }
    }

    fun logout() {
        userRepository.setCurrentUser(null)
        generatedOtp = null
        failedLoginAttempts = 0
        lockoutUntilMs = 0L
        _loginStatus.value = null
        _signUpStatus.value = null
        _resetPasswordStatus.value = null
    }

    /**
     * Real Google Sign-In using Credential Manager (modern Android API). Requires an Activity
     * context â€” pass LocalContext.current from the composable.
     */
    fun loginWithGoogle(context: Context) {
        val activity = context.findActivity()
        if (activity == null) {
            _loginStatus.value = LoginResult.Error("Google Sign-In: activity context not found")
            return
        }

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption =
                        GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false) // show all Google accounts
                                .setServerClientId(
                                        activity.getString(R.string.default_web_client_id)
                                )
                                .setAutoSelectEnabled(false)
                                .build()

                val request =
                        GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

                val result =
                        credentialManager.getCredential(
                                context = activity,
                                request = request
                        )

                val credential = result.credential
                if (credential is CustomCredential &&
                                credential.type ==
                                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCred.idToken

                    // Reset sync state for fresh start
                    sessionManager.saveLastSyncTimestamp(0L)
                    sessionManager.setInitialSyncCompleted(false)

                    // Call backend to verify Google ID Token and get our own JWT
                    val result = userRepository.remoteGoogleLogin(idToken)
                    
                    result.onSuccess { user ->
                        failedLoginAttempts = 0
                        lockoutUntilMs = 0L

                        // Reset sync state only on successful Google login
                        sessionManager.saveLastSyncTimestamp(0L)
                        sessionManager.setInitialSyncCompleted(false)

                        // Trigger immediate sync
                        syncManager.performMasterPull()

                        // Sync WhatsApp number to Shop Configuration
                        user.whatsappNumber?.let { number ->
                            viewModelScope.launch {
                                val currentProfile = restaurantRepository.getProfile()
                                if (currentProfile != null) {
                                    if (currentProfile.whatsappNumber != number) {
                                        restaurantRepository.saveProfile(currentProfile.copy(whatsappNumber = number))
                                    }
                                } else {
                                    restaurantRepository.saveProfile(
                                        RestaurantProfileEntity(
                                            id = 1,
                                            shopName = user.name,
                                            shopAddress = "",
                                            whatsappNumber = number,
                                            upiMobile = number,
                                            lastResetDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        )
                                    )
                                }
                            }
                        }

                        _loginStatus.value = LoginResult.Success(user)
                    }.onFailure { e ->
                        Log.e(TAG, "Remote Google login failed", e)
                        _loginStatus.value = LoginResult.Error("Google sync failed: ${e.message}")
                    }
                } else {
                    _loginStatus.value =
                            LoginResult.Error("Google Sign-In: unexpected credential type")
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google Sign-In cancelled or unavailable", e)
                _loginStatus.value = LoginResult.Error("Google Sign-In cancelled. Try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _loginStatus.value = LoginResult.Error("Google Sign-In failed. Please try again.")
            }
        }
    }



    fun resetSignUpStatus() {
        _signUpStatus.value = null
    }

    fun clearResetStatus() {
        _resetPasswordStatus.value = null
    }

    fun clearOtpStatus() {
        _otpVerificationStatus.value = null
    }

    sealed class LoginResult {
        data class Success(val user: UserEntity) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    sealed class SignUpResult {
        object Success : SignUpResult()
        // âœ… OTP removed from state â€” prevents OTP leakage through StateFlow
        object OtpSent : SignUpResult()
        data class Error(val message: String) : SignUpResult()
    }

    sealed class ResetPasswordResult {
        object Success : ResetPasswordResult()
        // âœ… OTP removed from state â€” prevents OTP leakage through StateFlow
        object OtpSent : ResetPasswordResult()
        data class Error(val message: String) : ResetPasswordResult()
    }

    sealed class OtpVerificationResult {
        object Success : OtpVerificationResult()
        object OtpSent : OtpVerificationResult()
        data class Error(val message: String) : OtpVerificationResult()
    }
}
