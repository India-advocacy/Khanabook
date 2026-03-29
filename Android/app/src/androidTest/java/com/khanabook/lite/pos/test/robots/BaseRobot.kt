package com.khanabook.lite.pos.test.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.test.core.app.ApplicationProvider
import com.khanabook.lite.pos.test.util.TestData

abstract class BaseRobot {

    protected val composeTestRule: ComposeTimeoutTimeoutException
        get() = throw IllegalStateException("composeTestRule must be initialized")

    fun waitForScreen(screenMatcher: SemanticsMatcher, timeoutMillis: Long = 5000L) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodes(isRoot()).fetchSemanticsNodes().any {
                it.parseToString().contains(screenMatcher.toString())
            }
        }
    }

    fun waitForLoading(timeoutMillis: Long = 10000L) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodes(isRoot()).fetchSemanticsNodes().none {
                it.parseToString().contains("Loading")
            }
        }
    }

    fun pressBack() {
        composeTestRule.activityRule.activity.onBackPressedDispatcher.onBackPressed()
    }

    fun enterText(
        matcher: SemanticsMatcher,
        text: String,
        clearFirst: Boolean = false,
        immediate: Boolean = false
    ) {
        val node = composeTestRule.onNode(matcher, useUnmergedTree = true)
        if (clearFirst) {
            node.performTextClearance()
        }
        if (immediate) {
            node.performTextInput(text)
        } else {
            node.performClick()
                .performTextInput(text)
        }
    }

    fun click(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .performClick()
    }

    fun assertExists(matcher: SemanticsMatcher, timeoutMillis: Long = 5000L) {
        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .waitForExists(timeoutMillis)
            .assertExists()
    }

    fun assertDoesNotExist(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    fun isDisplayed(matcher: SemanticsMatcher): Boolean {
        return try {
            composeTestRule.onNode(matcher, useUnmergedTree = true)
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
    }

    fun scrollTo(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun scrollAndClick(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .performScrollTo()
            .performClick()
    }
}
