package com.khanabook.lite.pos.domain.manager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.khanabook.lite.pos.data.local.entity.BillEntity
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import com.khanabook.lite.pos.data.repository.BillRepository
import com.khanabook.lite.pos.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class ReportGenerator(private val billRepository: BillRepository) {

    suspend fun getPaymentBreakdown(from: String, to: String): Map<String, Double> {
        val bills = billRepository.getBillsByDateRange(from, to).firstOrNull() ?: emptyList()
        val breakdown = mutableMapOf<String, Double>()
        
        for (bill in bills) {
            if (OrderStatus.fromDbValue(bill.orderStatus) != OrderStatus.COMPLETED) continue
            
            val mode = PaymentMode.fromDbValue(bill.paymentMode)
            val amount = bill.totalAmount
            val label = mode.displayLabel
            
            if (PaymentModeManager.isPartPayment(mode)) {
                val labels = PaymentModeManager.getPartLabels(mode)
                // Add to individual totals
                breakdown[labels.first] = (breakdown[labels.first] ?: 0.0) + bill.partAmount1
                breakdown[labels.second] = (breakdown[labels.second] ?: 0.0) + bill.partAmount2
                
                // Add to mode total
                breakdown[label] = (breakdown[label] ?: 0.0) + amount
                
                // Add to specific part components for breakdown display
                val part1Key = "${label}_part1"
                val part2Key = "${label}_part2"
                breakdown[part1Key] = (breakdown[part1Key] ?: 0.0) + bill.partAmount1
                breakdown[part2Key] = (breakdown[part2Key] ?: 0.0) + bill.partAmount2
            } else {
                breakdown[label] = (breakdown[label] ?: 0.0) + amount
            }
        }
        return breakdown
    }

    suspend fun getOrderLevelRows(from: String, to: String): List<OrderLevelRow> {
        val bills = billRepository.getBillsByDateRange(from, to).firstOrNull() ?: emptyList()
        return bills.filter { OrderStatus.fromDbValue(it.orderStatus) == OrderStatus.COMPLETED }
            .map { bill ->
                OrderLevelRow(
                    dailyId = bill.dailyOrderDisplay,
                    lifetimeId = bill.lifetimeOrderId,
                    billId = bill.id,
                    paymentMode = PaymentMode.fromDbValue(bill.paymentMode),
                    date = com.khanabook.lite.pos.domain.util.DateUtils.formatDisplay(bill.createdAt)
                )
            }
    }

    suspend fun getOrderDetail(billId: Int): BillWithItems? {
        return billRepository.getBillWithItemsById(billId)
    }

    suspend fun getOrderDetailsTable(from: String, to: String): List<OrderDetailRow> {
        val bills = billRepository.getBillsByDateRange(from, to).firstOrNull() ?: emptyList()
        return bills.map { bill ->
            OrderDetailRow(
                dailyNo = bill.dailyOrderDisplay,
                lifetimeNo = bill.lifetimeOrderId,
                billId = bill.id,
                currentStatus = formatCurrentStatus(bill),
                salesAmount = bill.totalAmount,
                payMode = PaymentMode.fromDbValue(bill.paymentMode),
                orderStatus = OrderStatus.fromDbValue(bill.orderStatus),
                salesDate = bill.createdAt
            )
        }
    }

    fun formatCurrentStatus(bill: BillEntity): String {
        val status = OrderStatus.fromDbValue(bill.orderStatus).name.lowercase().replaceFirstChar { it.uppercase() }
        val payMode = PaymentMode.fromDbValue(bill.paymentMode).displayLabel
        return "Order $status [$payMode]"
    }

    suspend fun getDailyReport(date: String): DailySalesReport {
        // Assume date is 'yyyy-MM-dd', add wildcards for SQLite like query if needed, 
        // but let's assume getBillsByDateRange with same date works or we pad it.
        val startOfDay = "$date 00:00:00"
        val endOfDay = "$date 23:59:59"
        val bills = billRepository.getBillsByDateRange(startOfDay, endOfDay).firstOrNull() ?: emptyList()
        
        var totalSales = 0.0
        var cash = 0.0
        var upi = 0.0
        var other = 0.0
        
        for (bill in bills) {
            if (OrderStatus.fromDbValue(bill.orderStatus) == OrderStatus.COMPLETED) {
                totalSales += bill.totalAmount
                val mode = PaymentMode.fromDbValue(bill.paymentMode)
                when (mode) {
                    PaymentMode.CASH -> cash += bill.totalAmount
                    PaymentMode.UPI -> upi += bill.totalAmount
                    PaymentMode.PART_CASH_UPI -> {
                        cash += bill.partAmount1
                        upi += bill.partAmount2
                    }
                    else -> other += bill.totalAmount
                }
            }
        }
        
        return DailySalesReport(
            totalSales = totalSales,
            totalOrders = bills.size,
            cashCollected = cash,
            upiCollected = upi,
            otherCollected = other
        )
    }

    suspend fun getMonthlyReport(month: Int, year: Int): MonthlySalesReport {
        val monthStr = month.toString().padStart(2, '0')
        val startOfMonth = "$year-$monthStr-01 00:00:00"
        val endOfMonth = "$year-$monthStr-31 23:59:59" // Simplification for range query
        
        val bills = billRepository.getBillsByDateRange(startOfMonth, endOfMonth).firstOrNull() ?: emptyList()
        
        var totalSales = 0.0
        var completedOrders = 0
        
        for (bill in bills) {
            if (OrderStatus.fromDbValue(bill.orderStatus) == OrderStatus.COMPLETED) {
                totalSales += bill.totalAmount
                completedOrders++
            }
        }
        
        return MonthlySalesReport(
            month = month,
            year = year,
            totalSales = totalSales,
            totalOrders = completedOrders
        )
    }

    suspend fun getTopSellingItems(from: String, to: String, limit: Int): List<TopSellingItem> {
        val startMillis = com.khanabook.lite.pos.domain.util.DateUtils.getStartOfDay(from)
        val endMillis = com.khanabook.lite.pos.domain.util.DateUtils.getEndOfDay(to)
        return billRepository.getTopSellingItemsInRange(startMillis, endMillis, limit)
    }
}


