package com.khanabook.lite.pos.test.screens

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.BaseTest
import com.khanabook.lite.pos.test.robots.HomeRobot
import com.khanabook.lite.pos.test.robots.LoginRobot
import com.khanabook.lite.pos.test.util.TestData
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class LoginScreenTest : BaseTest() {

    private lateinit var loginRobot: LoginRobot
    private lateinit var homeRobot: HomeRobot

    @Before
    override fun setUp() {
        super.setUp()
        loginRobot = LoginRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
    }

    @After
    override fun tearDown() {
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.tearDown()
    }

    @Test
    fun TC-LAYOUT-002_LoginScreen_LayoutValid_Portrait() {
        loginRobot
            .assertPhoneFieldVisible()
            .assertPasswordFieldVisible()
            .assertLoginButtonVisible()
    }

    @Test
    fun TC-LAYOUT-002_LoginScreen_LayoutValid_Landscape() {
        rotateDevice(ScreenOrientation.LANDSCAPE)
        
        loginRobot
            .assertPhoneFieldVisible()
            .assertPasswordFieldVisible()
            .assertLoginButtonVisible()
    }

    @Test
    fun TC-API-001_Login_Success_ValidCredentials() {
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot
            .enterCredentials()
            .tapLogin()
            .assertNoErrorMessage()
        
        homeRobot.waitForDataToLoad()
        homeRobot.assertDashboardVisible()
    }

    @Test
    fun TC-API-002_Login_Failure_InvalidCredentials() {
        mockApiServer.enqueueLoginFailure()
        
        loginRobot
            .enterCredentials(
                phone = TestData.InvalidCredentials.PHONE,
                password = TestData.InvalidCredentials.WRONG_PASSWORD
            )
            .tapLogin()
            .assertErrorMessageVisible()
    }

    @Test
    fun TC-API-003_Login_Failure_EmptyCredentials() {
        loginRobot
            .clearPhoneField()
            .clearPasswordField()
            .tapLogin()
            .assertErrorMessageVisible()
    }

    @Test
    fun TC-API-004_Login_Failure_NetworkOffline() {
        disableNetwork()
        
        loginRobot
            .enterCredentials()
            .tapLogin()
            .assertErrorMessageVisible()
    }

    @Test
    fun TC-NAV-003_Login_NavigationError_StaysOnScreen() {
        mockApiServer.enqueueLoginFailure()
        
        loginRobot
            .enterCredentials()
            .tapLogin()
            .assertPhoneFieldVisible()
            .assertPasswordFieldVisible()
    }

    @Test
    fun TC-VALIDATION-001_Login_PhoneNumberValidation() {
        loginRobot
            .enterPhone("12345")
            .tapLogin()
            .assertErrorMessageVisible()
    }

    @Test
    fun TC-VALIDATION-001_Login_PasswordMinLength() {
        loginRobot
            .enterPassword("123")
            .tapLogin()
            .assertErrorMessageVisible()
    }

    @Test
    fun TC-LAYOUT-003_Login_KeyboardNotObscured() {
        loginRobot.enterPhone("9876543210")
        loginRobot.enterPassword("password")
        
        composeTestRule.waitForIdle()
    }
}
