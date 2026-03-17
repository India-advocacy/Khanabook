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

    suspend fun getPaymentBreakdown(from: Long, to: Long): Map<String, String> {
        val bills = billRepository.getBillsByDateRange(from, to).firstOrNull() ?: emptyList()
        val breakdown = mutableMapOf<String, java.math.BigDecimal>()
        
        for (bill in bills) {
            if (OrderStatus.fromDbValue(bill.orderStatus) != OrderStatus.COMPLETED) continue
            
            val mode = PaymentMode.fromDbValue(bill.paymentMode)
            val amount = java.math.BigDecimal(bill.totalAmount)
            val label = mode.displayLabel
            
            if (PaymentModeManager.isPartPayment(mode)) {
                val labels = PaymentModeManager.getPartLabels(mode)
                val p1 = java.math.BigDecimal(bill.partAmount1)
                val p2 = java.math.BigDecimal(bill.partAmount2)

                breakdown[labels.first] = (breakdown[labels.first] ?: java.math.BigDecimal.ZERO).add(p1)
                breakdown[labels.second] = (breakdown[labels.second] ?: java.math.BigDecimal.ZERO).add(p2)
                
                breakdown[label] = (breakdown[label] ?: java.math.BigDecimal.ZERO).add(amount)
                
                val part1Key = "${label}_part1"
                val part2Key = "${label}_part2"
                breakdown[part1Key] = (breakdown[part1Key] ?: java.math.BigDecimal.ZERO).add(p1)
                breakdown[part2Key] = (breakdown[part2Key] ?: java.math.BigDecimal.ZERO).add(p2)
            } else {
                breakdown[label] = (breakdown[label] ?: java.math.BigDecimal.ZERO).add(amount)
            }
        }
        return breakdown.mapValues { it.value.setScale(2, java.math.RoundingMode.HALF_UP).toString() }
    }

    suspend fun getOrderLevelRows(from: Long, to: Long): List<OrderLevelRow> {
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

    suspend fun getOrderDetail(billId: Long): BillWithItems? {
        return billRepository.getBillWithItemsById(billId)
    }

    suspend fun getOrderDetailsTable(from: Long, to: Long): List<OrderDetailRow> {
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
        val startOfDay = "$date 00:00:00"
        val endOfDay = "$date 23:59:59"
        val bills = billRepository.getBillsByDateRange(startOfDay, endOfDay).firstOrNull() ?: emptyList()
        
        var totalSales = java.math.BigDecimal.ZERO
        var cash = java.math.BigDecimal.ZERO
        var upi = java.math.BigDecimal.ZERO
        var other = java.math.BigDecimal.ZERO
        
        for (bill in bills) {
            if (OrderStatus.fromDbValue(bill.orderStatus) == OrderStatus.COMPLETED) {
                val amount = java.math.BigDecimal(bill.totalAmount)
                totalSales = totalSales.add(amount)
                
                val mode = PaymentMode.fromDbValue(bill.paymentMode)
                when (mode) {
                    PaymentMode.CASH -> cash = cash.add(amount)
                    PaymentMode.UPI -> upi = upi.add(amount)
                    PaymentMode.PART_CASH_UPI -> {
                        cash = cash.add(java.math.BigDecimal(bill.partAmount1))
                        upi = upi.add(java.math.BigDecimal(bill.partAmount2))
                    }
                    else -> other = other.add(amount)
                }
            }
        }
        
        return DailySalesReport(
            totalSales = totalSales.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
            totalOrders = bills.size,
            cashCollected = cash.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
            upiCollected = upi.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
            otherCollected = other.setScale(2, java.math.RoundingMode.HALF_UP).toString()
        )
    }

    suspend fun getMonthlyReport(month: Int, year: Int): MonthlySalesReport {
        val monthStr = month.toString().padStart(2, '0')
        val startOfMonth = "$year-$monthStr-01 00:00:00"
        val endOfMonth = "$year-$monthStr-31 23:59:59"
        
        val bills = billRepository.getBillsByDateRange(startOfMonth, endOfMonth).firstOrNull() ?: emptyList()
        
        var totalSales = java.math.BigDecimal.ZERO
        var completedOrders = 0
        
        for (bill in bills) {
            if (OrderStatus.fromDbValue(bill.orderStatus) == OrderStatus.COMPLETED) {
                totalSales = totalSales.add(java.math.BigDecimal(bill.totalAmount))
                completedOrders++
            }
        }
        
        return MonthlySalesReport(
            month = month,
            year = year,
            totalSales = totalSales.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
            totalOrders = completedOrders
        )
    }

    suspend fun getTopSellingItems(from: Long, to: Long, limit: Int): List<TopSellingItem> {
        return billRepository.getTopSellingItemsInRange(from, to, limit)
    }
}


