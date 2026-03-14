package com.khanabook.lite.pos.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.khanabook.lite.pos.data.local.dao.CategoryDao
import com.khanabook.lite.pos.data.local.entity.CategoryEntity
import com.khanabook.lite.pos.domain.manager.SessionManager
import com.khanabook.lite.pos.worker.MasterSyncWorker
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
        private val categoryDao: CategoryDao,
        private val menuDao: com.khanabook.lite.pos.data.local.dao.MenuDao,
        private val sessionManager: SessionManager,
        private val workManager: WorkManager
) {
    suspend fun insertCategory(category: CategoryEntity): Long {
        val restaurantId = sessionManager.getRestaurantId()
        val deviceId = sessionManager.getDeviceId() ?: "default_device"

        val enrichedCategory =
                category.copy(
                        restaurantId = restaurantId,
                        deviceId = deviceId,
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                )
        val id = categoryDao.insertCategory(enrichedCategory)
        triggerBackgroundSync()
        return id
    }

    private fun triggerBackgroundSync() {
        val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncWorkRequest =
                OneTimeWorkRequestBuilder<MasterSyncWorker>().setConstraints(constraints).build()
        workManager.enqueueUniqueWork(
            "MasterSyncWorker_OneTime",
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategoriesFlow()
    }

    fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>> {
        return categoryDao.getActiveCategoriesFlow()
    }

    suspend fun toggleActive(id: Int, isActive: Boolean) {
        val current = categoryDao.getCategoryById(id) ?: return
        updateCategory(current.copy(isActive = isActive))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        val now = System.currentTimeMillis()
        categoryDao.markDeleted(category.id, now)
        val itemIds = menuDao.getItemIdsByCategory(category.id)
        menuDao.markItemsDeletedByCategory(category.id, now)
        itemIds.forEach { itemId ->
            menuDao.markVariantsDeletedByItem(itemId, now)
        }
        triggerBackgroundSync()
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val enrichedCategory =
                category.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        categoryDao.updateCategory(enrichedCategory)
        triggerBackgroundSync()
    }
}
