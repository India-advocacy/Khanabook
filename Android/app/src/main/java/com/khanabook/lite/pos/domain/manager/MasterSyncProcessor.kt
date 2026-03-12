package com.khanabook.lite.pos.domain.manager

import android.util.Log
import com.khanabook.lite.pos.data.local.dao.*
import com.khanabook.lite.pos.data.local.entity.*
import com.khanabook.lite.pos.data.remote.api.KhanaBookApi
import com.khanabook.lite.pos.data.remote.api.MasterSyncResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterSyncProcessor @Inject constructor(
    private val api: KhanaBookApi,
    private val billDao: BillDao,
    private val restaurantDao: RestaurantDao,
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val menuDao: MenuDao,
    private val inventoryDao: InventoryDao
) {

    suspend fun pushAll(): Boolean {
        return try {
            // 1. PUSH CONFIG (Users & Profiles)
            val unsyncedProfiles = restaurantDao.getUnsyncedRestaurantProfiles()
            if (unsyncedProfiles.isNotEmpty()) {
                unsyncedProfiles.chunked(50).forEach { batch ->
                    val syncedIds = api.pushRestaurantProfiles(batch)
                    restaurantDao.markRestaurantProfilesAsSynced(syncedIds)
                }
            }

            val unsyncedUsers = userDao.getUnsyncedUsers()
            if (unsyncedUsers.isNotEmpty()) {
                unsyncedUsers.chunked(50).forEach { batch ->
                    val syncedIds = api.pushUsers(batch)
                    userDao.markUsersAsSynced(syncedIds)
                }
            }

            // 2. PUSH MENU (Categories, Items, Variants)
            val unsyncedCategories = categoryDao.getUnsyncedCategories()
            if (unsyncedCategories.isNotEmpty()) {
                unsyncedCategories.chunked(50).forEach { batch ->
                    val syncedIds = api.pushCategories(batch)
                    categoryDao.markCategoriesAsSynced(syncedIds)
                }
            }

            val unsyncedMenuItems = menuDao.getUnsyncedMenuItems()
            if (unsyncedMenuItems.isNotEmpty()) {
                unsyncedMenuItems.chunked(50).forEach { batch ->
                    val syncedIds = api.pushMenuItems(batch)
                    menuDao.markMenuItemsAsSynced(syncedIds)
                }
            }

            val unsyncedVariants = menuDao.getUnsyncedItemVariants()
            if (unsyncedVariants.isNotEmpty()) {
                unsyncedVariants.chunked(50).forEach { batch ->
                    val syncedIds = api.pushItemVariants(batch)
                    menuDao.markItemVariantsAsSynced(syncedIds)
                }
            }

            // 3. PUSH INVENTORY
            val unsyncedStockLogs = inventoryDao.getUnsyncedStockLogs()
            if (unsyncedStockLogs.isNotEmpty()) {
                unsyncedStockLogs.chunked(50).forEach { batch ->
                    val syncedIds = api.pushStockLogs(batch)
                    inventoryDao.markStockLogsAsSynced(syncedIds)
                }
            }

            // 4. SYNC BILLS (Push only part)
            val unsyncedBills = billDao.getUnsyncedBills()
            if (unsyncedBills.isNotEmpty()) {
                unsyncedBills.chunked(50).forEach { batch ->
                    val syncedIds = api.pushBills(batch)
                    billDao.markBillsAsSynced(syncedIds)
                }
            }

            val unsyncedBillItems = billDao.getUnsyncedBillItems()
            if (unsyncedBillItems.isNotEmpty()) {
                unsyncedBillItems.chunked(50).forEach { batch ->
                    val syncedIds = api.pushBillItems(batch)
                    billDao.markBillItemsAsSynced(syncedIds)
                }
            }

            val unsyncedBillPayments = billDao.getUnsyncedBillPayments()
            if (unsyncedBillPayments.isNotEmpty()) {
                unsyncedBillPayments.chunked(50).forEach { batch ->
                    val syncedIds = api.pushBillPayments(batch)
                    billDao.markBillPaymentsAsSynced(syncedIds)
                }
            }

            true
        } catch (e: Exception) {
            Log.e("MasterSyncProcessor", "Push failed", e)
            false
        }
    }

    suspend fun insertMasterData(masterData: MasterSyncResponse) {
        if (masterData.profiles.isNotEmpty()) {
            val currentLocalProfile = restaurantDao.getProfile()
            restaurantDao.insertSyncedRestaurantProfiles(
                masterData.profiles.map {
                    it.copy(
                        id = 1,
                        isSynced = true,
                        // CRITICAL: Keep local counters to avoid multi-device ID conflicts
                        dailyOrderCounter = currentLocalProfile?.dailyOrderCounter ?: it.dailyOrderCounter,
                        lifetimeOrderCounter = currentLocalProfile?.lifetimeOrderCounter ?: it.lifetimeOrderCounter,
                        lastResetDate = currentLocalProfile?.lastResetDate ?: it.lastResetDate
                    )
                }
            )
        }
        if (masterData.users.isNotEmpty()) {
            userDao.insertSyncedUsers(masterData.users.map { it.copy(isSynced = true) })
        }
        if (masterData.categories.isNotEmpty()) {
            categoryDao.insertSyncedCategories(masterData.categories.map { it.copy(isSynced = true) })
        }
        if (masterData.menuItems.isNotEmpty()) {
            menuDao.insertSyncedMenuItems(masterData.menuItems.map { it.copy(isSynced = true) })
        }
        if (masterData.itemVariants.isNotEmpty()) {
            menuDao.insertSyncedItemVariants(masterData.itemVariants.map { it.copy(isSynced = true) })
        }
        if (masterData.stockLogs.isNotEmpty()) {
            inventoryDao.insertSyncedStockLogs(masterData.stockLogs.map { it.copy(isSynced = true) })
        }
        if (masterData.bills.isNotEmpty()) {
            billDao.insertSyncedBills(masterData.bills.map { it.copy(isSynced = true) })
        }
        if (masterData.billItems.isNotEmpty()) {
            billDao.insertSyncedBillItems(masterData.billItems.map { it.copy(isSynced = true) })
        }
        if (masterData.billPayments.isNotEmpty()) {
            billDao.insertSyncedBillPayments(masterData.billPayments.map { it.copy(isSynced = true) })
        }
    }
}
