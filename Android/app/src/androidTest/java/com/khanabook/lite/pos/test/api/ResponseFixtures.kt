package com.khanabook.lite.pos.test.api

import okhttp3.mockwebserver.MockResponse

object ResponseFixtures {

    private const val HEADER_CONTENT_TYPE = "Content-Type"
    private const val HEADER_APP_JSON = "application/json"
    private const val HEADER_AUTH = "Authorization"

    fun loginSuccess(): MockResponse {
        val body = """
            {
                "success": true,
                "message": "Login successful",
                "data": {
                    "user": {
                        "id": "user_001",
                        "name": "Test User",
                        "phone": "9876543210",
                        "email": "test@example.com",
                        "role": "owner"
                    },
                    "tokens": {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXzAwMSIsImlhdCI6MTYzOTg0MTYwMH0.mock_signature",
                        "refreshToken": "refresh_token_mock_12345",
                        "expiresIn": 86400
                    }
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setHeader(HEADER_AUTH, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock_signature")
            .setBody(body)
    }

    fun loginFailure(): MockResponse {
        val body = """
            {
                "success": false,
                "message": "Invalid credentials",
                "error": {
                    "code": "AUTH_FAILED",
                    "details": "Phone number or password is incorrect"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(401)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun signupSuccess(): MockResponse {
        val body = """
            {
                "success": true,
                "message": "Registration successful",
                "data": {
                    "user": {
                        "id": "user_002",
                        "name": "New User",
                        "phone": "9876543211",
                        "email": "newuser@example.com",
                        "role": "owner"
                    },
                    "tokens": {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new_user_token",
                        "refreshToken": "refresh_token_new_user",
                        "expiresIn": 86400
                    }
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(201)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun refreshTokenSuccess(): MockResponse {
        val body = """
            {
                "success": true,
                "data": {
                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshed_token",
                    "refreshToken": "new_refresh_token",
                    "expiresIn": 86400
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun masterSyncSuccess(): MockResponse {
        val body = """
            {
                "success": true,
                "data": {
                    "categories": [
                        {"id": "cat_001", "name": "Starters", "priority": 1},
                        {"id": "cat_002", "name": "Main Course", "priority": 2},
                        {"id": "cat_003", "name": "Beverages", "priority": 3},
                        {"id": "cat_004", "name": "Desserts", "priority": 4}
                    ],
                    "menuItems": [
                        {"id": "item_001", "name": "Burger", "price": 150.00, "categoryId": "cat_001"},
                        {"id": "item_002", "name": "Pizza", "price": 250.00, "categoryId": "cat_002"},
                        {"id": "item_003", "name": "Pasta", "price": 180.00, "categoryId": "cat_002"},
                        {"id": "item_004", "name": "Coke", "price": 50.00, "categoryId": "cat_003"},
                        {"id": "item_005", "name": "Coffee", "price": 80.00, "categoryId": "cat_003"}
                    ],
                    "variants": [
                        {"id": "var_001", "name": "Small", "priceModifier": -30, "menuItemId": "item_001"},
                        {"id": "var_002", "name": "Large", "priceModifier": 50, "menuItemId": "item_001"}
                    ],
                    "profile": {
                        "restaurantName": "Test Restaurant",
                        "address": "123 Test Street",
                        "phone": "9876543210"
                    }
                },
                "lastSyncTime": "2024-01-15T10:30:00Z"
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun billCreateSuccess(): MockResponse {
        val body = """
            {
                "success": true,
                "message": "Bill created successfully",
                "data": {
                    "billId": "BILL-${System.currentTimeMillis()}",
                    "orderId": "ORD-${System.currentTimeMillis()}",
                    "status": "completed",
                    "total": 400.00,
                    "items": [
                        {"name": "Burger", "quantity": 2, "price": 150.00},
                        {"name": "Pizza", "quantity": 1, "price": 100.00}
                    ],
                    "paymentMethod": "cash",
                    "createdAt": "2024-01-15T10:35:00Z"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(201)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun billsList(): MockResponse {
        val body = """
            {
                "success": true,
                "data": [
                    {
                        "id": "BILL-001",
                        "orderId": "ORD-001",
                        "total": 400.00,
                        "status": "completed",
                        "createdAt": "2024-01-15T10:35:00Z"
                    },
                    {
                        "id": "BILL-002",
                        "orderId": "ORD-002",
                        "total": 250.00,
                        "status": "completed",
                        "createdAt": "2024-01-15T09:20:00Z"
                    }
                ]
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun ordersList(): MockResponse {
        val body = """
            {
                "success": true,
                "data": [
                    {
                        "id": "ORD-001",
                        "billId": "BILL-001",
                        "status": "pending",
                        "total": 400.00,
                        "itemCount": 3,
                        "createdAt": "2024-01-15T10:35:00Z"
                    },
                    {
                        "id": "ORD-002",
                        "billId": "BILL-002",
                        "status": "completed",
                        "total": 250.00,
                        "itemCount": 2,
                        "createdAt": "2024-01-15T09:20:00Z"
                    }
                ]
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun ordersByStatus(status: String): MockResponse {
        val order = when (status) {
            "Pending" -> """{"id": "ORD-001", "status": "pending", "total": 400.00}"""
            "Completed" -> """{"id": "ORD-002", "status": "completed", "total": 250.00}"""
            else -> """{"id": "ORD-003", "status": "$status", "total": 300.00}"""
        }
        val body = """
            {
                "success": true,
                "data": [$order]
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun emptyOrders(): MockResponse {
        val body = """
            {
                "success": true,
                "data": []
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun orderDetail(orderId: String): MockResponse {
        val body = """
            {
                "success": true,
                "data": {
                    "id": "$orderId",
                    "billId": "BILL-001",
                    "status": "pending",
                    "items": [
                        {"name": "Burger", "quantity": 2, "price": 150.00, "total": 300.00},
                        {"name": "Pizza", "quantity": 1, "price": 100.00, "total": 100.00}
                    ],
                    "subtotal": 400.00,
                    "tax": 36.00,
                    "total": 436.00,
                    "paymentMethod": "cash",
                    "customerName": null,
                    "createdAt": "2024-01-15T10:35:00Z",
                    "updatedAt": "2024-01-15T10:35:00Z"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun menuSyncSuccess(): MockResponse {
        val body = """
            {
                "success": true,
                "message": "Menu synchronized successfully",
                "data": {
                    "syncedItems": 5,
                    "updatedItems": 2,
                    "newItems": 3
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun reportsData(range: String): MockResponse {
        val revenue = when (range) {
            "Today" -> 2500.00
            "This Week" -> 15000.00
            "This Month" -> 65000.00
            else -> 2500.00
        }
        val orderCount = when (range) {
            "Today" -> 25
            "This Week" -> 150
            "This Month" -> 650
            else -> 25
        }
        val body = """
            {
                "success": true,
                "data": {
                    "range": "$range",
                    "totalRevenue": $revenue,
                    "orderCount": $orderCount,
                    "averageOrderValue": ${revenue / orderCount},
                    "chartData": [
                        {"date": "2024-01-15", "revenue": 1200.00, "orders": 12},
                        {"date": "2024-01-14", "revenue": 1500.00, "orders": 15},
                        {"date": "2024-01-13", "revenue": 800.00, "orders": 8}
                    ]
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun emptyReports(): MockResponse {
        val body = """
            {
                "success": true,
                "data": {
                    "range": "Today",
                    "totalRevenue": 0,
                    "orderCount": 0,
                    "averageOrderValue": 0,
                    "chartData": []
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun profile(): MockResponse {
        val body = """
            {
                "success": true,
                "data": {
                    "id": "user_001",
                    "name": "Test User",
                    "phone": "9876543210",
                    "email": "test@example.com",
                    "restaurantName": "Test Restaurant",
                    "address": "123 Test Street",
                    "role": "owner"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun badRequest(message: String): MockResponse {
        val body = """
            {
                "success": false,
                "message": "$message",
                "error": {
                    "code": "VALIDATION_ERROR",
                    "details": "$message"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(400)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun serverError(): MockResponse {
        val body = """
            {
                "success": false,
                "message": "Internal server error",
                "error": {
                    "code": "SERVER_ERROR",
                    "details": "Something went wrong on our end"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(500)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun networkError(): MockResponse {
        return MockResponse().setResponseCode(503)
    }

    fun unauthorized(): MockResponse {
        val body = """
            {
                "success": false,
                "message": "Unauthorized",
                "error": {
                    "code": "UNAUTHORIZED",
                    "details": "Session expired. Please login again."
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(401)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun forbidden(): MockResponse {
        val body = """
            {
                "success": false,
                "message": "Forbidden",
                "error": {
                    "code": "FORBIDDEN",
                    "details": "You don't have permission to access this resource"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(403)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun notFound(resource: String): MockResponse {
        val body = """
            {
                "success": false,
                "message": "$resource not found",
                "error": {
                    "code": "NOT_FOUND",
                    "details": "The requested $resource does not exist"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(404)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun conflict(message: String): MockResponse {
        val body = """
            {
                "success": false,
                "message": "$message",
                "error": {
                    "code": "CONFLICT",
                    "details": "$message"
                }
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(409)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody(body)
    }

    fun slowResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader(HEADER_CONTENT_TYPE, HEADER_APP_JSON)
            .setBody("""{"success": true, "data": {"message": "Delayed response"}}""")
            .throttleBody(1024, 10, TimeUnit.SECONDS)
    }
}
