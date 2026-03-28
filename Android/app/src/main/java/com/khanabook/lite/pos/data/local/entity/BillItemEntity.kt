package com.khanabook.lite.pos.data.local.entity


import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["bill_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menu_item_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ItemVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["bill_id"]),
        Index(value = ["menu_item_id"]),
        Index(value = ["variant_id"])
    ]
)
data class BillItemEntity(
    @SerializedName("localId") @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bill_id")
    val billId: Long,
    @ColumnInfo(name = "menu_item_id")
    val menuItemId: Long?,
    @ColumnInfo(name = "item_name")
    val itemName: String, 
    @ColumnInfo(name = "variant_id")
    val variantId: Long? = null,
    @ColumnInfo(name = "variant_name")
    val variantName: String? = null, 
    val price: String, 
    val quantity: Int,
    @ColumnInfo(name = "item_total")
    val itemTotal: String, 
    @ColumnInfo(name = "special_instruction")
    val specialInstruction: String? = null,

    @ColumnInfo(name = "restaurant_id", defaultValue = "0") val restaurantId: Long = 0,
    @ColumnInfo(name = "device_id", defaultValue = "''") val deviceId: String = "",
    @ColumnInfo(name = "is_synced", defaultValue = "0") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis()
,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @SerializedName("serverId") @ColumnInfo(name = "server_id") val serverId: Long? = null
)


