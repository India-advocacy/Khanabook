package com.khanabook.lite.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.khanabook.lite.pos.data.local.dao.*
import com.khanabook.lite.pos.data.local.entity.*

@Database(
        entities =
                [
                        UserEntity::class,
                        RestaurantProfileEntity::class,
                        CategoryEntity::class,
                        MenuItemEntity::class,
                        ItemVariantEntity::class,
                        BillEntity::class,
                        BillItemEntity::class,
                        BillPaymentEntity::class,
                        StockLogEntity::class
                ],
        version = 18,
        exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun restaurantDao(): RestaurantDao
    abstract fun categoryDao(): CategoryDao
    abstract fun menuDao(): MenuDao
    abstract fun billDao(): BillDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        const val DATABASE_NAME = "khanabook_lite_db"

        // Migration 17 to 18
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop old inventory tables
                db.execSQL("DROP TABLE IF EXISTS `raw_materials`")
                db.execSQL("DROP TABLE IF EXISTS `material_batches`")
                db.execSQL("DROP TABLE IF EXISTS `recipe_ingredients`")
                db.execSQL("DROP TABLE IF EXISTS `raw_material_stock_logs`")

                // Add inventory tracking columns to menu items and variants
                try {
                    db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `current_stock` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `low_stock_threshold` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    // Columns might already exist from a previous beta build migration
                }

                try {
                    db.execSQL("ALTER TABLE `item_variants` ADD COLUMN `current_stock` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `item_variants` ADD COLUMN `low_stock_threshold` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    // Columns might already exist
                }
            }
        }
    }
}
