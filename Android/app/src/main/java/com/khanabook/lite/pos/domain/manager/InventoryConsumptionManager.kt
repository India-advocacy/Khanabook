package com.khanabook.lite.pos.domain.manager

import com.khanabook.lite.pos.data.local.entity.BillItemEntity
import com.khanabook.lite.pos.data.local.entity.StockLogEntity
import com.khanabook.lite.pos.data.repository.InventoryRepository
import com.khanabook.lite.pos.data.repository.MenuRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryConsumptionManager @Inject constructor(
    private val menuRepository: MenuRepository,
    private val inventoryRepository: InventoryRepository
) {
    /**
     * Deducts stock directly from MenuItem or ItemVariant based on the items in a bill.
     */
    suspend fun consumeMaterialsForBill(items: List<BillItemEntity>) {
        for (item in items) {
            val delta = -item.quantity.toDouble()
            
            if (item.variantId != null) {
                // Deduct from variant
                menuRepository.updateVariantStock(item.variantId, delta)
                
                // Log the stock change
                inventoryRepository.insertStockLog(
                    StockLogEntity(
                        menuItemId = item.menuItemId ?: 0,
                        variantId = item.variantId,
                        delta = delta,
                        reason = "Sale (Bill #${item.billId})",
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else if (item.menuItemId != null) {
                // Deduct from base item
                menuRepository.updateStock(item.menuItemId, delta)
                
                // Log the stock change
                inventoryRepository.insertStockLog(
                    StockLogEntity(
                        menuItemId = item.menuItemId,
                        delta = delta,
                        reason = "Sale (Bill #${item.billId})",
                        createdAt = System.currentTimeMillis().toString()
                    )
                )
            }
        }
    }
}
