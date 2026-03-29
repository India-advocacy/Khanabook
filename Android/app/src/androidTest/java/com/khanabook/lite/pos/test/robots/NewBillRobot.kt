package com.khanabook.lite.pos.test.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeUiTest
import com.khanabook.lite.pos.test.util.TestData

class NewBillRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val addItemButtonMatcher = hasText("Add Item", substring = true).or(hasContentDescription("Add Item"))
    private val checkoutButtonMatcher = hasText("Checkout", substring = true).or(hasText("Proceed to Pay"))
    private val categoryMatcher = hasText("Category", substring = true)
    private val itemMatcher = hasText(TestData.MenuItems.BURGER)
    private val pizzaMatcher = hasText(TestData.MenuItems.PIZZA)
    private val quantityPlusMatcher = hasContentDescription("Increase quantity")
    private val quantityMinusMatcher = hasContentDescription("Decrease quantity")
    private val cartTotalMatcher = hasText("Total", substring = true)
    private val emptyCartMatcher = hasText("Cart is empty", substring = true)

    fun tapAddItem(): NewBillRobot {
        composeTestRule.onNode(addItemButtonMatcher).performClick()
        return this
    }

    fun selectCategory(categoryName: String): NewBillRobot {
        composeTestRule.onNode(hasText(categoryName, substring = true)).performClick()
        return this
    }

    fun selectItem(itemName: String = TestData.MenuItems.BURGER): NewBillRobot {
        composeTestRule.onNode(hasText(itemName, substring = true)).performClick()
        return this
    }

    fun increaseQuantity(): NewBillRobot {
        composeTestRule.onNode(quantityPlusMatcher).performClick()
        return this
    }

    fun decreaseQuantity(): NewBillRobot {
        composeTestRule.onNode(quantityMinusMatcher).performClick()
        return this
    }

    fun tapCheckout(): CheckoutRobot {
        composeTestRule.onNode(checkoutButtonMatcher).performClick()
        return CheckoutRobot(composeTestRule)
    }

    fun addItemToCart(itemName: String = TestData.MenuItems.BURGER): NewBillRobot {
        tapAddItem()
        selectItem(itemName)
        return this
    }

    fun addMultipleItems(itemNames: List<String>): NewBillRobot {
        itemNames.forEach { addItemToCart(it) }
        return this
    }

    fun assertItemInCart(itemName: String): NewBillRobot {
        composeTestRule.onNode(hasText(itemName, substring = true)).assertIsDisplayed()
        return this
    }

    fun assertCartNotEmpty(): NewBillRobot {
        composeTestRule.onNode(emptyCartMatcher).assertDoesNotExist()
        return this
    }

    fun assertCartEmpty(): NewBillRobot {
        composeTestRule.onNode(emptyCartMatcher).assertIsDisplayed()
        return this
    }

    fun assertTotalVisible(): NewBillRobot {
        composeTestRule.onNode(cartTotalMatcher).assertIsDisplayed()
        return this
    }

    fun assertCheckoutButtonEnabled(): NewBillRobot {
        composeTestRule.onNode(checkoutButtonMatcher).assertIsEnabled()
        return this
    }

    fun assertCheckoutButtonDisabled(): NewBillRobot {
        composeTestRule.onNode(checkoutButtonMatcher).assertIsNotEnabled()
        return this
    }

    fun waitForMenuToLoad(): NewBillRobot {
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            try {
                composeTestRule.onNode(addItemButtonMatcher).fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return this
    }

    fun pressBack(): NewBillRobot {
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        return this
    }
}

class CheckoutRobot(private val composeTestRule: AndroidComposeUiTest<*>) {

    private val cashPaymentMatcher = hasText("Cash", substring = true)
    private val cardPaymentMatcher = hasText("Card", substring = true)
    private val upiPaymentMatcher = hasText("UPI", substring = true)
    private val completeButtonMatcher = hasText("Complete", substring = true).or(hasText("Pay"))
    private val amountFieldMatcher = hasPlaceholder("Amount")
    private val discountFieldMatcher = hasText("Discount", substring = true)
    private val successMessageMatcher = hasText("Success", substring = true).or(hasText("Order Created"))
    private val orderIdMatcher = hasText("Order #")

    fun selectCashPayment(): CheckoutRobot {
        composeTestRule.onNode(cashPaymentMatcher).performClick()
        return this
    }

    fun selectCardPayment(): CheckoutRobot {
        composeTestRule.onNode(cardPaymentMatcher).performClick()
        return this
    }

    fun selectUpiPayment(): CheckoutRobot {
        composeTestRule.onNode(upiPaymentMatcher).performClick()
        return this
    }

    fun enterCustomAmount(amount: String): CheckoutRobot {
        composeTestRule.onNode(amountFieldMatcher).performTextInput(amount)
        return this
    }

    fun applyDiscount(discountPercent: Int = 10): CheckoutRobot {
        composeTestRule.onNode(hasText("Apply")).performClick()
        return this
    }

    fun tapComplete(): CheckoutRobot {
        composeTestRule.onNode(completeButtonMatcher).performClick()
        return this
    }

    fun completePayment(paymentMethod: PaymentMethod = PaymentMethod.CASH): CheckoutRobot {
        when (paymentMethod) {
            PaymentMethod.CASH -> selectCashPayment()
            PaymentMethod.CARD -> selectCardPayment()
            PaymentMethod.UPI -> selectUpiPayment()
        }
        tapComplete()
        return this
    }

    fun assertPaymentMethodsVisible(): CheckoutRobot {
        listOf(cashPaymentMatcher, cardPaymentMatcher, upiPaymentMatcher).forEach { matcher ->
            composeTestRule.onNode(matcher).assertIsDisplayed()
        }
        return this
    }

    fun assertSuccessMessageShown(): CheckoutRobot {
        composeTestRule.onNode(successMessageMatcher).waitForExists(10000)
        composeTestRule.onNode(successMessageMatcher).assertIsDisplayed()
        return this
    }

    fun assertOrderIdGenerated(): CheckoutRobot {
        composeTestRule.onNode(orderIdMatcher).assertIsDisplayed()
        return this
    }

    fun assertTotalAmountShown(): CheckoutRobot {
        composeTestRule.onNode(hasText("₹", substring = true)).assertIsDisplayed()
        return this
    }

    enum class PaymentMethod {
        CASH, CARD, UPI
    }
}
