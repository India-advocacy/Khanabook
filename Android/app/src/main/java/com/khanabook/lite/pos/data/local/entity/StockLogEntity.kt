package com.khanabook.lite.pos.data.local.entity

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "stock_logs",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menu_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["menu_item_id"])]
)
data class StockLogEntity(
    @SerializedName("localId") @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "menu_item_id")
    val menuItemId: Int,
    @ColumnInfo(name = "variant_id")
    val variantId: Int? = null,
    val delta: String,
    val reason: String, // 'sale', 'adjustment', 'initial'
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "restaurant_id", defaultValue = "0") val restaurantId: Long = 0,
    @ColumnInfo(name = "device_id", defaultValue = "''") val deviceId: String = "",
    @ColumnInfo(name = "is_synced", defaultValue = "0") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false
)
