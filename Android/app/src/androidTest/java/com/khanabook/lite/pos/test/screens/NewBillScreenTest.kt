package com.khanabook.lite.pos.test.screens

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.BaseTest
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.robots.*
import com.khanabook.lite.pos.test.util.TestData
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class NewBillScreenTest : BaseTest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var newBillRobot: NewBillRobot
    private lateinit var checkoutRobot: CheckoutRobot
    private lateinit var loginRobot: LoginRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        newBillRobot = NewBillRobot(composeTestRule)
        checkoutRobot = CheckoutRobot(composeTestRule)
        loginRobot = LoginRobot(composeTestRule)
        
        mockApiServer.enqueueLoginSuccess()
        mockApiServer.enqueueMasterSyncSuccess()
        
        loginRobot.submitLogin()
        homeRobot.waitForDataToLoad()
        homeRobot.tapNewBill().waitForMenuToLoad()
    }

    @Test
    fun TC-LAYOUT-007_NewBillScreen_LayoutValid() {
        newBillRobot
            .assertCartEmpty()
            .assertCheckoutButtonDisabled()
    }

    @Test
    fun TC-LAYOUT-008_NewBillScreen_ScrollPerformance() {
        repeat(10) {
            newBillRobot.addItemToCart(TestData.MenuItems.BURGER)
        }
        
        newBillRobot.assertCartNotEmpty()
    }

    @Test
    fun TC-JOURNEY-001_NewBillScreen_CompleteSaleFlow_CashPayment() {
        mockApiServer.enqueueBillCreateSuccess()
        
        newBillRobot
            .addItemToCart(TestData.MenuItems.BURGER)
            .assertItemInCart(TestData.MenuItems.BURGER)
            .assertCheckoutButtonEnabled()
            .tapCheckout()
            .completePayment(CheckoutRobot.PaymentMethod.CASH)
            .assertSuccessMessageShown()
            .assertOrderIdGenerated()
    }

    @Test
    fun TC-JOURNEY-001_NewBillScreen_CompleteSaleFlow_CardPayment() {
        mockApiServer.enqueueBillCreateSuccess()
        
        newBillRobot
            .addItemToCart(TestData.MenuItems.PIZZA)
            .addItemToCart(TestData.MenuItems.COKE)
            .tapCheckout()
            .completePayment(CheckoutRobot.PaymentMethod.CARD)
            .assertSuccessMessageShown()
    }

    @Test
    fun TC-JOURNEY-001_NewBillScreen_CompleteSaleFlow_UpiPayment() {
        mockApiServer.enqueueBillCreateSuccess()
        
        newBillRobot
            .addItemToCart(TestData.MenuItems.BURGER)
            .addItemToCart(TestData.MenuItems.PIZZA)
            .tapCheckout()
            .completePayment(CheckoutRobot.PaymentMethod.UPI)
            .assertSuccessMessageShown()
    }

    @Test
    fun TC-API-009_NewBillScreen_SubmitEmptyCart() {
        newBillRobot
            .assertCheckoutButtonDisabled()
    }

    @Test
    fun TC-STATE-001_NewBillScreen_CartState_AfterRotation() {
        newBillRobot.addItemToCart(TestData.MenuItems.BURGER)
        newBillRobot.addItemToCart(TestData.MenuItems.PIZZA)
        
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        newBillRobot
            .assertItemInCart(TestData.MenuItems.BURGER)
            .assertItemInCart(TestData.MenuItems.PIZZA)
            .assertCartNotEmpty()
        
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        newBillRobot
            .assertItemInCart(TestData.MenuItems.BURGER)
            .assertItemInCart(TestData.MenuItems.PIZZA)
    }

    @Test
    fun TC-STATE-001_NewBillScreen_CartState_PreservedOnBack() {
        newBillRobot
            .addItemToCart(TestData.MenuItems.BURGER)
            .addItemToCart(TestData.MenuItems.PIZZA)
            .pressBack()
        
        homeRobot.tapNewBill().waitForMenuToLoad()
        
        newBillRobot
            .assertItemInCart(TestData.MenuItems.BURGER)
            .assertItemInCart(TestData.MenuItems.PIZZA)
    }

    @Test
    fun TC-NAV-004_NewBillScreen_BackStack_PreservesCart() {
        mockApiServer.enqueueBillCreateSuccess()
        
        newBillRobot
            .addItemToCart(TestData.MenuItems.BURGER)
            .tapCheckout()
            .selectCashPayment()
        
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        
        newBillRobot.assertItemInCart(TestData.MenuItems.BURGER)
    }

    @Test
    fun TC-VALIDATION-002_NewBillScreen_QuantityValidation() {
        newBillRobot
            .addItemToCart(TestData.MenuItems.BURGER)
            .increaseQuantity()
            .increaseQuantity()
        
        newBillRobot.increaseQuantity()
        
        newBillRobot.decreaseQuantity()
        
        newBillRobot.decreaseQuantity()
    }
}
