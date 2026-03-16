package com.khanabook.lite.pos.data.remote.api

import com.khanabook.lite.pos.data.local.entity.*
import com.khanabook.lite.pos.data.remote.ResetPasswordRequest
import com.khanabook.lite.pos.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface KhanaBookApi {

        @POST("auth/login")
        suspend fun login(@Body request: LoginRequest): AuthResponse

        @POST("auth/signup")
        suspend fun signup(@Body request: SignupRequest): AuthResponse

        @POST("auth/google")
        suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse

        @POST("sync/bills/push")
        suspend fun pushBills(@Body bills: List<BillEntity>): PushSyncResponse

        @GET("sync/bills/pull")
        suspend fun pullBills(
                @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
                @Query("deviceId") deviceId: String
        ): List<BillEntity>

        @POST("sync/bills/items/push")
        suspend fun pushBillItems(@Body items: List<BillItemEntity>): PushSyncResponse

        @GET("sync/bills/items/pull")
        suspend fun pullBillItems(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): List<BillItemEntity>

        @POST("sync/bills/payments/push")
        suspend fun pushBillPayments(@Body payments: List<BillPaymentEntity>): PushSyncResponse

        @GET("sync/bills/payments/pull")
        suspend fun pullBillPayments(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): List<BillPaymentEntity>

        // Config Endpoints
        @POST("sync/restaurantprofile/push")
        suspend fun pushRestaurantProfiles(@Body profiles: List<RestaurantProfileEntity>): PushSyncResponse

        @GET("sync/restaurantprofile/pull")
        suspend fun pullRestaurantProfiles(
                @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
                @Query("deviceId") deviceId: String
        ): List<RestaurantProfileEntity>

        @POST("sync/config/users/push")
        suspend fun pushUsers(@Body users: List<UserEntity>): PushSyncResponse

        @GET("sync/config/users/pull")
        suspend fun pullUsers(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): List<UserEntity>

        // Menu Endpoints
        @POST("sync/menu/categories/push")
        suspend fun pushCategories(@Body categories: List<CategoryEntity>): PushSyncResponse

        @GET("sync/menu/categories/pull")
        suspend fun pullCategories(
                @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
                @Query("deviceId") deviceId: String
        ): List<CategoryEntity>

        @POST("sync/menuitem/push")
        suspend fun pushMenuItems(@Body items: List<MenuItemEntity>): PushSyncResponse

        @GET("sync/menuitem/pull")
        suspend fun pullMenuItems(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): List<MenuItemEntity>

        @POST("sync/itemvariant/push")
        suspend fun pushItemVariants(@Body variants: List<ItemVariantEntity>): PushSyncResponse

        @GET("sync/itemvariant/pull")
        suspend fun pullItemVariants(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): List<ItemVariantEntity>

        @POST("sync/stocklog/push")
        suspend fun pushStockLogs(@Body logs: List<StockLogEntity>): PushSyncResponse

        @GET("sync/stocklog/pull")
        suspend fun pullStockLogs(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): List<StockLogEntity>

        @GET("sync/master/pull")
        suspend fun pullMasterSync(
            @Query("lastSyncTimestamp") lastSyncTimestamp: Long,
            @Query("deviceId") deviceId: String
        ): MasterSyncResponse

        @POST("sync/restaurantprofile/counters/increment")
        suspend fun incrementCounters(): CounterResponse

        @POST("auth/reset-password")
        suspend fun resetPassword(@Body request: ResetPasswordRequest)

        @GET("auth/check-user")
        suspend fun checkUser(@Query("phoneNumber") phoneNumber: String): Boolean
}
