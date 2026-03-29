package com.khanabook.lite.pos.test

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.robots.*
import com.khanabook.lite.pos.test.util.TestData
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class OfflineTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockApiServer: MockApiServer
    private lateinit var uiDevice: UiDevice
    private lateinit var loginRobot: LoginRobot
    private lateinit var homeRobot: HomeRobot
    private lateinit var ordersRobot: OrdersRobot
    private lateinit var newBillRobot: NewBillRobot

    @Before
    fun setUp() {
        hiltRule.inject()
        mockApiServer = MockApiServer()
        mockApiServer.start()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        loginRobot = LoginRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
        ordersRobot = OrdersRobot(composeTestRule)
        newBillRobot = NewBillRobot(composeTestRule)
    }

    @After
    fun tearDown() {
        mockApiServer.shutdown()
        enableNetwork()
    }

    private fun disableNetwork() {
        uiDevice.executeShellCommand("svc wifi disable")
        uiDevice.executeShellCommand("svc data disable")
    }

    private fun enableNetwork() {
        uiDevice.executeShellCommand("svc wifi enable")
        uiDevice.executeShellCommand("svc data enable")
    }

    @Test
    fun TC-OFFLINE-001_Login_FailsGracefully_Offline() {
        disableNetwork()
        
        loginRobot
            .enterCredentials()
            .tapLogin()
            .assertErrorMessageVisible()
    }

    @Test
    fun TC-OFFLINE-001_HomeScreen_CachedData_Offline() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        disableNetwork()
        
        homeRobot
            .assertDashboardVisible()
            .assertMetricsCardsVisible()
    }

    @Test
    fun TC-OFFLINE-001_NewBillScreen_CachedMenu_Offline() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        disableNetwork()
        
        homeRobot.tapNewBill().waitForMenuToLoad()
        
        newBillRobot
            .addItemToCart(TestData.MenuItems.BURGER)
            .assertItemInCart(TestData.MenuItems.BURGER)
    }

    @Test
    fun TC-OFFLINE-001_OrdersScreen_CachedData_Offline() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        homeRobot.tapOrdersTab().waitForOrdersToLoad()
        
        disableNetwork()
        
        ordersRobot.assertOrdersListNotEmpty()
    }

    @Test
    fun TC-OFFLINE-002_NetworkDropsMidRequest_HandledGracefully() {
        mockApiServer.enqueueLoginSuccess()
        
        loginRobot.enterCredentials()
        loginRobot.tapLogin()
        
        disableNetwork()
        
        loginRobot.assertErrorMessageVisible()
        
        enableNetwork()
    }

    @Test
    fun TC-OFFLINE-001_Reconnect_RefreshesData() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        disableNetwork()
        
        homeRobot.assertDashboardVisible()
        
        enableNetwork()
        
        homeRobot
            .pullToRefresh()
            .assertMetricsCardsVisible()
    }
}
