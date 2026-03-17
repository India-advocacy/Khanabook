package com.khanabook.lite.pos.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

data class OrderLevelRow(
    val dailyId: String,
    val lifetimeId: Int,
    val billId: Int,
    val paymentMode: PaymentMode,
    val date: String
)

data class OrderDetailRow(
    val dailyNo: String,
    val lifetimeNo: Int,
    val billId: Int,
    val currentStatus: String,
    val salesAmount: String,
    val payMode: PaymentMode,
    val orderStatus: OrderStatus,
    val salesDate: Long
)

data class DailySalesReport(
    val totalSales: String,
    val totalOrders: Int,
    val cashCollected: String,
    val upiCollected: String,
    val otherCollected: String
)

data class MonthlySalesReport(
    val month: Int,
    val year: Int,
    val totalSales: String,
    val totalOrders: Int
)

data class TopSellingItem(
    val itemName: String,
    val quantitySold: Int,
    val revenue: String
)


