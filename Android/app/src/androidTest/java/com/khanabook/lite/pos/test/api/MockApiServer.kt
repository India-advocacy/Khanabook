package com.khanabook.lite.pos.test.api

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.TimeUnit

class MockApiServer : MockWebServer() {

    private val responseFixtures = ResponseFixtures()

    private val dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when {
                request.path!!.contains("/api/auth/login") -> handleLogin(request)
                request.path!!.contains("/api/auth/register") -> handleSignup(request)
                request.path!!.contains("/api/auth/refresh") -> handleRefreshToken(request)
                request.path!!.contains("/api/sync/master") -> handleMasterSync(request)
                request.path!!.contains("/api/bills") && request.method == "POST" -> handleBillCreate(request)
                request.path!!.contains("/api/bills") -> handleBillsList(request)
                request.path!!.contains("/api/orders") -> handleOrders(request)
                request.path!!.contains("/api/orders/") -> handleOrderDetail(request)
                request.path!!.contains("/api/menu/sync") -> handleMenuSync(request)
                request.path!!.contains("/api/reports") -> handleReports(request)
                request.path!!.contains("/api/profile") -> handleProfile(request)
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    init {
        setDispatcher(dispatcher)
    }

    private fun handleLogin(request: RecordedRequest): MockResponse {
        return when {
            request.body.readUtf8().contains("wrongpass") -> {
                responseFixtures.loginFailure()
            }
            request.body.readUtf8().contains("\"phone\":\"\"") || 
            request.body.readUtf8().contains("\"password\":\"\"") -> {
                responseFixtures.badRequest("Validation failed")
            }
            else -> responseFixtures.loginSuccess()
        }
    }

    private fun handleSignup(request: RecordedRequest): MockResponse {
        return responseFixtures.signupSuccess()
    }

    private fun handleRefreshToken(request: RecordedRequest): MockResponse {
        return responseFixtures.refreshTokenSuccess()
    }

    private fun handleMasterSync(request: RecordedRequest): MockResponse {
        return responseFixtures.masterSyncSuccess()
    }

    private fun handleBillCreate(request: RecordedRequest): MockResponse {
        return if (request.body.readUtf8().isEmpty() || 
                   !request.body.readUtf8().contains("items")) {
            responseFixtures.badRequest("At least one item required")
        } else {
            responseFixtures.billCreateSuccess()
        }
    }

    private fun handleBillsList(request: RecordedRequest): MockResponse {
        return responseFixtures.billsList()
    }

    private fun handleOrders(request: RecordedRequest): MockResponse {
        return when {
            request.path!!.contains("status=Pending") -> responseFixtures.ordersByStatus("Pending")
            request.path!!.contains("status=Completed") -> responseFixtures.ordersByStatus("Completed")
            else -> responseFixtures.ordersList()
        }
    }

    private fun handleOrderDetail(request: RecordedRequest): MockResponse {
        val orderId = request.path!!.substringAfterLast("/")
        return responseFixtures.orderDetail(orderId)
    }

    private fun handleMenuSync(request: RecordedRequest): MockResponse {
        return responseFixtures.menuSyncSuccess()
    }

    private fun handleReports(request: RecordedRequest): MockResponse {
        return when {
            request.path!!.contains("range=today") -> responseFixtures.reportsData("Today")
            request.path!!.contains("range=week") -> responseFixtures.reportsData("This Week")
            request.path!!.contains("range=month") -> responseFixtures.reportsData("This Month")
            else -> responseFixtures.reportsData("Today")
        }
    }

    private fun handleProfile(request: RecordedRequest): MockResponse {
        return responseFixtures.profile()
    }

    fun enqueueLoginSuccess() = enqueue(responseFixtures.loginSuccess())
    fun enqueueLoginFailure() = enqueue(responseFixtures.loginFailure())
    fun enqueueSignupSuccess() = enqueue(responseFixtures.signupSuccess())
    fun enqueueMasterSyncSuccess() = enqueue(responseFixtures.masterSyncSuccess())
    fun enqueueBillCreateSuccess() = enqueue(responseFixtures.billCreateSuccess())
    fun enqueueEmptyOrders() = enqueue(responseFixtures.emptyOrders())
    fun enqueueFilteredOrders(status: String) = enqueue(responseFixtures.ordersByStatus(status))
    fun enqueueOrderDetail(orderId: String) = enqueue(responseFixtures.orderDetail(orderId))
    fun enqueueReportsData(range: String) = enqueue(responseFixtures.reportsData(range))
    fun enqueueEmptyReports() = enqueue(responseFixtures.emptyReports())

    fun enqueueServerError() = enqueue(responseFixtures.serverError())
    fun enqueueNetworkError() {
        shutdown()
    }
}
