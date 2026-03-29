package com.khanabook.lite.pos.test.screens

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.BaseTest
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.robots.HomeRobot
import com.khanabook.lite.pos.test.robots.LoginRobot
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class HomeScreenTest : BaseTest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var loginRobot: LoginRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        loginRobot = LoginRobot(composeTestRule)
        
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
    }

    @Test
    fun TC-LAYOUT-005_MainScreen_BottomNavigationVisible() {
        homeRobot
            .assertBottomNavigationVisible()
            .assertQuickActionsVisible()
    }

    @Test
    fun TC-LAYOUT-006_HomeScreen_DashboardCardsVisible() {
        homeRobot
            .assertDashboardVisible()
            .assertMetricsCardsVisible()
    }

    @Test
    fun TC-NAV-007_HomeScreen_TabNavigation_OrdersTab() {
        homeRobot.tapOrdersTab().waitForOrdersToLoad()
        
        composeTestRule.onNode(hasText("Orders")).assertIsDisplayed()
    }

    @Test
    fun TC-NAV-007_HomeScreen_TabNavigation_ReportsTab() {
        homeRobot.tapReportsTab().waitForReportsToLoad()
        
        composeTestRule.onNode(hasText("Reports")).assertIsDisplayed()
    }

    @Test
    fun TC-NAV-007_HomeScreen_TabNavigation_SettingsTab() {
        homeRobot.tapSettingsTab()
        
        composeTestRule.onNode(hasText("Settings")).assertIsDisplayed()
    }

    @Test
    fun TC-NAV-005_HomeScreen_RapidTabSwitching() {
        repeat(5) {
            homeRobot.tapOrdersTab()
            homeRobot.tapReportsTab()
            homeRobot.tapHomeTab()
            homeRobot.tapSettingsTab()
        }
        
        homeRobot.assertBottomNavigationVisible()
    }

    @Test
    fun TC-STATE-001_HomeScreen_StatePreserved_AfterRotation() {
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        homeRobot.assertDashboardVisible()
        
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        homeRobot.assertDashboardVisible()
    }
}
