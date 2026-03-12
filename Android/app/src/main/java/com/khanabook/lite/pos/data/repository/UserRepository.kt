package com.khanabook.lite.pos.data.repository

import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.khanabook.lite.pos.data.local.dao.UserDao
import com.khanabook.lite.pos.data.local.entity.UserEntity
import com.khanabook.lite.pos.domain.manager.SessionManager
import com.khanabook.lite.pos.worker.MasterSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val KEY_USER_EMAIL = "logged_in_user_email"

class UserRepository(
        private val userDao: UserDao,
        private val prefs: SharedPreferences,
        private val sessionManager: SessionManager,
        private val workManager: WorkManager,
        private val api: com.khanabook.lite.pos.data.remote.api.KhanaBookApi
) {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    suspend fun remoteLogin(phoneNumber: String, passwordPlain: String, localPasswordHash: String): Result<UserEntity> {
        return try {
            val deviceId = sessionManager.getDeviceId() ?: "unknown_device"
            val request = com.khanabook.lite.pos.data.remote.api.LoginRequest(phoneNumber, passwordPlain, deviceId)
            
            val response = api.login(request)

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveRestaurantId(response.restaurantId)

            var localUser = userDao.getUserByEmail(phoneNumber)
            if (localUser == null) {
                localUser = UserEntity(
                    name = response.userName,
                    email = phoneNumber,
                    passwordHash = localPasswordHash,
                    restaurantId = response.restaurantId,
                    deviceId = deviceId,
                    isActive = true,
                    isSynced = true,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                userDao.insertUser(localUser)
            } else {
                localUser = localUser.copy(
                    restaurantId = response.restaurantId,
                    isSynced = true
                )
                userDao.insertUser(localUser)
            }

            setCurrentUser(localUser)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remoteSignup(name: String, phoneNumber: String, passwordPlain: String, localPasswordHash: String): Result<UserEntity> {
        return try {
            val deviceId = sessionManager.getDeviceId() ?: "unknown_device"
            val request = com.khanabook.lite.pos.data.remote.api.SignupRequest(phoneNumber, name, passwordPlain, deviceId)
            
            val response = api.signup(request)

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveRestaurantId(response.restaurantId)

            var localUser = userDao.getUserByEmail(phoneNumber)
            if (localUser == null) {
                localUser = UserEntity(
                    name = name,
                    email = phoneNumber,
                    passwordHash = localPasswordHash,
                    restaurantId = response.restaurantId,
                    deviceId = deviceId,
                    isActive = true,
                    isSynced = true,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                userDao.insertUser(localUser)
            } else {
                localUser = localUser.copy(
                    restaurantId = response.restaurantId,
                    isSynced = true
                )
                userDao.insertUser(localUser)
            }

            setCurrentUser(localUser)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remoteGoogleLogin(idToken: String): Result<UserEntity> {
        return try {
            val deviceId = sessionManager.getDeviceId() ?: "unknown_device"
            val request = com.khanabook.lite.pos.data.remote.api.GoogleLoginRequest(idToken, deviceId)
            val response = api.loginWithGoogle(request)

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveRestaurantId(response.restaurantId)

            // In Google login case, userName from token payload is likely the email
            val email = response.userName 
            var localUser = userDao.getUserByEmail(email)
            if (localUser == null) {
                localUser = UserEntity(
                    name = response.userName,
                    email = email,
                    passwordHash = "GOOGLE_AUTH",
                    restaurantId = response.restaurantId,
                    deviceId = deviceId,
                    isActive = true,
                    isSynced = true,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                userDao.insertUser(localUser)
            } else {
                localUser = localUser.copy(
                    restaurantId = response.restaurantId,
                    isSynced = true
                )
                userDao.insertUser(localUser)
            }

            setCurrentUser(localUser)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadPersistedUser() {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        if (email != null) {
            val user = userDao.getUserByEmail(email)
            _currentUser.value = user
        }
    }

    fun setCurrentUser(user: UserEntity?) {
        _currentUser.value = user
        if (user != null) {
            prefs.edit().putString(KEY_USER_EMAIL, user.email).apply()
            triggerBackgroundSync()
        } else {
            prefs.edit().remove(KEY_USER_EMAIL).apply()
        }
    }

    suspend fun insertUser(user: UserEntity): Long {
        val enriched =
                user.copy(
                        restaurantId = sessionManager.getRestaurantId(),
                        deviceId = sessionManager.getDeviceId() ?: "default_device",
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                )
        val id = userDao.insertUser(enriched)
        triggerBackgroundSync()
        return id
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    suspend fun updatePasswordHash(userId: Int, newHash: String) {
        userDao.updatePasswordHash(userId, newHash)
        triggerBackgroundSync()
    }

    suspend fun updateAdminPhoneNumber(newPhone: String) {
        userDao.updateAdminPhoneNumber(newPhone)
        triggerBackgroundSync()
    }

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    suspend fun setActivationStatus(userId: Int, isActive: Boolean) {
        userDao.setActivationStatus(userId, isActive)
        triggerBackgroundSync()
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
        triggerBackgroundSync()
    }

    private fun triggerBackgroundSync() {
        val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncWorkRequest =
                OneTimeWorkRequestBuilder<MasterSyncWorker>().setConstraints(constraints).build()
        workManager.enqueueUniqueWork(
            "ImmediateSync",
            ExistingWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
