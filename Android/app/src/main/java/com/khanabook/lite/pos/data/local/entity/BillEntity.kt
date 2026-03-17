package com.khanabook.lite.pos.data.local.entity

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(
        tableName = "bills",
        foreignKeys =
                [
                        ForeignKey(
                                entity = UserEntity::class,
                                parentColumns = ["id"],
                                childColumns = ["created_by"],
                                onDelete = ForeignKey.SET_NULL
                        )],
        indices = [
            Index(value = ["created_by"]),
            Index(value = ["order_status"]),
            Index(value = ["created_at"]),
            Index(value = ["daily_order_id"])
        ]
)
data class BillEntity(
        @SerializedName("localId") @PrimaryKey(autoGenerate = true) val id: Long = 0,
        @ColumnInfo(name = "restaurant_id", defaultValue = "0") val restaurantId: Long = 0,
        @ColumnInfo(name = "device_id", defaultValue = "''") val deviceId: String = "",
        @ColumnInfo(name = "daily_order_id") val dailyOrderId: Long,
        @ColumnInfo(name = "daily_order_display") val dailyOrderDisplay: String, 
        @ColumnInfo(name = "lifetime_order_id") val lifetimeOrderId: Long, 
        @ColumnInfo(name = "order_type", defaultValue = "order")
        val orderType: String = "order", 
        @ColumnInfo(name = "customer_name") val customerName: String? = null,
        @ColumnInfo(name = "customer_whatsapp") val customerWhatsapp: String? = null,
        val subtotal: String,
        @ColumnInfo(name = "gst_percentage", defaultValue = "'0.0'") val gstPercentage: String = "0.0",
        @ColumnInfo(name = "cgst_amount", defaultValue = "'0.0'") val cgstAmount: String = "0.0",
        @ColumnInfo(name = "sgst_amount", defaultValue = "'0.0'") val sgstAmount: String = "0.0",
        @ColumnInfo(name = "custom_tax_amount", defaultValue = "'0.0'")
        val customTaxAmount: String = "0.0",
        @ColumnInfo(name = "total_amount") val totalAmount: String,
        @ColumnInfo(name = "payment_mode")
        val paymentMode:
                String, 
        @ColumnInfo(name = "part_amount_1", defaultValue = "'0.0'") val partAmount1: String = "0.0",
        @ColumnInfo(name = "part_amount_2", defaultValue = "'0.0'") val partAmount2: String = "0.0",
        @ColumnInfo(name = "payment_status") val paymentStatus: String, 
        @ColumnInfo(name = "order_status")
        val orderStatus: String, 
        @ColumnInfo(name = "created_by") val createdBy: Long? = null,
        @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "paid_at") val paidAt: Long? = null,

        
        @ColumnInfo(name = "is_synced", defaultValue = "0") val isSynced: Boolean = false,
        @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false
)
