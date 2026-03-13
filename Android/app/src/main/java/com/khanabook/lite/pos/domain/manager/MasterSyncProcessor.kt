package com.khanabook.lite.pos.domain.manager

import android.util.Log
import com.khanabook.lite.pos.data.local.dao.*
import com.khanabook.lite.pos.data.local.entity.*
import com.khanabook.lite.pos.data.remote.api.KhanaBookApi
import com.khanabook.lite.pos.data.remote.api.MasterSyncResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterSyncProcessor @Inject constructor(
    private val api: KhanaBookApi,
    private val billDao: BillDao,
    private val restaurantDao: RestaurantDao,
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val menuDao: MenuDao,
    private val inventoryDao: InventoryDao
) {

    private fun String?.orFallback(default: String): String = this?.takeUnless { it.isBlank() } ?: default

    suspend fun pushAll(): Boolean {
        return try {
            // 1. PUSH CONFIG (Users & Profiles)
            val unsyncedProfiles = restaurantDao.getUnsyncedRestaurantProfiles()
            if (unsyncedProfiles.isNotEmpty()) {
                unsyncedProfiles.chunked(50).forEach { batch ->
                    val syncedIds = api.pushRestaurantProfiles(batch)
                    restaurantDao.markRestaurantProfilesAsSynced(syncedIds)
                }
            }

            val unsyncedUsers = userDao.getUnsyncedUsers()
            if (unsyncedUsers.isNotEmpty()) {
                unsyncedUsers.chunked(50).forEach { batch ->
                    val syncedIds = api.pushUsers(batch)
                    userDao.markUsersAsSynced(syncedIds)
                }
            }

            // 2. PUSH MENU (Categories, Items, Variants)
            val unsyncedCategories = categoryDao.getUnsyncedCategories()
            if (unsyncedCategories.isNotEmpty()) {
                unsyncedCategories.chunked(50).forEach { batch ->
                    val syncedIds = api.pushCategories(batch)
                    categoryDao.markCategoriesAsSynced(syncedIds)
                }
            }

            val unsyncedMenuItems = menuDao.getUnsyncedMenuItems()
            if (unsyncedMenuItems.isNotEmpty()) {
                unsyncedMenuItems.chunked(50).forEach { batch ->
                    val syncedIds = api.pushMenuItems(batch)
                    menuDao.markMenuItemsAsSynced(syncedIds)
                }
            }

            val unsyncedVariants = menuDao.getUnsyncedItemVariants()
            if (unsyncedVariants.isNotEmpty()) {
                unsyncedVariants.chunked(50).forEach { batch ->
                    val syncedIds = api.pushItemVariants(batch)
                    menuDao.markItemVariantsAsSynced(syncedIds)
                }
            }

            // 3. PUSH INVENTORY
            val unsyncedStockLogs = inventoryDao.getUnsyncedStockLogs()
            if (unsyncedStockLogs.isNotEmpty()) {
                unsyncedStockLogs.chunked(50).forEach { batch ->
                    val syncedIds = api.pushStockLogs(batch)
                    inventoryDao.markStockLogsAsSynced(syncedIds)
                }
            }

            // 4. SYNC BILLS (Push only part)
            val unsyncedBills = billDao.getUnsyncedBills()
            if (unsyncedBills.isNotEmpty()) {
                unsyncedBills.chunked(50).forEach { batch ->
                    val syncedIds = api.pushBills(batch)
                    billDao.markBillsAsSynced(syncedIds)
                }
            }

            val unsyncedBillItems = billDao.getUnsyncedBillItems()
            if (unsyncedBillItems.isNotEmpty()) {
                unsyncedBillItems.chunked(50).forEach { batch ->
                    val syncedIds = api.pushBillItems(batch)
                    billDao.markBillItemsAsSynced(syncedIds)
                }
            }

            val unsyncedBillPayments = billDao.getUnsyncedBillPayments()
            if (unsyncedBillPayments.isNotEmpty()) {
                unsyncedBillPayments.chunked(50).forEach { batch ->
                    val syncedIds = api.pushBillPayments(batch)
                    billDao.markBillPaymentsAsSynced(syncedIds)
                }
            }

            true
        } catch (e: Exception) {
            Log.e("MasterSyncProcessor", "Push failed", e)
            false
        }
    }

    suspend fun insertMasterData(masterData: MasterSyncResponse) {
        if (masterData.profiles.isNotEmpty()) {
            val currentLocalProfile = restaurantDao.getProfile()
            restaurantDao.insertSyncedRestaurantProfiles(
                masterData.profiles.map { remoteProfile ->
                    RestaurantProfileEntity(
                        id = 1,
                        shopName = remoteProfile.shopName,
                        shopAddress = remoteProfile.shopAddress,
                        whatsappNumber = remoteProfile.whatsappNumber,
                        email = remoteProfile.email,
                        logoPath = remoteProfile.logoPath,
                        fssaiNumber = remoteProfile.fssaiNumber,
                        emailInvoiceConsent = remoteProfile.emailInvoiceConsent,
                        country = remoteProfile.country?.takeUnless { it.isBlank() }
                            ?: currentLocalProfile?.country
                            ?: "India",
                        gstEnabled = remoteProfile.gstEnabled,
                        gstin = remoteProfile.gstin,
                        isTaxInclusive = remoteProfile.isTaxInclusive,
                        gstPercentage = remoteProfile.gstPercentage,
                        customTaxName = remoteProfile.customTaxName,
                        customTaxNumber = remoteProfile.customTaxNumber,
                        customTaxPercentage = remoteProfile.customTaxPercentage,
                        currency = remoteProfile.currency?.takeUnless { it.isBlank() }
                            ?: currentLocalProfile?.currency
                            ?: "INR",
                        upiEnabled = remoteProfile.upiEnabled,
                        upiQrPath = remoteProfile.upiQrPath,
                        upiHandle = remoteProfile.upiHandle,
                        upiMobile = remoteProfile.upiMobile,
                        cashEnabled = remoteProfile.cashEnabled,
                        posEnabled = remoteProfile.posEnabled,
                        zomatoEnabled = remoteProfile.zomatoEnabled,
                        swiggyEnabled = remoteProfile.swiggyEnabled,
                        ownWebsiteEnabled = remoteProfile.ownWebsiteEnabled,
                        printerEnabled = remoteProfile.printerEnabled,
                        printerName = remoteProfile.printerName,
                        printerMac = remoteProfile.printerMac,
                        paperSize = remoteProfile.paperSize.takeUnless { it.isBlank() }
                            ?: currentLocalProfile?.paperSize
                            ?: "58mm",
                        autoPrintOnSuccess = remoteProfile.autoPrintOnSuccess,
                        includeLogoInPrint = remoteProfile.includeLogoInPrint,
                        printCustomerWhatsapp = remoteProfile.printCustomerWhatsapp,
                        // Keep local counters to avoid multi-device ID conflicts.
                        dailyOrderCounter = currentLocalProfile?.dailyOrderCounter ?: remoteProfile.dailyOrderCounter,
                        lifetimeOrderCounter = currentLocalProfile?.lifetimeOrderCounter ?: remoteProfile.lifetimeOrderCounter,
                        lastResetDate = currentLocalProfile?.lastResetDate ?: remoteProfile.lastResetDate,
                        sessionTimeoutMinutes = remoteProfile.sessionTimeoutMinutes,
                        restaurantId = remoteProfile.restaurantId,
                        deviceId = remoteProfile.deviceId,
                        isSynced = true,
                        updatedAt = remoteProfile.updatedAt,
                        isDeleted = remoteProfile.isDeleted
                    )
                }
            )
        }
        if (masterData.users.isNotEmpty()) {
            val localUsersByEmail = userDao.getAllUsersOnce().associateBy { it.email }
            userDao.insertSyncedUsers(
                masterData.users.map { remoteUser ->
                    val localUser = localUsersByEmail[remoteUser.email]
                    // Use remote hash if valid; fall back to local hash; sentinel if neither exists
                    val resolvedPasswordHash = remoteUser.passwordHash
                        .takeUnless { it.isNullOrBlank() }
                        ?: localUser?.passwordHash
                        ?: "SYNC_IMPORTED_USER"

                    UserEntity(
                        id = remoteUser.id,
                        name = remoteUser.name,
                        email = remoteUser.email,
                        passwordHash = resolvedPasswordHash,
                        whatsappNumber = remoteUser.whatsappNumber ?: localUser?.whatsappNumber,
                        isActive = remoteUser.isActive,
                        createdAt = remoteUser.createdAt.orFallback(""),
                        restaurantId = remoteUser.restaurantId,
                        deviceId = remoteUser.deviceId,
                        isSynced = true
                    )
                }
            )
        }
        if (masterData.categories.isNotEmpty()) {
            categoryDao.insertSyncedCategories(
                masterData.categories.map { remoteCategory ->
                    CategoryEntity(
                        id = remoteCategory.id,
                        name = remoteCategory.name.orFallback("Uncategorized"),
                        isVeg = remoteCategory.isVeg,
                        sortOrder = remoteCategory.sortOrder,
                        isActive = remoteCategory.isActive,
                        createdAt = remoteCategory.createdAt.orFallback(""),
                        restaurantId = remoteCategory.restaurantId,
                        deviceId = remoteCategory.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteCategory.updatedAt,
                        isDeleted = remoteCategory.isDeleted
                    )
                }
            )
        }
        if (masterData.menuItems.isNotEmpty()) {
            menuDao.insertSyncedMenuItems(
                masterData.menuItems.map { remoteMenuItem ->
                    MenuItemEntity(
                        id = remoteMenuItem.id,
                        categoryId = remoteMenuItem.categoryId,
                        name = remoteMenuItem.name.orFallback("Unnamed Item"),
                        basePrice = remoteMenuItem.basePrice,
                        foodType = remoteMenuItem.foodType.orFallback("veg"),
                        description = remoteMenuItem.description,
                        isAvailable = remoteMenuItem.isAvailable,
                        currentStock = remoteMenuItem.currentStock,
                        lowStockThreshold = remoteMenuItem.lowStockThreshold,
                        createdAt = remoteMenuItem.createdAt.orFallback(""),
                        restaurantId = remoteMenuItem.restaurantId,
                        deviceId = remoteMenuItem.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteMenuItem.updatedAt,
                        isDeleted = remoteMenuItem.isDeleted
                    )
                }
            )
        }
        if (masterData.itemVariants.isNotEmpty()) {
            menuDao.insertSyncedItemVariants(
                masterData.itemVariants.map { remoteVariant ->
                    ItemVariantEntity(
                        id = remoteVariant.id,
                        menuItemId = remoteVariant.menuItemId,
                        variantName = remoteVariant.variantName.orFallback("Default"),
                        price = remoteVariant.price,
                        isAvailable = remoteVariant.isAvailable,
                        sortOrder = remoteVariant.sortOrder,
                        currentStock = remoteVariant.currentStock,
                        lowStockThreshold = remoteVariant.lowStockThreshold,
                        restaurantId = remoteVariant.restaurantId,
                        deviceId = remoteVariant.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteVariant.updatedAt,
                        isDeleted = remoteVariant.isDeleted
                    )
                }
            )
        }
        if (masterData.stockLogs.isNotEmpty()) {
            inventoryDao.insertSyncedStockLogs(
                masterData.stockLogs.map { remoteStockLog ->
                    StockLogEntity(
                        id = remoteStockLog.id,
                        menuItemId = remoteStockLog.menuItemId,
                        variantId = remoteStockLog.variantId,
                        delta = remoteStockLog.delta,
                        reason = remoteStockLog.reason.orFallback("adjustment"),
                        createdAt = remoteStockLog.createdAt.orFallback(""),
                        restaurantId = remoteStockLog.restaurantId,
                        deviceId = remoteStockLog.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteStockLog.updatedAt,
                        isDeleted = remoteStockLog.isDeleted
                    )
                }
            )
        }
        if (masterData.bills.isNotEmpty()) {
            billDao.insertSyncedBills(
                masterData.bills.map { remoteBill ->
                    BillEntity(
                        id = remoteBill.id,
                        restaurantId = remoteBill.restaurantId,
                        deviceId = remoteBill.deviceId.orFallback(""),
                        dailyOrderId = remoteBill.dailyOrderId,
                        dailyOrderDisplay = remoteBill.dailyOrderDisplay.orFallback(""),
                        lifetimeOrderId = remoteBill.lifetimeOrderId,
                        orderType = remoteBill.orderType.orFallback("order"),
                        customerName = remoteBill.customerName,
                        customerWhatsapp = remoteBill.customerWhatsapp,
                        subtotal = remoteBill.subtotal,
                        gstPercentage = remoteBill.gstPercentage,
                        cgstAmount = remoteBill.cgstAmount,
                        sgstAmount = remoteBill.sgstAmount,
                        customTaxAmount = remoteBill.customTaxAmount,
                        totalAmount = remoteBill.totalAmount,
                        paymentMode = remoteBill.paymentMode.orFallback("cash"),
                        partAmount1 = remoteBill.partAmount1,
                        partAmount2 = remoteBill.partAmount2,
                        paymentStatus = remoteBill.paymentStatus.orFallback("success"),
                        orderStatus = remoteBill.orderStatus.orFallback("completed"),
                        createdBy = remoteBill.createdBy,
                        createdAt = remoteBill.createdAt.orFallback(""),
                        paidAt = remoteBill.paidAt,
                        isSynced = true,
                        updatedAt = remoteBill.updatedAt,
                        isDeleted = remoteBill.isDeleted
                    )
                }
            )
        }
        if (masterData.billItems.isNotEmpty()) {
            billDao.insertSyncedBillItems(
                masterData.billItems.map { remoteBillItem ->
                    BillItemEntity(
                        id = remoteBillItem.id,
                        billId = remoteBillItem.billId,
                        menuItemId = remoteBillItem.menuItemId,
                        itemName = remoteBillItem.itemName.orFallback("Unnamed Item"),
                        variantId = remoteBillItem.variantId,
                        variantName = remoteBillItem.variantName,
                        price = remoteBillItem.price,
                        quantity = remoteBillItem.quantity,
                        itemTotal = remoteBillItem.itemTotal,
                        specialInstruction = remoteBillItem.specialInstruction,
                        restaurantId = remoteBillItem.restaurantId,
                        deviceId = remoteBillItem.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteBillItem.updatedAt,
                        isDeleted = remoteBillItem.isDeleted
                    )
                }
            )
        }
        if (masterData.billPayments.isNotEmpty()) {
            billDao.insertSyncedBillPayments(
                masterData.billPayments.map { remoteBillPayment ->
                    BillPaymentEntity(
                        id = remoteBillPayment.id,
                        billId = remoteBillPayment.billId,
                        paymentMode = remoteBillPayment.paymentMode.orFallback("cash"),
                        amount = remoteBillPayment.amount,
                        restaurantId = remoteBillPayment.restaurantId,
                        deviceId = remoteBillPayment.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteBillPayment.updatedAt,
                        isDeleted = remoteBillPayment.isDeleted
                    )
                }
            )
        }
    }
}
