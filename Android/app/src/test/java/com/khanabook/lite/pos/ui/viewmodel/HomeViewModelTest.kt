package com.khanabook.lite.pos.ui.viewmodel

import com.khanabook.lite.pos.data.local.entity.BillEntity
import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import com.khanabook.lite.pos.data.repository.BillRepository
import com.khanabook.lite.pos.domain.util.ConnectionStatus
import com.khanabook.lite.pos.domain.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @Mock
    private lateinit var billRepository: BillRepository

    @Mock
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        whenever(networkMonitor.status).thenReturn(flowOf(ConnectionStatus.Available))
        whenever(billRepository.getUnsyncedCount()).thenReturn(flowOf(0))
        
        val profile = RestaurantProfileEntity(
            id = 1,
            shopName = "Test Shop",
            timezone = "Asia/Kolkata"
        )
        whenever(billRepository.getProfileFlow()).thenReturn(flowOf(profile))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `todayStats should count all orders including cancelled ones`() = runTest {
        val bills = listOf(
            BillEntity(id = 1, dailyOrderId = 1, dailyOrderDisplay = "B1", lifetimeOrderId = 1, subtotal = "100.0", totalAmount = "100.0", paymentMode = "cash", paymentStatus = "success", orderStatus = "completed"),
            BillEntity(id = 2, dailyOrderId = 2, dailyOrderDisplay = "B2", lifetimeOrderId = 2, subtotal = "50.0", totalAmount = "50.0", paymentMode = "cash", paymentStatus = "failed", orderStatus = "cancelled")
        )
        
        whenever(billRepository.getBillsByDateRange(any<Long>(), any<Long>())).thenReturn(flowOf(bills))
        
        viewModel = HomeViewModel(billRepository, networkMonitor)
        
        // Advance time for StateFlow to collect
        testDispatcher.scheduler.advanceUntilIdle()
        
        val stats = viewModel.todayStats.value
        assertEquals(2, stats.orderCount) // Should be 2, not 1
        assertEquals(100.0, stats.revenue, 0.001) // Revenue only from completed
    }
}
