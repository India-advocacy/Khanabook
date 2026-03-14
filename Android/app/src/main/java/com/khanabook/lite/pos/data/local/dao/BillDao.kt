package com.khanabook.lite.pos.data.local.dao

import androidx.room.*
import com.khanabook.lite.pos.data.local.entity.BillEntity
import com.khanabook.lite.pos.data.local.entity.BillItemEntity
import com.khanabook.lite.pos.data.local.entity.BillPaymentEntity
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillPayments(payments: List<BillPaymentEntity>)

    @Query("SELECT * FROM bills WHERE id = :id") suspend fun getBillById(id: Int): BillEntity?

    @Query("SELECT * FROM bills WHERE lifetime_order_id = :id")
    suspend fun getBillByLifetimeId(id: Int): BillEntity?

    @Query(
            "SELECT * FROM bills WHERE daily_order_display = :displayId AND created_at LIKE :datePrefix || '%'"
    )
    suspend fun getBillByDailyIdAndDate(displayId: String, datePrefix: String): BillEntity?

    @Query(
            "SELECT * FROM bills WHERE daily_order_id = :dailyId AND created_at LIKE :datePrefix || '%'"
    )
    suspend fun getBillByDailyIntIdAndDate(dailyId: Int, datePrefix: String): BillEntity?

    @Query("SELECT * FROM bills WHERE order_status = 'draft'")
    fun getDraftBills(): Flow<List<BillEntity>>

    @Query("UPDATE bills SET order_status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: Int, status: String)

    @Query("UPDATE bills SET payment_mode = :mode WHERE id = :id")
    suspend fun updatePaymentMode(id: Int, mode: String)

    @Query("UPDATE bills SET payment_status = :status WHERE id = :id")
    suspend fun updatePaymentStatus(id: Int, status: String)

    @Query(
            "SELECT * FROM bills WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC"
    )
    fun getBillsByDateRange(startDate: String, endDate: String): Flow<List<BillEntity>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillWithItemsById(id: Int): BillWithItems?

    @Transaction
    @Query("SELECT * FROM bills WHERE lifetime_order_id = :id")
    suspend fun getBillWithItemsByLifetimeId(id: Int): BillWithItems?

    @Transaction
    suspend fun insertFullBill(
            bill: BillEntity,
            items: List<BillItemEntity>,
            payments: List<BillPaymentEntity>
    ) {
        val billId = insertBill(bill).toInt()
        val itemsWithId = items.map { it.copy(billId = billId) }
        val paymentsWithId = payments.map { it.copy(billId = billId) }
        insertBillItems(itemsWithId)
        insertBillPayments(paymentsWithId)
    }

    // --- NEW CLOUD SYNC OPERATIONS ---

    @Query("SELECT * FROM bills WHERE is_synced = 0")
    suspend fun getUnsyncedBills(): List<BillEntity>

    @Query("UPDATE bills SET is_synced = 1 WHERE id IN (:billIds)")
    suspend fun markBillsAsSynced(billIds: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncedBills(bills: List<BillEntity>)

    @Query("SELECT COUNT(*) FROM bills WHERE is_synced = 0")
    fun getUnsyncedCount(): Flow<Int>

    // --- BILL ITEMS SYNC ---
    @Query("SELECT * FROM bill_items WHERE is_synced = 0")
    suspend fun getUnsyncedBillItems(): List<BillItemEntity>

    @Query("UPDATE bill_items SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markBillItemsAsSynced(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncedBillItems(items: List<BillItemEntity>)

    // --- BILL PAYMENTS SYNC ---
    @Query("SELECT * FROM bill_payments WHERE is_synced = 0")
    suspend fun getUnsyncedBillPayments(): List<BillPaymentEntity>

    @Query("UPDATE bill_payments SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markBillPaymentsAsSynced(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncedBillPayments(payments: List<BillPaymentEntity>)
}
