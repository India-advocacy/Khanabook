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
            val deviceId = sessionManager.getDeviceId()
            val request = com.khanabook.lite.pos.data.remote.api.LoginRequest(phoneNumber, passwordPlain, deviceId)
            
            val response = api.login(request)
            val loginId = response.loginId?.takeIf { it.isNotBlank() } ?: phoneNumber

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveRestaurantId(response.restaurantId)

            var localUser = userDao.getUserByEmail(loginId)
            if (localUser == null) {
                localUser = UserEntity(
                    name = response.userName,
                    email = loginId,
                    passwordHash = localPasswordHash,
                    whatsappNumber = response.whatsappNumber ?: phoneNumber,
                    restaurantId = response.restaurantId,
                    deviceId = deviceId,
                    isActive = true,
                    isSynced = true,
                    createdAt = System.currentTimeMillis()
                )
                val id = userDao.insertUser(localUser)
                localUser = localUser.copy(id = id)
            } else {
                localUser = localUser.copy(
                    name = response.userName,
                    email = loginId,
                    whatsappNumber = response.whatsappNumber ?: localUser.whatsappNumber,
                    restaurantId = response.restaurantId,
                    isSynced = true
                )
                val id = userDao.insertUser(localUser)
                localUser = localUser.copy(id = id)
            }

            setCurrentUser(localUser)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remoteSignup(name: String, phoneNumber: String, passwordPlain: String, localPasswordHash: String): Result<UserEntity> {
        return try {
            val deviceId = sessionManager.getDeviceId()
            val request = com.khanabook.lite.pos.data.remote.api.SignupRequest(phoneNumber, name, passwordPlain, deviceId)
            
            val response = api.signup(request)
            val loginId = response.loginId?.takeIf { it.isNotBlank() } ?: phoneNumber

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveRestaurantId(response.restaurantId)

            var localUser = userDao.getUserByEmail(loginId)
            if (localUser == null) {
                localUser = UserEntity(
                    name = name,
                    email = loginId,
                    passwordHash = localPasswordHash,
                    whatsappNumber = response.whatsappNumber ?: phoneNumber,
                    restaurantId = response.restaurantId,
                    deviceId = deviceId,
                    isActive = true,
                    isSynced = true,
                    createdAt = System.currentTimeMillis()
                )
                val id = userDao.insertUser(localUser)
                localUser = localUser.copy(id = id)
            } else {
                localUser = localUser.copy(
                    name = response.userName,
                    email = loginId,
                    whatsappNumber = response.whatsappNumber ?: localUser.whatsappNumber,
                    restaurantId = response.restaurantId,
                    isSynced = true
                )
                val id = userDao.insertUser(localUser)
                localUser = localUser.copy(id = id)
            }

            setCurrentUser(localUser)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remoteGoogleLogin(idToken: String): Result<UserEntity> {
        return try {
            val deviceId = sessionManager.getDeviceId()
            val request = com.khanabook.lite.pos.data.remote.api.GoogleLoginRequest(idToken, deviceId)
            val response = api.loginWithGoogle(request)
            val loginId =
                response.loginId?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Auth response missing login identifier")

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveRestaurantId(response.restaurantId)

            var localUser = userDao.getUserByEmail(loginId)
            if (localUser == null) {
                localUser = UserEntity(
                    name = response.userName,
                    email = loginId,
                    passwordHash = "GOOGLE_AUTH",
                    whatsappNumber = response.whatsappNumber,
                    restaurantId = response.restaurantId,
                    deviceId = deviceId,
                    isActive = true,
                    isSynced = true,
                    createdAt = System.currentTimeMillis()
                )
                val id = userDao.insertUser(localUser)
                localUser = localUser.copy(id = id)
            } else {
                localUser = localUser.copy(
                    name = response.userName,
                    email = loginId,
                    whatsappNumber = response.whatsappNumber ?: localUser.whatsappNumber,
                    restaurantId = response.restaurantId,
                    isSynced = true
                )
                val id = userDao.insertUser(localUser)
                localUser = localUser.copy(id = id)
            }

            setCurrentUser(localUser)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadPersistedUser() {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val activeUserId = sessionManager.getActiveUserId()

        if (activeUserId != null) {
            val user = userDao.getUserById(activeUserId)
            _currentUser.value = user
            if (user != null) {
                prefs.edit().putString(KEY_USER_EMAIL, user.email).apply()
            }
        } else if (email != null) {
            val user = userDao.getUserByEmail(email)
            _currentUser.value = user
            user?.let { sessionManager.saveActiveUserId(it.id); sessionManager.saveActiveUserRole(it.role) }
        }
    }

    fun setCurrentUser(user: UserEntity?) {
        _currentUser.value = user
        if (user != null) {
            prefs.edit().putString(KEY_USER_EMAIL, user.email).apply()
            sessionManager.saveActiveUserId(user.id)
            sessionManager.saveActiveUserRole(user.role)
            triggerBackgroundSync()
        } else {
            prefs.edit().remove(KEY_USER_EMAIL).apply()
            sessionManager.clearLocalUserSession()
        }
    }

    suspend fun insertUser(user: UserEntity): Long {
        val enriched =
                user.copy(
                        restaurantId = sessionManager.getRestaurantId(),
                        deviceId = sessionManager.getDeviceId(),
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

    suspend fun updatePasswordHash(userId: Long, newHash: String) {
        userDao.updatePasswordHash(userId, newHash, System.currentTimeMillis())
        triggerBackgroundSync()
    }

    suspend fun remoteResetPassword(phoneNumber: String, newPasswordPlain: String) {
        val request = com.khanabook.lite.pos.data.remote.ResetPasswordRequest(phoneNumber, newPasswordPlain)
        api.resetPassword(request)
    }

    suspend fun checkUserExistsRemotely(phoneNumber: String): Boolean {
        return try {
            api.checkUser(phoneNumber)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateWhatsappNumber(userId: Long, newPhone: String) {
        userDao.updateWhatsappNumber(userId, newPhone, System.currentTimeMillis())
        triggerBackgroundSync()
    }

    suspend fun updateAccountDetails(userId: Long, newEmail: String, newPhone: String) {
        userDao.updateAccountDetails(userId, newEmail, newPhone, System.currentTimeMillis())
        triggerBackgroundSync()
    }

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    fun getActiveStaff(): Flow<List<UserEntity>> {
        return userDao.getActiveStaff()
    }

    suspend fun setActivationStatus(userId: Long, isActive: Boolean) {
        userDao.setActivationStatus(userId, isActive, System.currentTimeMillis())
        triggerBackgroundSync()
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.markDeleted(user.id, System.currentTimeMillis())
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
