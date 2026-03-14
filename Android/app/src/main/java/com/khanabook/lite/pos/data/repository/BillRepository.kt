package com.khanabook.lite.pos.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.khanabook.lite.pos.data.local.dao.BillDao
import com.khanabook.lite.pos.data.local.entity.BillEntity
import com.khanabook.lite.pos.data.local.entity.BillItemEntity
import com.khanabook.lite.pos.data.local.entity.BillPaymentEntity
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import com.khanabook.lite.pos.domain.manager.InventoryConsumptionManager
import com.khanabook.lite.pos.worker.MasterSyncWorker
import kotlinx.coroutines.flow.Flow

class BillRepository(
        private val billDao: BillDao,
        private val inventoryConsumptionManager: InventoryConsumptionManager? = null,
        private val workManager: WorkManager
) {

    suspend fun insertFullBill(
            bill: BillEntity,
            items: List<BillItemEntity>,
            payments: List<BillPaymentEntity>
    ) {
        billDao.insertFullBill(bill, items, payments)
        
        if (bill.orderStatus.equals("completed", ignoreCase = true) ||
            bill.orderStatus.equals("paid", ignoreCase = true)
        ) {
            inventoryConsumptionManager?.consumeMaterialsForBill(items)
        }
        
        triggerBackgroundSync()
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

    suspend fun getBillById(id: Int): BillEntity? {
        return billDao.getBillById(id)
    }

    suspend fun getBillWithItemsById(id: Int): BillWithItems? {
        return billDao.getBillWithItemsById(id)
    }

    suspend fun getBillWithItemsByLifetimeId(id: Int): BillWithItems? {
        return billDao.getBillWithItemsByLifetimeId(id)
    }

    suspend fun getBillByDailyIdAndDate(displayId: String, datePrefix: String): BillEntity? {
        return billDao.getBillByDailyIdAndDate(displayId, datePrefix)
    }

    suspend fun getBillByDailyIntIdAndDate(dailyId: Int, datePrefix: String): BillEntity? {
        return billDao.getBillByDailyIntIdAndDate(dailyId, datePrefix)
    }

    fun getDraftBills(): Flow<List<BillEntity>> {
        return billDao.getDraftBills()
    }

    suspend fun updateOrderStatus(id: Int, status: String) {
        val current = billDao.getBillById(id) ?: return
        billDao.updateBill(
            current.copy(
                orderStatus = status,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (status.equals("completed", ignoreCase = true) ||
                        status.equals("paid", ignoreCase = true)
        ) {
            val billWithItems = billDao.getBillWithItemsById(id)
            billWithItems?.let { inventoryConsumptionManager?.consumeMaterialsForBill(it.items) }
        }
        triggerBackgroundSync()
    }

    suspend fun updatePaymentMode(id: Int, mode: String) {
        val current = billDao.getBillById(id) ?: return
        billDao.updateBill(
            current.copy(
                paymentMode = mode,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        triggerBackgroundSync()
    }

    suspend fun updatePaymentStatus(id: Int, status: String) {
        val current = billDao.getBillById(id) ?: return
        billDao.updateBill(
            current.copy(
                paymentStatus = status,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        triggerBackgroundSync()
    }

    fun getBillsByDateRange(startDate: String, endDate: String): Flow<List<BillEntity>> {
        return billDao.getBillsByDateRange(startDate, endDate)
    }

    fun getUnsyncedCount(): Flow<Int> {
        return billDao.getUnsyncedCount()
    }
}
