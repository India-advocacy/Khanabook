package com.khanabook.lite.pos.test.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeUiTest
import com.khanabook.lite.pos.test.util.TestData

class HomeRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val newBillButtonMatcher = hasText("New Bill", substring = true).or(hasContentDescription("New Bill"))
    private val ordersTabMatcher = hasText("Orders", substring = true)
    private val reportsTabMatcher = hasText("Reports", substring = true)
    private val searchTabMatcher = hasText("Search", substring = true)
    private val settingsTabMatcher = hasText("Settings", substring = true)
    private val homeTabMatcher = hasText("Home", substring = true).or(hasContentDescription("Home"))

    fun tapNewBill(): NewBillRobot {
        composeTestRule.onNode(newBillButtonMatcher).performClick()
        return NewBillRobot(composeTestRule)
    }

    fun tapOrdersTab(): OrdersRobot {
        composeTestRule.onNode(ordersTabMatcher).performClick()
        return OrdersRobot(composeTestRule)
    }

    fun tapReportsTab(): ReportsRobot {
        composeTestRule.onNode(reportsTabMatcher).performClick()
        return ReportsRobot(composeTestRule)
    }

    fun tapSettingsTab(): SettingsRobot {
        composeTestRule.onNode(settingsTabMatcher).performClick()
        return SettingsRobot(composeTestRule)
    }

    fun tapHomeTab(): HomeRobot {
        composeTestRule.onNode(homeTabMatcher).performClick()
        return this
    }

    fun assertDashboardVisible(): HomeRobot {
        composeTestRule.onNode(
            hasText("Dashboard", substring = true)
                .or(hasText("Home"))
        ).assertIsDisplayed()
        return this
    }

    fun assertQuickActionsVisible(): HomeRobot {
        composeTestRule.onNode(newBillButtonMatcher).assertIsDisplayed()
        return this
    }

    fun assertBottomNavigationVisible(): HomeRobot {
        listOf(ordersTabMatcher, reportsTabMatcher, searchTabMatcher, settingsTabMatcher).forEach { matcher ->
            composeTestRule.onNode(matcher).assertIsDisplayed()
        }
        return this
    }

    fun assertMetricsCardsVisible(): HomeRobot {
        composeTestRule.onNode(
            hasText("Today's Orders", substring = true)
                .or(hasText("Revenue", substring = true))
                .or(hasText("Pending", substring = true))
        ).assertIsDisplayed()
        return this
    }

    fun waitForDataToLoad(): HomeRobot {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNode(newBillButtonMatcher).fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }

    fun pullToRefresh(): HomeRobot {
        composeTestRule.onNode(hasText("Dashboard")).performTouchInput {
            down(500f, 200f)
            moveTo(500f, 1000f)
            up()
        }
        return this
    }
}
