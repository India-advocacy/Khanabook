package com.khanabook.lite.pos.test.util

object TestData {

    object ValidCredentials {
        const val PHONE = "9876543210"
        const val PASSWORD = "Test@1234"
        const val NAME = "Test User"
        const val EMAIL = "test@example.com"
    }

    object InvalidCredentials {
        const val PHONE = "9876543210"
        const val WRONG_PASSWORD = "wrongpass"
        const val EMPTY_PHONE = ""
        const val EMPTY_PASSWORD = ""
        const val SHORT_PASSWORD = "123"
        const val LONG_PHONE = "987654321012345"
    }

    object MenuItems {
        const val BURGER = "Burger"
        const val PIZZA = "Pizza"
        const val PASTA = "Pasta"
        const val COKE = "Coke"
        const val COFFEE = "Coffee"
        
        val POPULAR_ITEMS = listOf(BURGER, PIZZA, PASTA)
    }

    object Categories {
        const val STARTERS = "Starters"
        const val MAIN_COURSE = "Main Course"
        const val BEVERAGES = "Beverages"
        const val DESSERTS = "Desserts"
        
        val ALL = listOf(STARTERS, MAIN_COURSE, BEVERAGES, DESSERTS)
    }

    object BillAmounts {
        const val AMOUNT_100 = "100"
        const val AMOUNT_500 = "500"
        const val AMOUNT_1000 = "1000"
        const val EXACT_AMOUNT = "250"
    }

    object DiscountCodes {
        const val VALID_10_PERCENT = "SAVE10"
        const val VALID_20_PERCENT = "SAVE20"
        const val INVALID_CODE = "INVALID"
        const val EXPIRED_CODE = "EXPIRED2020"
    }

    object OrderStatuses {
        const val PENDING = "Pending"
        const val PREPARING = "Preparing"
        const val READY = "Ready"
        const val COMPLETED = "Completed"
        const val CANCELLED = "Cancelled"
    }

    object PaymentMethods {
        const val CASH = "Cash"
        const val CARD = "Card"
        const val UPI = "UPI"
        const val WALLET = "Wallet"
    }

    object ValidationMessages {
        const val PHONE_REQUIRED = "Phone number is required"
        const val PASSWORD_REQUIRED = "Password is required"
        const val PHONE_INVALID = "Invalid phone number"
        const val PASSWORD_SHORT = "Password must be at least 8 characters"
        const val CART_EMPTY = "Cart is empty"
        const val ITEM_REQUIRED = "Please select at least one item"
    }

    object ErrorMessages {
        const val NETWORK_ERROR = "Network error. Please check your connection."
        const val SERVER_ERROR = "Server error. Please try again later."
        const val AUTH_FAILED = "Authentication failed"
        const val SESSION_EXPIRED = "Session expired. Please login again."
        const val UNKNOWN_ERROR = "Something went wrong"
    }

    object SuccessMessages {
        const val LOGIN_SUCCESS = "Login successful"
        const val ORDER_CREATED = "Order created successfully"
        const val MENU_IMPORTED = "Menu items imported successfully"
        const val SETTINGS_SAVED = "Settings saved"
        const val LOGOUT_SUCCESS = "Logged out successfully"
    }

    object ApiEndpoints {
        const val LOGIN = "/api/auth/login"
        const val SIGNUP = "/api/auth/register"
        const val REFRESH_TOKEN = "/api/auth/refresh"
        const val LOGOUT = "/api/auth/logout"
        const val MASTER_SYNC = "/api/sync/master"
        const val BILLS = "/api/bills"
        const val ORDERS = "/api/orders"
        const val MENU_SYNC = "/api/menu/sync"
        const val PROFILE = "/api/profile"
    }

    object TimeoutValues {
        const val SHORT = 2000L
        const val MEDIUM = 5000L
        const val LONG = 10000L
        const val EXTRA_LONG = 15000L
    }

    object Dates {
        const val TODAY = "Today"
        const val THIS_WEEK = "This Week"
        const val THIS_MONTH = "This Month"
        const val LAST_7_DAYS = "Last 7 Days"
        const val LAST_30_DAYS = "Last 30 Days"
        const val CUSTOM_RANGE = "Custom Range"
    }
}
