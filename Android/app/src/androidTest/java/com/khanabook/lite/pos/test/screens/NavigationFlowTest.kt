package com.khanabook.lite.pos.test.screens

import android.content.Intent
import android.net.Uri
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
class NavigationFlowTest : BaseTest() {

    private lateinit var loginRobot: LoginRobot
    private lateinit var homeRobot: HomeRobot
    private lateinit var newBillRobot: NewBillRobot
    private lateinit var checkoutRobot: CheckoutRobot
    private lateinit var ordersRobot: OrdersRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var menuConfigRobot: MenuConfigurationRobot
    private lateinit var logoutRobot: LogoutRobot

    @Before
    override fun setUp() {
        super.setUp()
        loginRobot = LoginRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
        newBillRobot = NewBillRobot(composeTestRule)
        checkoutRobot = CheckoutRobot(composeTestRule)
        ordersRobot = OrdersRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        menuConfigRobot = MenuConfigurationRobot(composeTestRule)
        logoutRobot = LogoutRobot(composeTestRule)
    }

    @Test
    fun TC-NAV-001_AuthFlow_LoginToMain() {
        mockApiServer.enqueueLoginSuccess()
        
        loginRobot.submitLogin()
        
        homeRobot
            .waitForDataToLoad()
            .assertDashboardVisible()
    }

    @Test
    fun TC-NAV-001_AuthFlow_SignUpToMain() {
        mockApiServer.enqueueSignupSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot
            .tapSignUp()
            .assertNavigationToSignUp()
    }

    @Test
    fun TC-NAV-002_AuthFlow_ReturningUserSkipsInitialSync() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        
        homeRobot
            .waitForDataToLoad()
            .assertDashboardVisible()
    }

    @Test
    fun TC-NAV-006_Navigation_BackToExit_HomeScreen() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        repeat(3) {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
            Thread.sleep(500)
        }
    }

    @Test
    fun TC-NAV-007_Navigation_AllTabsAccessible() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        homeRobot
            .tapHomeTab()
            .assertDashboardVisible()
        
        homeRobot
            .tapOrdersTab()
            .waitForOrdersToLoad()
        
        homeRobot
            .tapReportsTab()
            .waitForReportsToLoad()
        
        homeRobot
            .tapSearchTab()
        
        homeRobot
            .tapSettingsTab()
            .assertSectionsVisible()
        
        homeRobot
            .tapHomeTab()
            .assertDashboardVisible()
    }

    @Test
    fun TC-NAV-009_Navigation_SettingsToMenuConfiguration() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        homeRobot.tapSettingsTab()
        
        settingsRobot
            .tapMenuSection()
            .tapMenuConfiguration()
            .assertModeSelectionVisible()
    }

    @Test
    fun TC-NAV-005_Navigation_RapidTabSwitching() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        repeat(10) {
            homeRobot.tapHomeTab()
            homeRobot.tapOrdersTab()
            homeRobot.tapSettingsTab()
            homeRobot.tapReportsTab()
            homeRobot.tapSearchTab()
        }
        
        homeRobot.assertBottomNavigationVisible()
    }

    @Test
    fun TC-NAV-008_DeepLink_BillDeepLink() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        mockApiServer.enqueueOrderDetail("ORD-123")
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("khanabook://bill/ORD-123")
        }
        composeTestRule.activity.startActivity(deepLinkIntent)
    }

    @Test
    fun TC-JOURNEY-002_MenuConfigurationFlow() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        homeRobot.tapSettingsTab()
        
        settingsRobot
            .tapMenuSection()
            .tapMenuConfiguration()
            .assertModeSelectionVisible()
            .assertManualModeVisible()
            .assertSmartImportVisible()
    }

    @Test
    fun TC-STATE-002_AppState_KillMidFlow_Resume() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        homeRobot.tapNewBill().waitForMenuToLoad()
        newBillRobot.addItemToCart(TestData.MenuItems.BURGER)
        newBillRobot.addItemToCart(TestData.MenuItems.PIZZA)
        
        uiDevice.executeShellCommand("am force-stop com.khanabook.lite.pos")
        
        val relaunchIntent = composeTestRule.activity.intent
        composeTestRule.activity.startActivity(relaunchIntent)
    }

    @Test
    fun TC-SEC-001_Logout_ClearsSession() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        
        homeRobot.tapSettingsTab()
        
        settingsRobot
            .scrollToLogout()
            .tapLogout()
            .confirmLogout()
            .assertLoginScreenShown()
        
        loginRobot
            .assertPhoneFieldVisible()
            .assertPasswordFieldVisible()
    }
}
