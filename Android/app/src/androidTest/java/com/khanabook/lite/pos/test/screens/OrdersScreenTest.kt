package com.khanabook.lite.pos.test.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.BaseTest
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.robots.*
import com.khanabook.lite.pos.test.util.TestData
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class OrdersScreenTest : BaseTest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var ordersRobot: OrdersRobot
    private lateinit var orderDetailRobot: OrderDetailRobot
    private lateinit var newBillRobot: NewBillRobot
    private lateinit var checkoutRobot: CheckoutRobot
    private lateinit var loginRobot: LoginRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        ordersRobot = OrdersRobot(composeTestRule)
        newBillRobot = NewBillRobot(composeTestRule)
        checkoutRobot = CheckoutRobot(composeTestRule)
        loginRobot = LoginRobot(composeTestRule)
        
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
    }

    private fun createTestOrder() {
        homeRobot.tapNewBill().waitForMenuToLoad()
        newBillRobot.addItemToCart(TestData.MenuItems.BURGER)
        mockApiServer.enqueueBillCreateSuccess()
        newBillRobot.tapCheckout().completePayment(CheckoutRobot.PaymentMethod.CASH)
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Order Created")).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }

    @Test
    fun TC-LAYOUT-009_OrdersScreen_LayoutValid() {
        homeRobot.tapOrdersTab()
        
        ordersRobot
            .waitForOrdersToLoad()
            .assertOrdersListNotEmpty()
    }

    @Test
    fun TC-LAYOUT-009_OrdersScreen_EmptyState() {
        mockApiServer.enqueueEmptyOrders()
        
        homeRobot.tapOrdersTab()
        
        ordersRobot
            .waitForOrdersToLoad()
            .assertEmptyStateShown()
    }

    @Test
    fun TC-NAV-004_OrdersScreen_NavigateToDetail() {
        createTestOrder()
        
        homeRobot.tapOrdersTab().waitForOrdersToLoad()
        
        ordersRobot
            .tapOrder(0)
            .assertOrderDetailsVisible()
            .assertTotalAmountVisible()
            .assertPrintButtonVisible()
    }

    @Test
    fun TC-NAV-004_OrdersScreen_BackFromDetail() {
        createTestOrder()
        
        homeRobot.tapOrdersTab().waitForOrdersToLoad()
        
        ordersRobot
            .tapOrder(0)
            .pressBack()
            .assertOrdersListNotEmpty()
    }

    @Test
    fun TC-JOURNEY-003_OrdersScreen_ViewAndUpdateStatus() {
        createTestOrder()
        
        homeRobot.tapOrdersTab().waitForOrdersToLoad()
        
        ordersRobot
            .tapOrder(0)
            .tapUpdateStatus()
            .selectNewStatus(TestData.OrderStatuses.COMPLETED)
            .confirmStatusUpdate()
        
        composeTestRule.onNode(
            hasText(TestData.OrderStatuses.COMPLETED, substring = true)
        ).assertIsDisplayed()
    }

    @Test
    fun TC-API-012_OrdersScreen_FilterByStatus() {
        mockApiServer.enqueueFilteredOrders(TestData.OrderStatuses.PENDING)
        
        homeRobot.tapOrdersTab()
        
        ordersRobot
            .waitForOrdersToLoad()
            .filterByStatus(OrdersRobot.OrderStatus.PENDING)
            .assertPendingOrdersVisible()
    }

    @Test
    fun TC-API-012_OrdersScreen_SearchOrder() {
        createTestOrder()
        
        homeRobot.tapOrdersTab().waitForOrdersToLoad()
        
        ordersRobot.searchOrder("ORD-001")
    }

    @Test
    fun TC-OFFLINE-001_OrdersScreen_OfflineMode() {
        disableNetwork()
        
        homeRobot.tapOrdersTab()
        
        ordersRobot
            .waitForOrdersToLoad()
            .assertOrdersListNotEmpty()
        
        enableNetwork()
    }
}
