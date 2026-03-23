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

    private fun loginError(message: String, code: LoginErrorCode) =
        LoginResult.Error(message, code)

    private val _signUpStatus = MutableStateFlow<SignUpResult?>(null)
    val signUpStatus: StateFlow<SignUpResult?> = _signUpStatus

    private val _resetPasswordStatus = MutableStateFlow<ResetPasswordResult?>(null)
    val resetPasswordStatus: StateFlow<ResetPasswordResult?> = _resetPasswordStatus

    private val _otpVerificationStatus = MutableStateFlow<OtpVerificationResult?>(null)
    val otpVerificationStatus: StateFlow<OtpVerificationResult?> = _otpVerificationStatus

    
    private var generatedOtp: String? = null

    
    
    private var failedLoginAttempts = 0
    private var lockoutUntilMs: Long = 0L

    fun login(email: String, password: String) {
        
        val now = System.currentTimeMillis()
        if (now < lockoutUntilMs) {
            val remainingSeconds = (lockoutUntilMs - now) / 1000
            _loginStatus.value =
                    loginError(
                            "Too many failed attempts. Try again in $remainingSeconds seconds.",
                            LoginErrorCode.LOCKED_OUT
                    )
            return
        }

        viewModelScope.launch {
            _loginStatus.value = LoginResult.Loading 
            performLogin(email, password)
        }
    }

    private suspend fun performLogin(email: String, password: String) {
        val localHash = authManager.hashPassword(password)
        val result = userRepository.remoteLogin(email, password, localHash)

        result.onSuccess { user ->
            Log.d(TAG, "Remote login success for: $email")
            handleLoginSuccess(user)
            _loginStatus.value = LoginResult.Success(user)
        }.onFailure { e ->
            Log.e(TAG, "Remote login failed: ${e.message}. Falling back to local.", e)
            
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
                        _loginStatus.value =
                                loginError("Account is inactive", LoginErrorCode.ACCOUNT_INACTIVE)
                    }
                } else {
                    failedLoginAttempts++
                    if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                        lockoutUntilMs = System.currentTimeMillis() + 5 * 60 * 1000L
                        _loginStatus.value = loginError("Too many failed attempts. Locked for 5 minutes.", LoginErrorCode.LOCKED_OUT)
                    } else {
                        val remaining = MAX_FAILED_ATTEMPTS - failedLoginAttempts
                        _loginStatus.value = loginError(
                            "Incorrect password. $remaining attempt(s) remaining.",
                            LoginErrorCode.INCORRECT_PASSWORD
                        )
                    }
                }
            } else {
                failedLoginAttempts++
                if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                    lockoutUntilMs = System.currentTimeMillis() + 5 * 60 * 1000L
                    _loginStatus.value = loginError("Too many failed attempts. Locked for 5 minutes.", LoginErrorCode.LOCKED_OUT)
                } else {
                    _loginStatus.value = loginError(
                        "No account found with this number or server is offline.",
                        LoginErrorCode.ACCOUNT_NOT_FOUND
                    )
                }
            }
        }
    }

    private suspend fun handleLoginSuccess(user: UserEntity) {
        failedLoginAttempts = 0
        lockoutUntilMs = 0L

        
        sessionManager.saveLastSyncTimestamp(0L)
        sessionManager.setInitialSyncCompleted(false)

        
        syncManager.performMasterPull()

        
        user.whatsappNumber?.let { number ->
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

    fun sendOtp(phoneNumber: String, purpose: String = "signup") {
        viewModelScope.launch {
            
            val otp = (100000..999999).random().toString()
            generatedOtp = otp
            
            

            try {
                
                if (purpose == "reset") {
                    
                    var user = userRepository.getUserByEmail(phoneNumber)
                    
                    
                    if (user == null) {
                        try {
                            val exists = userRepository.checkUserExistsRemotely(phoneNumber)
                            if (!exists) {
                                _resetPasswordStatus.value = ResetPasswordResult.Error("No account found with this number")
                                generatedOtp = null
                                return@launch
                            }
                        } catch (e: Exception) {
                            
                            _resetPasswordStatus.value = ResetPasswordResult.Error("Account not found locally and server is unreachable")
                            generatedOtp = null
                            return@launch
                        }
                    }
                }

                val formattedPhone = phoneNumber

                if (BuildConfig.META_ACCESS_TOKEN.isEmpty() || BuildConfig.WHATSAPP_PHONE_NUMBER_ID.isEmpty()) {
                    Log.d(TAG, "Meta API tokens not configured. Simulating OTP send. OTP is: $otp")
                    when (purpose) {
                        "reset" -> _resetPasswordStatus.value = ResetPasswordResult.OtpSent
                        "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.OtpSent
                        else -> _signUpStatus.value = SignUpResult.OtpSent
                    }
                    return@launch
                }

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
                                                                                        ) 
                                                                                        
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
                                                                                        ) 
                                                                                        
                                                                                        
                                                                                        
                                                                                        
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
                    
                    when (purpose) {
                        "reset" -> _resetPasswordStatus.value = ResetPasswordResult.OtpSent
                        "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.OtpSent
                        else -> _signUpStatus.value = SignUpResult.OtpSent
                    }
                } else {
                    val apiError = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "WhatsApp API Error: $apiError")
                    generatedOtp = null 
                    val errorMessage = "Failed to send OTP. Please try again."
                    when (purpose) {
                        "reset" -> _resetPasswordStatus.value = ResetPasswordResult.Error(errorMessage)
                        "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.Error(errorMessage)
                        else -> _signUpStatus.value = SignUpResult.Error(errorMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTP Send Exception: ${e.message}")
                generatedOtp = null
                val errorMessage = "Network error. Failed to send OTP."
                when (purpose) {
                    "reset" -> _resetPasswordStatus.value = ResetPasswordResult.Error(errorMessage)
                    "update_whatsapp" -> _otpVerificationStatus.value = OtpVerificationResult.Error(errorMessage)
                    else -> _signUpStatus.value = SignUpResult.Error(errorMessage)
                }
            }
        }
    }

    fun verifyOtp(enteredOtp: String): Boolean {
        val valid = enteredOtp.isNotBlank() && enteredOtp == generatedOtp
        if (valid) {
            generatedOtp = null 
        }
        return valid
    }

    fun signUp(name: String, phoneNumber: String, password: String) {
        viewModelScope.launch {
            _signUpStatus.value = SignUpResult.Loading
            try {
                
                val localHash = authManager.hashPassword(password)
                val result = userRepository.remoteSignup(name, phoneNumber, password, localHash)
                
                result.onSuccess {
                    
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
                    
                    
                    performLogin(phoneNumber, password)
                    
                    
                    if (_loginStatus.value is LoginResult.Success) {
                        _signUpStatus.value = SignUpResult.Success
                    } else if (_loginStatus.value is LoginResult.Error) {
                        val error = _loginStatus.value as LoginResult.Error
                        _signUpStatus.value = SignUpResult.Error("Signup successful but Login failed: ${error.message}")
                    }
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
                
                userRepository.remoteResetPassword(phoneNumber, newPassword)

                
                val user = userRepository.getUserByEmail(phoneNumber)
                if (user != null) {
                    val newHash = authManager.hashPassword(newPassword)
                    userRepository.updatePasswordHash(user.id, newHash)
                }
                
                _resetPasswordStatus.value = ResetPasswordResult.Success
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

    
    fun loginWithGoogle(context: Context) {
        val activity = context.findActivity()
        if (activity == null) {
            _loginStatus.value =
                    loginError(
                            "Google Sign-In: activity context not found",
                            LoginErrorCode.GOOGLE_CONTEXT_MISSING
                    )
            return
        }

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption =
                        GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false) 
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

                    
                    sessionManager.saveLastSyncTimestamp(0L)
                    sessionManager.setInitialSyncCompleted(false)

                    
                    val result = userRepository.remoteGoogleLogin(idToken)
                    
                    result.onSuccess { user ->
                        failedLoginAttempts = 0
                        lockoutUntilMs = 0L

                        
                        sessionManager.saveLastSyncTimestamp(0L)
                        sessionManager.setInitialSyncCompleted(false)

                        
                        syncManager.performMasterPull()

                        
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
                        _loginStatus.value = loginError(
                            "Google sync failed: ${e.message}",
                            LoginErrorCode.GOOGLE_SYNC_FAILED
                        )
                    }
                } else {
                    _loginStatus.value =
                            loginError(
                                    "Google Sign-In: unexpected credential type",
                                    LoginErrorCode.GOOGLE_UNEXPECTED_CREDENTIAL
                            )
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google Sign-In cancelled or unavailable", e)
                _loginStatus.value = loginError(
                        "Google Sign-In cancelled. Try again.",
                        LoginErrorCode.GOOGLE_CANCELLED
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _loginStatus.value = loginError(
                        "Google Sign-In failed. Please try again.",
                        LoginErrorCode.GOOGLE_FAILED
                )
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

    fun loginWithTestOfflineCredentials() {
        if (!BuildConfig.DEBUG) return
        
        viewModelScope.launch {
            _loginStatus.value = LoginResult.Success(UserEntity(name="Loading...", email="test", restaurantId=0, deviceId="")) // Trigger loading state logic
            
            // 1. Create fake JWT
            sessionManager.saveAuthToken("offline_test_token_" + System.currentTimeMillis())
            sessionManager.saveRestaurantId(12345L)
            sessionManager.setInitialSyncCompleted(true)

            // 2. Create Owner with PIN 1234
            val pinHash = authManager.hashPassword("1234")
            val testOwner = UserEntity(
                name = "Test Owner (1234)",
                email = "owner_test@khanabook.com",
                role = "owner",
                pinHash = pinHash,
                isActive = true,
                isSynced = true,
                restaurantId = 12345L,
                deviceId = sessionManager.getDeviceId()
            )
            
            userRepository.insertUser(testOwner)
            
            // 3. Clear active user to force Staff Selection screen
            sessionManager.clearLocalUserSession()
            userRepository.setCurrentUser(null)
            
            _loginStatus.value = LoginResult.Success(testOwner)
        }
    }

    sealed class LoginResult {
        object Loading : LoginResult()
        data class Success(val user: UserEntity) : LoginResult()
        data class Error(val message: String, val code: LoginErrorCode) : LoginResult()
    }

    enum class LoginErrorCode {
        LOCKED_OUT,
        INCORRECT_PASSWORD,
        ACCOUNT_INACTIVE,
        ACCOUNT_NOT_FOUND,
        GOOGLE_CONTEXT_MISSING,
        GOOGLE_SYNC_FAILED,
        GOOGLE_UNEXPECTED_CREDENTIAL,
        GOOGLE_CANCELLED,
        GOOGLE_FAILED
    }

    sealed class SignUpResult {
        object Loading : SignUpResult()
        object Success : SignUpResult()
        
        object OtpSent : SignUpResult()
        data class Error(val message: String) : SignUpResult()
    }

    sealed class ResetPasswordResult {
        object Success : ResetPasswordResult()
        
        object OtpSent : ResetPasswordResult()
        data class Error(val message: String) : ResetPasswordResult()
    }

    sealed class OtpVerificationResult {
        object Success : OtpVerificationResult()
        object OtpSent : OtpVerificationResult()
        data class Error(val message: String) : OtpVerificationResult()
    }
}
