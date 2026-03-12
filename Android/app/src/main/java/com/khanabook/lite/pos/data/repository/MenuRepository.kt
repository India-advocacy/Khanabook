package com.khanabook.lite.pos.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.khanabook.lite.pos.data.local.dao.MenuDao
import com.khanabook.lite.pos.data.local.entity.ItemVariantEntity
import com.khanabook.lite.pos.data.local.entity.MenuItemEntity
import com.khanabook.lite.pos.data.local.relation.MenuWithVariants
import com.khanabook.lite.pos.domain.manager.SessionManager
import com.khanabook.lite.pos.worker.MasterSyncWorker
import kotlinx.coroutines.flow.Flow

class MenuRepository(
        private val menuDao: MenuDao,
        private val sessionManager: SessionManager,
        private val workManager: WorkManager
) {

    suspend fun insertItem(item: MenuItemEntity): Long {
        val enriched =
                item.copy(
                        restaurantId = sessionManager.getRestaurantId(),
                        deviceId = sessionManager.getDeviceId() ?: "default_device",
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                )
        val id = menuDao.insertItem(enriched)
        triggerBackgroundSync()
        return id
    }

    suspend fun updateItem(item: MenuItemEntity) {
        val enriched = item.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        menuDao.updateItem(enriched)
        triggerBackgroundSync()
    }

    suspend fun getItemById(id: Int): MenuItemEntity? {
        return menuDao.getItemById(id)
    }

    suspend fun getItemOnce(id: Int): MenuItemEntity? {
        return menuDao.getItemById(id)
    }

    suspend fun getItemByName(name: String): MenuItemEntity? {
        return menuDao.getItemByName(name)
    }

    suspend fun getAllMenuItemsOnce(): List<MenuItemEntity> {
        return menuDao.getAllMenuItemsOnce()
    }

    suspend fun getAllVariantsOnce(): List<ItemVariantEntity> {
        return menuDao.getAllVariantsOnce()
    }

    suspend fun updateStock(id: Int, delta: Double) {
        menuDao.updateStock(id, delta)
        triggerBackgroundSync()
    }

    fun getItemsByCategoryFlow(categoryId: Int): Flow<List<MenuItemEntity>> {
        return menuDao.getItemsByCategoryFlow(categoryId)
    }

    fun getAllItemsFlow(): Flow<List<MenuItemEntity>> {
        return menuDao.getAllItemsFlow()
    }

    fun getMenuWithVariantsByCategoryFlow(categoryId: Int): Flow<List<MenuWithVariants>> {
        return menuDao.getMenuWithVariantsByCategoryFlow(categoryId)
    }

    fun searchItems(query: String): Flow<List<MenuItemEntity>> {
        return menuDao.searchItems("%$query%")
    }

    suspend fun toggleItemAvailability(id: Int, isAvailable: Boolean) {
        menuDao.toggleItemAvailability(id, isAvailable)
        triggerBackgroundSync()
    }

    suspend fun deleteItem(item: MenuItemEntity) {
        menuDao.deleteItem(item)
        triggerBackgroundSync()
    }

    suspend fun insertVariant(variant: ItemVariantEntity): Long {
        val enriched =
                variant.copy(
                        restaurantId = sessionManager.getRestaurantId(),
                        deviceId = sessionManager.getDeviceId() ?: "default_device",
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                )
        val id = menuDao.insertVariant(enriched)
        triggerBackgroundSync()
        return id
    }

    suspend fun updateVariant(variant: ItemVariantEntity) {
        val enriched = variant.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        menuDao.updateVariant(enriched)
        triggerBackgroundSync()
    }

    suspend fun updateVariantStock(id: Int, delta: Double) {
        menuDao.updateVariantStock(id, delta)
        triggerBackgroundSync()
    }

    suspend fun deleteVariant(variant: ItemVariantEntity) {
        menuDao.deleteVariant(variant)
        triggerBackgroundSync()
    }

    fun getVariantsForItemFlow(itemId: Int): Flow<List<ItemVariantEntity>> {
        return menuDao.getVariantsForItemFlow(itemId)
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
}
