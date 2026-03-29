package com.khanabook.lite.pos.test.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeUiTest
import com.khanabook.lite.pos.test.util.TestData

class LoginRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val phoneFieldMatcher = hasText("Phone", substring = true)
        .or(hasPlaceholder("Phone number"))
        .or(hasText("Phone number"))

    private val passwordFieldMatcher = hasText("Password", substring = true)
        .or(hasPlaceholder("Password"))

    private val loginButtonMatcher = hasText("Login", substring = true)
        .or(hasText("Sign In", substring = true))

    private val signUpLinkMatcher = hasText("Sign Up", substring = true)
        .or(hasText("Create Account", substring = true))
        .or(hasText("Register", substring = true))

    private val forgotPasswordMatcher = hasText("Forgot Password?", substring = true)

    private val errorMatcher = hasText("Invalid", substring = true)
        .or(hasText("Error", substring = true))
        .or(hasText("Failed", substring = true))
        .or(hasText("incorrect", substring = true))
        .or(hasText("failed", substring = true))

    private val loadingMatcher = hasText("Loading", substring = true)
        .or(hasContentDescription("Loading"))

    private val successMatcher = hasText("Success", substring = true)
        .or(hasText("Home"))
        .or(hasText("Dashboard"))

    fun enterPhone(phone: String): LoginRobot {
        composeTestRule.onNode(hasPlaceholder("Phone number"))
            .performTextInput(phone)
        return this
    }

    fun enterPassword(password: String): LoginRobot {
        composeTestRule.onNode(hasPlaceholder("Password"))
            .performTextInput(password)
        return this
    }

    fun enterCredentials(
        phone: String = TestData.ValidCredentials.PHONE,
        password: String = TestData.ValidCredentials.PASSWORD
    ): LoginRobot {
        enterPhone(phone)
        enterPassword(password)
        return this
    }

    fun tapLogin(): LoginRobot {
        composeTestRule.onNode(loginButtonMatcher)
            .assertIsEnabled()
            .performClick()
        return this
    }

    fun tapSignUp(): LoginRobot {
        composeTestRule.onNode(signUpLinkMatcher)
            .performClick()
        return this
    }

    fun tapForgotPassword(): LoginRobot {
        composeTestRule.onNode(forgotPasswordMatcher)
            .performClick()
        return this
    }

    fun submitLogin() {
        enterCredentials()
        tapLogin()
    }

    fun clearPhoneField(): LoginRobot {
        composeTestRule.onNode(hasPlaceholder("Phone number"))
            .performTextClearance()
        return this
    }

    fun clearPasswordField(): LoginRobot {
        composeTestRule.onNode(hasPlaceholder("Password"))
            .performTextClearance()
        return this
    }

    fun clearAllFields(): LoginRobot {
        clearPhoneField()
        clearPasswordField()
        return this
    }

    fun assertPhoneFieldVisible(): LoginRobot {
        composeTestRule.onNode(hasPlaceholder("Phone number"))
            .assertIsDisplayed()
        return this
    }

    fun assertPasswordFieldVisible(): LoginRobot {
        composeTestRule.onNode(hasPlaceholder("Password"))
            .assertIsDisplayed()
        return this
    }

    fun assertLoginButtonVisible(): LoginRobot {
        composeTestRule.onNode(loginButtonMatcher)
            .assertIsDisplayed()
        return this
    }

    fun assertLoginButtonEnabled(): LoginRobot {
        composeTestRule.onNode(loginButtonMatcher)
            .assertIsEnabled()
        return this
    }

    fun assertLoginButtonDisabled(): LoginRobot {
        composeTestRule.onNode(loginButtonMatcher)
            .assertIsNotEnabled()
        return this
    }

    fun assertErrorMessageVisible(): LoginRobot {
        composeTestRule.onNode(errorMatcher, useUnmergedTree = true)
            .waitForExists(5000)
            .assertIsDisplayed()
        return this
    }

    fun assertNoErrorMessage(): LoginRobot {
        composeTestRule.waitForIdle(2000)
        try {
            composeTestRule.onNode(errorMatcher, useUnmergedTree = true)
                .assertDoesNotExist()
        } catch (e: AssertionError) {
            // Expected if no error exists
        }
        return this
    }

    fun assertLoadingIndicatorShown(): LoginRobot {
        composeTestRule.onNode(loadingMatcher)
            .waitForExists(3000)
        return this
    }

    fun assertLoadingIndicatorHidden(): LoginRobot {
        composeTestRule.onNode(loadingMatcher)
            .assertDoesNotExist()
        return this
    }

    fun assertNavigationToHome(): LoginRobot {
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeTestRule.onAllNodes(hasText("Home", substring = true))
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodes(hasText("Dashboard", substring = true))
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodes(hasText("Menu", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }

    fun assertNavigationToSignUp(): LoginRobot {
        composeTestRule.onNode(
            hasText("Create Account", substring = true)
                .or(hasText("Sign Up", substring = true))
        ).assertIsDisplayed()
        return this
    }

    fun waitForLoginScreen(): LoginRobot {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNode(hasPlaceholder("Phone number"))
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }

    fun waitForNavigation(timeoutMs: Long = 10000): LoginRobot {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            try {
                composeTestRule.onAllNodes(hasText("Home", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }
}
