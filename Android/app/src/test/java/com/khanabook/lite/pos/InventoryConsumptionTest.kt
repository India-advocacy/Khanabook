package com.khanabook.lite.pos

import com.khanabook.lite.pos.data.local.entity.BillItemEntity
import com.khanabook.lite.pos.data.local.entity.StockLogEntity
import com.khanabook.lite.pos.data.repository.InventoryRepository
import com.khanabook.lite.pos.data.repository.MenuRepository
import com.khanabook.lite.pos.domain.manager.InventoryConsumptionManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class InventoryConsumptionTest {

    @Mock
    private lateinit var menuRepository: MenuRepository

    @Mock
    private lateinit var inventoryRepository: InventoryRepository

    private lateinit var inventoryConsumptionManager: InventoryConsumptionManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        inventoryConsumptionManager = InventoryConsumptionManager(menuRepository, inventoryRepository)
    }

    @Test
    fun `consumeMaterialsForBill should deduct stock and log base item sale`() = runTest {
        val billItems = listOf(
            BillItemEntity(
                id = 1,
                billId = 42,
                menuItemId = 101,
                itemName = "Biryani",
                quantity = 3,
                price = "200.00",
                itemTotal = "600.00"
            )
        )

        inventoryConsumptionManager.consumeMaterialsForBill(billItems)

        verify(menuRepository).updateStock(101, "-3")

        val logCaptor = argumentCaptor<StockLogEntity>()
        verify(inventoryRepository).insertStockLog(logCaptor.capture())

        val log = logCaptor.firstValue
        org.junit.Assert.assertEquals(101, log.menuItemId)
        org.junit.Assert.assertEquals(null, log.variantId)
        org.junit.Assert.assertEquals("-3", log.delta)
        org.junit.Assert.assertEquals("Sale (Bill #42)", log.reason)
    }

    @Test
    fun `consumeMaterialsForBill should deduct variant stock and log variant sale`() = runTest {
        val billItems = listOf(
            BillItemEntity(
                id = 1,
                billId = 7,
                menuItemId = 101,
                itemName = "Tea",
                variantId = 501,
                variantName = "Large",
                quantity = 2,
                price = "30.00",
                itemTotal = "60.00"
            )
        )

        inventoryConsumptionManager.consumeMaterialsForBill(billItems)

        verify(menuRepository).updateVariantStock(501, "-2")

        val logCaptor = argumentCaptor<StockLogEntity>()
        verify(inventoryRepository).insertStockLog(logCaptor.capture())

        val log = logCaptor.firstValue
        org.junit.Assert.assertEquals(101, log.menuItemId)
        org.junit.Assert.assertEquals(501, log.variantId)
        org.junit.Assert.assertEquals("-2", log.delta)
        org.junit.Assert.assertEquals("Sale (Bill #7)", log.reason)
    }
}
