package com.khanabook.lite.pos.test.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.khanabook.lite.pos.test.BaseTest
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.robots.*
import com.khanabook.lite.pos.test.util.TestData
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class SettingsScreenTest : BaseTest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var menuConfigRobot: MenuConfigurationRobot
    private lateinit var logoutRobot: LogoutRobot
    private lateinit var loginRobot: LoginRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        menuConfigRobot = MenuConfigurationRobot(composeTestRule)
        logoutRobot = LogoutRobot(composeTestRule)
        loginRobot = LoginRobot(composeTestRule)
        
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        homeRobot.tapSettingsTab()
    }

    @Test
    fun TC-LAYOUT-012_SettingsScreen_LayoutValid() {
        settingsRobot.assertSectionsVisible()
    }

    @Test
    fun TC-NAV-009_SettingsScreen_NavigationToMenuConfig() {
        settingsRobot
            .tapMenuSection()
            .tapMenuConfiguration()
            .assertModeSelectionVisible()
    }

    @Test
    fun TC-NAV-009_SettingsScreen_BackFromMenuConfig() {
        settingsRobot
            .tapMenuSection()
            .tapMenuConfiguration()
            .pressBack()
        
        settingsRobot.assertSectionsVisible()
    }

    @Test
    fun TC-SEC-001_SettingsScreen_LogoutFlow() {
        settingsRobot
            .scrollToLogout()
            .tapLogout()
            .assertLogoutConfirmationShown()
            .confirmLogout()
            .assertLoginScreenShown()
    }

    @Test
    fun TC-SEC-001_SettingsScreen_Logout_CancelFlow() {
        settingsRobot
            .scrollToLogout()
            .tapLogout()
            .assertLogoutConfirmationShown()
            .cancelLogout()
            .assertSectionsVisible()
    }

    @Test
    fun TC-OFFLINE-002_SettingsScreen_OfflineAccess() {
        disableNetwork()
        
        settingsRobot.assertSectionsVisible()
        
        enableNetwork()
    }
}
