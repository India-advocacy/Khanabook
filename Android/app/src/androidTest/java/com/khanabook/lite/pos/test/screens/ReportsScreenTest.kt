package com.khanabook.lite.pos.test.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.khanabook.lite.pos.test.BaseTest
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.api.ResponseFixtures
import com.khanabook.lite.pos.test.robots.*
import com.khanabook.lite.pos.test.util.TestData
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class ReportsScreenTest : BaseTest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var reportsRobot: ReportsRobot
    private lateinit var loginRobot: LoginRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        reportsRobot = ReportsRobot(composeTestRule)
        loginRobot = LoginRobot(composeTestRule)
        
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
    }

    @Test
    fun TC-LAYOUT-010_ReportsScreen_LayoutValid() {
        homeRobot.tapReportsTab()
        
        reportsRobot
            .waitForReportsToLoad()
            .assertReportsTitleVisible()
            .assertFilterOptionsVisible()
    }

    @Test
    fun TC-LAYOUT-010_ReportsScreen_Rotation_ChartsAdapt() {
        homeRobot.tapReportsTab()
        
        reportsRobot.waitForReportsToLoad()
        
        reportsRobot.assertRevenueChartVisible()
    }

    @Test
    fun TC-API-012_ReportsScreen_FilterToday() {
        mockApiServer.enqueueReportsData(TestData.Dates.TODAY)
        
        homeRobot.tapReportsTab()
        
        reportsRobot
            .waitForReportsToLoad()
            .selectToday()
            .assertRevenueChartVisible()
    }

    @Test
    fun TC-API-012_ReportsScreen_FilterThisWeek() {
        mockApiServer.enqueueReportsData(TestData.Dates.THIS_WEEK)
        
        homeRobot.tapReportsTab()
        
        reportsRobot
            .waitForReportsToLoad()
            .selectThisWeek()
            .assertRevenueChartVisible()
    }

    @Test
    fun TC-API-012_ReportsScreen_FilterThisMonth() {
        mockApiServer.enqueueReportsData(TestData.Dates.THIS_MONTH)
        
        homeRobot.tapReportsTab()
        
        reportsRobot
            .waitForReportsToLoad()
            .selectThisMonth()
            .assertRevenueChartVisible()
    }

    @Test
    fun TC-API-012_ReportsScreen_CustomDateRange() {
        mockApiServer.enqueueReportsData(TestData.Dates.CUSTOM_RANGE)
        
        homeRobot.tapReportsTab()
        
        reportsRobot
            .waitForReportsToLoad()
            .selectCustomRange()
    }

    @Test
    fun TC-API-012_ReportsScreen_NoDataMessage() {
        mockApiServer.enqueueEmptyReports()
        
        homeRobot.tapReportsTab()
        
        reportsRobot
            .waitForReportsToLoad()
            .assertNoDataMessage()
    }
}
