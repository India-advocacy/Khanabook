package com.khanabook.lite.pos.domain.manager

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PREFS_NAME = "session_prefs"
private const val KEY_LAST_INTERACTION = "last_interaction_time"
private const val SESSION_CHECK_INTERVAL_MS = 60_000L // check every 60 seconds

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var timeoutMinutes: Int
        get() = prefs.getInt("session_timeout_minutes", 30)
        set(value) {
            prefs.edit().putInt("session_timeout_minutes", value).apply()
        }

    private val _isSessionExpired = MutableStateFlow(false)
    val isSessionExpired: StateFlow<Boolean> = _isSessionExpired

    // Background scope for periodic session checks
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var sessionCheckJob: kotlinx.coroutines.Job? = null

    init {
        startPeriodicCheck()
    }

    /** Persist last interaction time so it survives process kills */
    private var lastInteractionTime: Long
        get() = prefs.getLong(KEY_LAST_INTERACTION, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_LAST_INTERACTION, value).apply()

    fun updateTimeout(minutes: Int) {
        timeoutMinutes = minutes
    }

    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (_isSessionExpired.value) {
            _isSessionExpired.value = false
        }
    }

    fun checkSession() {
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - lastInteractionTime
        val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)

        if (elapsedMinutes >= timeoutMinutes) {
            _isSessionExpired.value = true
        }
    }

    fun resetSession() {
        _isSessionExpired.value = false
        lastInteractionTime = System.currentTimeMillis()
    }

    /** Automatically checks session expiry in the background every minute */
    private fun startPeriodicCheck() {
        sessionCheckJob?.cancel()
        sessionCheckJob = scope.launch {
            while (currentCoroutineContext().isActive) {
                delay(SESSION_CHECK_INTERVAL_MS)
                checkSession()
            }
        }
    }

    // --- NEW CLOUD SYNC METADATA ---
    fun getAuthToken(): String? {
        return securePrefs.getString("auth_token", null)
    }

    fun saveAuthToken(token: String) {
        securePrefs.edit().putString("auth_token", token).apply()
    }

    fun getDeviceId(): String {
        synchronized(this) {
            var deviceId = prefs.getString("device_id", null)
            if (deviceId == null) {
                deviceId = java.util.UUID.randomUUID().toString()
                saveDeviceId(deviceId)
            }
            return deviceId!!
        }
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString("device_id", deviceId).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_sync_timestamp", 0L)
    }

    fun saveLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_sync_timestamp", timestamp).apply()
    }

    fun getRestaurantId(): Long {
        return prefs.getLong("restaurant_id", 0L)
    }

    fun saveRestaurantId(restaurantId: Long) {
        prefs.edit().putLong("restaurant_id", restaurantId).apply()
    }

    fun setInitialSyncCompleted(isCompleted: Boolean) {
        prefs.edit().putBoolean("initial_sync_completed", isCompleted).apply()
    }

    fun isInitialSyncCompleted(): Boolean {
        return prefs.getBoolean("initial_sync_completed", false)
    }

    fun clearSession() {
        sessionCheckJob?.cancel()
        prefs.edit().clear().apply()
        _isSessionExpired.value = true
    }
}
