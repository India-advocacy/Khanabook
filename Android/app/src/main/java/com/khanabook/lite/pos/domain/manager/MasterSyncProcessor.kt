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
                    val response = api.pushRestaurantProfiles(batch)
                    restaurantDao.markRestaurantProfilesAsSynced(response.successfulLocalIds)
                }
            }

            val unsyncedUsers = userDao.getUnsyncedUsers()
            if (unsyncedUsers.isNotEmpty()) {
                unsyncedUsers.chunked(50).forEach { batch ->
                    val response = api.pushUsers(batch)
                    userDao.markUsersAsSynced(response.successfulLocalIds)
                }
            }

            // 2. PUSH MENU (Categories, Items, Variants)
            val unsyncedCategories = categoryDao.getUnsyncedCategories()
            if (unsyncedCategories.isNotEmpty()) {
                unsyncedCategories.chunked(50).forEach { batch ->
                    val response = api.pushCategories(batch)
                    categoryDao.markCategoriesAsSynced(response.successfulLocalIds)
                }
            }

            val unsyncedMenuItems = menuDao.getUnsyncedMenuItems()
            if (unsyncedMenuItems.isNotEmpty()) {
                unsyncedMenuItems.chunked(50).forEach { batch ->
                    val response = api.pushMenuItems(batch)
                    menuDao.markMenuItemsAsSynced(response.successfulLocalIds)
                }
            }

            val unsyncedVariants = menuDao.getUnsyncedItemVariants()
            if (unsyncedVariants.isNotEmpty()) {
                unsyncedVariants.chunked(50).forEach { batch ->
                    val response = api.pushItemVariants(batch)
                    menuDao.markItemVariantsAsSynced(response.successfulLocalIds)
                }
            }

            // 3. PUSH INVENTORY
            val unsyncedStockLogs = inventoryDao.getUnsyncedStockLogs()
            if (unsyncedStockLogs.isNotEmpty()) {
                unsyncedStockLogs.chunked(50).forEach { batch ->
                    val response = api.pushStockLogs(batch)
                    inventoryDao.markStockLogsAsSynced(response.successfulLocalIds)
                }
            }

            // 4. SYNC BILLS (Push only part)
            val unsyncedBills = billDao.getUnsyncedBills()
            if (unsyncedBills.isNotEmpty()) {
                unsyncedBills.chunked(50).forEach { batch ->
                    val response = api.pushBills(batch)
                    billDao.markBillsAsSynced(response.successfulLocalIds)
                }
            }

            val unsyncedBillItems = billDao.getUnsyncedBillItems()
            if (unsyncedBillItems.isNotEmpty()) {
                unsyncedBillItems.chunked(50).forEach { batch ->
                    val response = api.pushBillItems(batch)
                    billDao.markBillItemsAsSynced(response.successfulLocalIds)
                }
            }

            val unsyncedBillPayments = billDao.getUnsyncedBillPayments()
            if (unsyncedBillPayments.isNotEmpty()) {
                unsyncedBillPayments.chunked(50).forEach { batch ->
                    val response = api.pushBillPayments(batch)
                    billDao.markBillPaymentsAsSynced(response.successfulLocalIds)
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
                        shopName = remoteProfile.shopName.orFallback("My Shop"),
                        shopAddress = remoteProfile.shopAddress.orFallback(""),
                        whatsappNumber = remoteProfile.whatsappNumber.orFallback(""),
                        email = remoteProfile.email.orFallback(""),
                        logoPath = remoteProfile.logoPath, // Keep null if null to handle download later
                        fssaiNumber = remoteProfile.fssaiNumber.orFallback(""),
                        emailInvoiceConsent = remoteProfile.emailInvoiceConsent ?: false,
                        country = remoteProfile.country.orFallback(currentLocalProfile?.country ?: "India"),
                        gstEnabled = remoteProfile.gstEnabled ?: false,
                        gstin = remoteProfile.gstin.orFallback(""),
                        isTaxInclusive = remoteProfile.isTaxInclusive ?: false,
                        gstPercentage = remoteProfile.gstPercentage ?: 0.0,
                        customTaxName = remoteProfile.customTaxName.orFallback(""),
                        customTaxNumber = remoteProfile.customTaxNumber.orFallback(""),
                        customTaxPercentage = remoteProfile.customTaxPercentage ?: 0.0,
                        currency = remoteProfile.currency.orFallback(currentLocalProfile?.currency ?: "INR"),
                        upiEnabled = remoteProfile.upiEnabled ?: false,
                        upiQrPath = remoteProfile.upiQrPath,
                        upiHandle = remoteProfile.upiHandle.orFallback(""),
                        upiMobile = remoteProfile.upiMobile.orFallback(""),
                        cashEnabled = remoteProfile.cashEnabled ?: true,
                        posEnabled = remoteProfile.posEnabled ?: false,
                        zomatoEnabled = remoteProfile.zomatoEnabled ?: false,
                        swiggyEnabled = remoteProfile.swiggyEnabled ?: false,
                        ownWebsiteEnabled = remoteProfile.ownWebsiteEnabled ?: false,
                        printerEnabled = remoteProfile.printerEnabled ?: false,
                        printerName = remoteProfile.printerName.orFallback(""),
                        printerMac = remoteProfile.printerMac.orFallback(""),
                        paperSize = remoteProfile.paperSize.orFallback(currentLocalProfile?.paperSize ?: "58mm"),
                        autoPrintOnSuccess = remoteProfile.autoPrintOnSuccess ?: false,
                        includeLogoInPrint = remoteProfile.includeLogoInPrint ?: true,
                        printCustomerWhatsapp = remoteProfile.printCustomerWhatsapp ?: true,
                        dailyOrderCounter = currentLocalProfile?.dailyOrderCounter ?: remoteProfile.dailyOrderCounter,
                        lifetimeOrderCounter = currentLocalProfile?.lifetimeOrderCounter ?: remoteProfile.lifetimeOrderCounter,
                        lastResetDate = currentLocalProfile?.lastResetDate ?: remoteProfile.lastResetDate.orFallback(""),
                        sessionTimeoutMinutes = remoteProfile.sessionTimeoutMinutes ?: 30,
                        restaurantId = remoteProfile.restaurantId ?: 0L,
                        deviceId = remoteProfile.deviceId.orFallback("unknown_device"),
                        isSynced = true,
                        updatedAt = remoteProfile.updatedAt,
                        isDeleted = remoteProfile.isDeleted ?: false
                    )
                }
            )
        }

        if (masterData.users.isNotEmpty()) {
            val localUsersByEmail = userDao.getAllUsersOnce().associateBy { it.email }
            userDao.insertSyncedUsers(
                masterData.users.map { remoteUser ->
                    val localUser = localUsersByEmail[remoteUser.email]
                    val resolvedPasswordHash = remoteUser.passwordHash
                        .takeUnless { it.isNullOrBlank() }
                        ?: localUser?.passwordHash
                        ?: "SYNC_IMPORTED_USER"

                    UserEntity(
                        id = remoteUser.id,
                        name = remoteUser.name.orFallback("User"),
                        email = remoteUser.email.orFallback(""),
                        passwordHash = resolvedPasswordHash,
                        whatsappNumber = remoteUser.whatsappNumber ?: localUser?.whatsappNumber ?: "",
                        isActive = remoteUser.isActive ?: true,
                        createdAt = remoteUser.createdAt ?: System.currentTimeMillis(),
                        restaurantId = remoteUser.restaurantId ?: 0L,
                        deviceId = remoteUser.deviceId.orFallback(""),
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
                        name = remoteCategory.name.orFallback("Category"),
                        isVeg = remoteCategory.isVeg,
                        sortOrder = remoteCategory.sortOrder ?: 0,
                        createdAt = remoteCategory.createdAt ?: System.currentTimeMillis(),
                        restaurantId = remoteCategory.restaurantId ?: 0L,
                        deviceId = remoteCategory.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteCategory.updatedAt,
                        isDeleted = remoteCategory.isDeleted ?: false
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
                        basePrice = (remoteMenuItem.basePrice ?: 0.0).toString(),
                        foodType = remoteMenuItem.foodType.orFallback("veg"),
                        description = remoteMenuItem.description,
                        isAvailable = remoteMenuItem.isAvailable ?: true,
                        currentStock = (remoteMenuItem.currentStock ?: 0.0).toString(),
                        lowStockThreshold = (remoteMenuItem.lowStockThreshold ?: 0.0).toString(),
                        createdAt = remoteMenuItem.createdAt ?: System.currentTimeMillis(),
                        restaurantId = remoteMenuItem.restaurantId ?: 0L,
                        deviceId = remoteMenuItem.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteMenuItem.updatedAt,
                        isDeleted = remoteMenuItem.isDeleted ?: false
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
                        price = (remoteVariant.price ?: 0.0).toString(),
                        isAvailable = remoteVariant.isAvailable ?: true,
                        sortOrder = remoteVariant.sortOrder ?: 0,
                        currentStock = (remoteVariant.currentStock ?: 0.0).toString(),
                        lowStockThreshold = (remoteVariant.lowStockThreshold ?: 0.0).toString(),
                        restaurantId = remoteVariant.restaurantId ?: 0L,
                        deviceId = remoteVariant.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteVariant.updatedAt,
                        isDeleted = remoteVariant.isDeleted ?: false
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
                        delta = (remoteStockLog.delta ?: 0.0).toString(),
                        reason = remoteStockLog.reason.orFallback("adjustment"),
                        createdAt = remoteStockLog.createdAt ?: System.currentTimeMillis(),
                        restaurantId = remoteStockLog.restaurantId ?: 0L,
                        deviceId = remoteStockLog.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteStockLog.updatedAt,
                        isDeleted = remoteStockLog.isDeleted ?: false
                    )
                }
            )
        }

        if (masterData.bills.isNotEmpty()) {
            billDao.insertSyncedBills(
                masterData.bills.map { remoteBill ->
                    BillEntity(
                        id = remoteBill.id,
                        restaurantId = remoteBill.restaurantId ?: 0L,
                        deviceId = remoteBill.deviceId.orFallback(""),
                        dailyOrderId = remoteBill.dailyOrderId ?: 0,
                        dailyOrderDisplay = remoteBill.dailyOrderDisplay.orFallback(""),
                        lifetimeOrderId = remoteBill.lifetimeOrderId ?: 0,
                        orderType = remoteBill.orderType.orFallback("order"),
                        customerName = remoteBill.customerName,
                        customerWhatsapp = remoteBill.customerWhatsapp,
                        subtotal = (remoteBill.subtotal ?: 0.0).toString(),
                        gstPercentage = (remoteBill.gstPercentage ?: 0.0).toString(),
                        cgstAmount = (remoteBill.cgstAmount ?: 0.0).toString(),
                        sgstAmount = (remoteBill.sgstAmount ?: 0.0).toString(),
                        customTaxAmount = (remoteBill.customTaxAmount ?: 0.0).toString(),
                        totalAmount = (remoteBill.totalAmount ?: 0.0).toString(),
                        paymentMode = remoteBill.paymentMode.orFallback("cash"),
                        partAmount1 = (remoteBill.partAmount1 ?: 0.0).toString(),
                        partAmount2 = (remoteBill.partAmount2 ?: 0.0).toString(),
                        paymentStatus = remoteBill.paymentStatus.orFallback("success"),
                        orderStatus = remoteBill.orderStatus.orFallback("completed"),
                        createdBy = remoteBill.createdBy?.toInt(),
                        createdAt = remoteBill.createdAt ?: System.currentTimeMillis(),
                        paidAt = remoteBill.paidAt,
                        isSynced = true,
                        updatedAt = remoteBill.updatedAt,
                        isDeleted = remoteBill.isDeleted ?: false
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
                        price = (remoteBillItem.price ?: 0.0).toString(),
                        quantity = remoteBillItem.quantity?.toInt() ?: 0,
                        itemTotal = (remoteBillItem.itemTotal ?: 0.0).toString(),
                        specialInstruction = remoteBillItem.specialInstruction,
                        restaurantId = remoteBillItem.restaurantId ?: 0L,
                        deviceId = remoteBillItem.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteBillItem.updatedAt,
                        isDeleted = remoteBillItem.isDeleted ?: false
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
                        amount = (remoteBillPayment.amount ?: 0.0).toString(),
                        restaurantId = remoteBillPayment.restaurantId ?: 0L,
                        deviceId = remoteBillPayment.deviceId.orFallback(""),
                        isSynced = true,
                        updatedAt = remoteBillPayment.updatedAt,
                        isDeleted = remoteBillPayment.isDeleted ?: false
                    )
                }
            )
        }
    }
}
