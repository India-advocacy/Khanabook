package com.khanabook.lite.pos.test.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeUiTest

class SettingsRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val profileSectionMatcher = hasText("Profile", substring = true)
    private val menuSectionMatcher = hasText("Menu", substring = true)
    private val paymentSectionMatcher = hasText("Payment", substring = true)
    private val printerSectionMatcher = hasText("Printer", substring = true)
    private val shopSectionMatcher = hasText("Shop", substring = true)
    private val aboutSectionMatcher = hasText("About", substring = true)
    private val logoutButtonMatcher = hasText("Logout", substring = true).or(hasText("Sign Out"))
    private val menuConfigMatcher = hasText("Menu Configuration", substring = true)

    fun tapProfileSection(): SettingsRobot {
        composeTestRule.onNode(profileSectionMatcher).performClick()
        return this
    }

    fun tapMenuSection(): SettingsRobot {
        composeTestRule.onNode(menuSectionMatcher).performClick()
        return this
    }

    fun tapMenuConfiguration(): MenuConfigurationRobot {
        composeTestRule.onNode(menuConfigMatcher).performClick()
        return MenuConfigurationRobot(composeTestRule)
    }

    fun tapPaymentSection(): SettingsRobot {
        composeTestRule.onNode(paymentSectionMatcher).performClick()
        return this
    }

    fun tapPrinterSection(): SettingsRobot {
        composeTestRule.onNode(printerSectionMatcher).performClick()
        return this
    }

    fun tapShopSection(): SettingsRobot {
        composeTestRule.onNode(shopSectionMatcher).performClick()
        return this
    }

    fun tapAboutSection(): SettingsRobot {
        composeTestRule.onNode(aboutSectionMatcher).performClick()
        return this
    }

    fun tapLogout(): LogoutRobot {
        composeTestRule.onNode(logoutButtonMatcher).performClick()
        return LogoutRobot(composeTestRule)
    }

    fun scrollToLogout(): SettingsRobot {
        composeTestRule.onNode(logoutButtonMatcher).performScrollTo()
        return this
    }

    fun assertSectionsVisible(): SettingsRobot {
        listOf(
            profileSectionMatcher,
            menuSectionMatcher,
            paymentSectionMatcher,
            printerSectionMatcher
        ).forEach { matcher ->
            composeTestRule.onNode(matcher).assertIsDisplayed()
        }
        return this
    }

    fun assertLogoutButtonVisible(): SettingsRobot {
        scrollToLogout()
        composeTestRule.onNode(logoutButtonMatcher).assertIsDisplayed()
        return this
    }

    fun pressBack(): SettingsRobot {
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        return this
    }
}

class MenuConfigurationRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val modeSelectionMatcher = hasText("Select Mode", substring = true)
    private val manualModeMatcher = hasText("Manual", substring = true)
    private val smartImportMatcher = hasText("Smart Import", substring = true)
    private val selectPdfMatcher = hasText("Select PDF", substring = true).or(hasContentDescription("Select PDF"))
    private val scanImageMatcher = hasText("Scan Image", substring = true).or(hasContentDescription("Scan"))
    private val reviewItemsMatcher = hasText("Review Items", substring = true)
    private val importButtonMatcher = hasText("Import", substring = true)

    fun selectManualMode(): MenuConfigurationRobot {
        composeTestRule.onNode(manualModeMatcher).performClick()
        return this
    }

    fun selectSmartImport(): MenuConfigurationRobot {
        composeTestRule.onNode(smartImportMatcher).performClick()
        return this
    }

    fun tapSelectPdf(): MenuConfigurationRobot {
        composeTestRule.onNode(selectPdfMatcher).performClick()
        return this
    }

    fun tapScanImage(): MenuConfigurationRobot {
        composeTestRule.onNode(scanImageMatcher).performClick()
        return this
    }

    fun assertModeSelectionVisible(): MenuConfigurationRobot {
        composeTestRule.onNode(modeSelectionMatcher).assertIsDisplayed()
        return this
    }

    fun assertManualModeVisible(): MenuConfigurationRobot {
        composeTestRule.onNode(manualModeMatcher).assertIsDisplayed()
        return this
    }

    fun assertSmartImportVisible(): MenuConfigurationRobot {
        composeTestRule.onNode(smartImportMatcher).assertIsDisplayed()
        return this
    }

    fun waitForOcrProcessing(): MenuConfigurationRobot {
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeTestRule.onNode(reviewItemsMatcher).fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }

    fun pressBack(): MenuConfigurationRobot {
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        return this
    }
}

class LogoutRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val confirmButtonMatcher = hasText("Confirm", substring = true).or(hasText("Logout"))
    private val cancelButtonMatcher = hasText("Cancel", substring = true)

    fun confirmLogout(): LogoutRobot {
        composeTestRule.onNode(confirmButtonMatcher).performClick()
        return this
    }

    fun cancelLogout(): SettingsRobot {
        composeTestRule.onNode(cancelButtonMatcher).performClick()
        return SettingsRobot(composeTestRule)
    }

    fun assertLogoutConfirmationShown(): LogoutRobot {
        composeTestRule.onNode(confirmButtonMatcher).assertIsDisplayed()
        composeTestRule.onNode(cancelButtonMatcher).assertIsDisplayed()
        return this
    }

    fun assertLoginScreenShown(): LogoutRobot {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Login", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        return this
    }
}
