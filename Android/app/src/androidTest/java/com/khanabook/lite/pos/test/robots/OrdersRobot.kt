package com.khanabook.lite.pos.test.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeUiTest

class OrdersRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val ordersListMatcher = hasText("Orders", substring = true)
    private val orderItemMatcher = hasText("Order #", substring = true)
    private val pendingStatusMatcher = hasText("Pending", substring = true)
    private val completedStatusMatcher = hasText("Completed", substring = true)
    private val filterButtonMatcher = hasContentDescription("Filter").or(hasText("Filter"))
    private val searchMatcher = hasPlaceholder("Search orders")
    private val refreshMatcher = hasContentDescription("Refresh").or(hasText("Refresh"))

    fun tapOrder(orderIndex: Int = 0): OrderDetailRobot {
        composeTestRule.onAllNodes(orderItemMatcher)
            .getOrNull(orderIndex)
            ?.performClick()
        return OrderDetailRobot(composeTestRule)
    }

    fun filterByStatus(status: OrderStatus): OrdersRobot {
        composeTestRule.onNode(filterButtonMatcher).performClick()
        composeTestRule.onNode(hasText(status.displayName, substring = true)).performClick()
        return this
    }

    fun searchOrder(orderId: String): OrdersRobot {
        composeTestRule.onNode(searchMatcher).performTextInput(orderId)
        return this
    }

    fun pullToRefresh(): OrdersRobot {
        composeTestRule.onNode(ordersListMatcher).performTouchInput {
            down(500f, 300f)
            moveTo(500f, 100f)
            up()
        }
        return this
    }

    fun swipeOrder(orderIndex: Int = 0, direction: SwipeDirection = SwipeDirection.LEFT): OrdersRobot {
        val node = composeTestRule.onAllNodes(orderItemMatcher).getOrNull(orderIndex)
        node?.let {
            when (direction) {
                SwipeDirection.LEFT -> {
                    it.performTouchInput {
                        down(800f, 500f)
                        moveTo(100f, 500f)
                        up()
                    }
                }
                SwipeDirection.RIGHT -> {
                    it.performTouchInput {
                        down(100f, 500f)
                        moveTo(800f, 500f)
                        up()
                    }
                }
            }
        }
        return this
    }

    fun assertOrderExists(orderId: String): OrdersRobot {
        composeTestRule.onNode(hasText(orderId, substring = true)).assertIsDisplayed()
        return this
    }

    fun assertOrdersListNotEmpty(): OrdersRobot {
        composeTestRule.onNode(orderItemMatcher).assertIsDisplayed()
        return this
    }

    fun assertEmptyStateShown(): OrdersRobot {
        composeTestRule.onNode(hasText("No orders", substring = true)).assertIsDisplayed()
        return this
    }

    fun assertPendingOrdersVisible(): OrdersRobot {
        composeTestRule.onNode(pendingStatusMatcher).assertIsDisplayed()
        return this
    }

    fun waitForOrdersToLoad(): OrdersRobot {
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            try {
                composeTestRule.onAllNodes(orderItemMatcher).fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }

    fun tapBack(): OrdersRobot {
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        return this
    }

    enum class OrderStatus(val displayName: String) {
        ALL("All"),
        PENDING("Pending"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled")
    }

    enum class SwipeDirection {
        LEFT, RIGHT
    }
}

class OrderDetailRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val orderIdMatcher = hasText("Order #")
    private val orderItemsMatcher = hasText("Items", substring = true)
    private val totalAmountMatcher = hasText("Total", substring = true)
    private val printButtonMatcher = hasContentDescription("Print").or(hasText("Print"))
    private val statusUpdateMatcher = hasText("Update Status")
    private val shareButtonMatcher = hasContentDescription("Share").or(hasText("Share"))

    fun tapPrint(): OrderDetailRobot {
        composeTestRule.onNode(printButtonMatcher).performClick()
        return this
    }

    fun tapUpdateStatus(): OrderDetailRobot {
        composeTestRule.onNode(statusUpdateMatcher).performClick()
        return this
    }

    fun tapShare(): OrderDetailRobot {
        composeTestRule.onNode(shareButtonMatcher).performClick()
        return this
    }

    fun selectNewStatus(status: String): OrderDetailRobot {
        composeTestRule.onNode(hasText(status, substring = true)).performClick()
        return this
    }

    fun confirmStatusUpdate(): OrderDetailRobot {
        composeTestRule.onNode(hasText("Confirm").or(hasText("Update"))).performClick()
        return this
    }

    fun assertOrderDetailsVisible(): OrderDetailRobot {
        composeTestRule.onNode(orderIdMatcher).assertIsDisplayed()
        composeTestRule.onNode(orderItemsMatcher).assertIsDisplayed()
        return this
    }

    fun assertTotalAmountVisible(): OrderDetailRobot {
        composeTestRule.onNode(totalAmountMatcher).assertIsDisplayed()
        return this
    }

    fun assertPrintButtonVisible(): OrderDetailRobot {
        composeTestRule.onNode(printButtonMatcher).assertIsDisplayed()
        return this
    }

    fun pressBack(): OrderDetailRobot {
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        return this
    }
}
