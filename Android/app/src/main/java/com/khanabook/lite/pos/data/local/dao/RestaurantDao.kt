package com.khanabook.lite.pos.data.local.dao

import androidx.room.*
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: RestaurantProfileEntity)

    @Query("SELECT * FROM restaurant_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): RestaurantProfileEntity?

    @Query("SELECT * FROM restaurant_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<RestaurantProfileEntity?>

    @Query(
        "UPDATE restaurant_profile SET daily_order_counter = :counter, last_reset_date = :date, is_synced = 0, updated_at = :updatedAt WHERE id = 1"
    )
    suspend fun resetDailyCounter(counter: Int, date: String, updatedAt: Long)

    @Query(
        "UPDATE restaurant_profile SET daily_order_counter = daily_order_counter + 1, lifetime_order_counter = lifetime_order_counter + 1, is_synced = 0, updated_at = :updatedAt WHERE id = 1"
    )
    suspend fun incrementOrderCounters(updatedAt: Long)

    @Transaction
    suspend fun incrementAndGetCounters(): Pair<Int, Int> {
        val profile = getProfile() ?: throw Exception("Profile not found")
        // Server now handles date derivation, but local daily counter still needs a reset check.
        // For simplicity during transition, we'll use system date locally.
        val zoneId = java.time.ZoneId.of(profile.timezone ?: "Asia/Kolkata")
        val today = java.time.LocalDate.now(zoneId).toString()
        val isNewDay = profile.lastResetDate != today
        val now = System.currentTimeMillis()
        
        val nextDaily = if (isNewDay) 1 else profile.dailyOrderCounter + 1
        val nextLifetime = profile.lifetimeOrderCounter + 1
        
        if (isNewDay) {
            resetDailyCounter(nextDaily, today, now)
            // Still need to increment lifetime
            updateLifetimeCounter(nextLifetime, now)
        } else {
            incrementOrderCounters(now)
        }
        
        return Pair(nextDaily, nextLifetime)
    }

    @Transaction
    suspend fun updateCounters(daily: Int, lifetime: Int) {
        val current = getProfile() ?: return
        val zoneId = java.time.ZoneId.of(current.timezone ?: "Asia/Kolkata")
        val today = java.time.LocalDate.now(zoneId).toString()
        saveProfile(current.copy(
            dailyOrderCounter = daily,
            lifetimeOrderCounter = lifetime,
            lastResetDate = today,
            isSynced = true,
            updatedAt = System.currentTimeMillis()
        ))
    }

    @Query(
        "UPDATE restaurant_profile SET lifetime_order_counter = :counter, is_synced = 0, updated_at = :updatedAt WHERE id = 1"
    )
    suspend fun updateLifetimeCounter(counter: Int, updatedAt: Long)

    @Query("UPDATE restaurant_profile SET upi_qr_path = :path, is_synced = 0, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateUpiQrPath(path: String?, updatedAt: Long)

    @Query("UPDATE restaurant_profile SET logo_path = :path, is_synced = 0, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateLogoPath(path: String?, updatedAt: Long)

    @Query("SELECT * FROM restaurant_profile WHERE is_synced = 0")
    suspend fun getUnsyncedRestaurantProfiles(): List<RestaurantProfileEntity>

    @Query("UPDATE restaurant_profile SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markRestaurantProfilesAsSynced(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncedRestaurantProfiles(items: List<RestaurantProfileEntity>)
}
