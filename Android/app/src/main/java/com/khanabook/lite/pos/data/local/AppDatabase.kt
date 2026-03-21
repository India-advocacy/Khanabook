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
        version = 23,
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

        
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                
                db.execSQL("DROP TABLE IF EXISTS `raw_materials`")
                db.execSQL("DROP TABLE IF EXISTS `material_batches`")
                db.execSQL("DROP TABLE IF EXISTS `recipe_ingredients`")
                db.execSQL("DROP TABLE IF EXISTS `raw_material_stock_logs`")

                
                try {
                    db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `current_stock` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `low_stock_threshold` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    
                }

                try {
                    db.execSQL("ALTER TABLE `item_variants` ADD COLUMN `current_stock` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `item_variants` ADD COLUMN `low_stock_threshold` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    
                }
            }
        }
        
        
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                
                try {
                    db.execSQL("ALTER TABLE `restaurant_profile` ADD COLUMN `country` TEXT DEFAULT 'India'")
                } catch (e: Exception) {
                    db.execSQL("UPDATE `restaurant_profile` SET `country` = 'India' WHERE `country` IS NULL")
                }

                try {
                    db.execSQL("ALTER TABLE `restaurant_profile` ADD COLUMN `currency` TEXT DEFAULT 'INR'")
                } catch (e: Exception) {
                    db.execSQL("UPDATE `restaurant_profile` SET `currency` = 'INR' WHERE `currency` IS NULL")
                }
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'owner'")
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `pin_hash` TEXT")
                } catch (e: Exception) {
                    // Ignore if columns exist
                }
            }
        }
    }
}
