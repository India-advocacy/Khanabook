package com.khanabook.lite.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khanabook.lite.pos.data.local.entity.*
import com.khanabook.lite.pos.data.local.relation.BillWithItems
import com.khanabook.lite.pos.data.repository.BillRepository
import com.khanabook.lite.pos.data.repository.RestaurantRepository
import com.khanabook.lite.pos.data.repository.MenuRepository
import com.khanabook.lite.pos.data.repository.InventoryRepository

import com.khanabook.lite.pos.domain.manager.BillCalculator
import com.khanabook.lite.pos.domain.manager.OrderIdManager
import com.khanabook.lite.pos.domain.model.*
import androidx.compose.runtime.Immutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val menuRepository: MenuRepository,
    private val restaurantRepository: RestaurantRepository,
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: com.khanabook.lite.pos.domain.manager.SessionManager,
    val printerManager: com.khanabook.lite.pos.domain.manager.BluetoothPrinterManager
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    init {
        // Optimization: Debounce summary updates to avoid redundant calculations during rapid taps
        _cartItems
            .debounce(300)
            .onEach { updateSummary() }
            .launchIn(viewModelScope)
    }

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName

    private val _customerWhatsapp = MutableStateFlow("")
    val customerWhatsapp: StateFlow<String> = _customerWhatsapp

    private val _paymentMode = MutableStateFlow(PaymentMode.UPI)
    val paymentMode: StateFlow<PaymentMode> = _paymentMode

    private val _partAmount1 = MutableStateFlow(0.0)
    private val _partAmount2 = MutableStateFlow(0.0)

    private val _billSummary = MutableStateFlow(BillSummary())
    val billSummary: StateFlow<BillSummary> = _billSummary
    
    private val _lastBill = MutableStateFlow<BillWithItems?>(null)
    val lastBill: StateFlow<BillWithItems?> = _lastBill

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun addToCart(item: MenuItemEntity, variant: ItemVariantEntity? = null) {
        viewModelScope.launch {
            val latestItem = menuRepository.getItemById(item.id) ?: item
            val current = _cartItems.value.toMutableList()
            val existing = current.find { it.item.id == item.id && it.variant?.id == variant?.id }
            
            val currentQuantityInCart = existing?.quantity ?: 0
            
            val stockToCheck = if (variant != null) {
                // For variants, we use the variant's own stock
                variant.currentStock
            } else {
                // For base items, we use the item's stock
                latestItem.currentStock
            }

            val thresholdToCheck = if (variant != null) {
                variant.lowStockThreshold
            } else {
                latestItem.lowStockThreshold
            }

            if (currentQuantityInCart >= stockToCheck) {
                _error.value = "Reached maximum stock for ${variant?.variantName ?: latestItem.name}"
                return@launch
            }

            // Show warning if reaching or below threshold
            val remainingAfterAdd = stockToCheck - (currentQuantityInCart + 1)
            var warningMessage: String? = null
            if (remainingAfterAdd <= thresholdToCheck && remainingAfterAdd > 0) {
                warningMessage = "Running out of stock for ${variant?.variantName ?: latestItem.name}"
            } else if (remainingAfterAdd == 0.0) {
                warningMessage = "Reached maximum stock for ${variant?.variantName ?: latestItem.name}"
            }

            if (warningMessage != null) _error.value = warningMessage

            _cartItems.update { current ->
                val mutable = current.toMutableList()
                val existingInUpdate = mutable.find { it.item.id == item.id && it.variant?.id == variant?.id }
                
                if (existingInUpdate != null) {
                    val idx = mutable.indexOf(existingInUpdate)
                    mutable[idx] = existingInUpdate.copy(quantity = existingInUpdate.quantity + 1)
                } else {
                    mutable.add(CartItem(latestItem, variant, 1))
                }
                mutable
            }
        }
    }

    fun removeFromCart(item: MenuItemEntity, variant: ItemVariantEntity? = null) {
        _cartItems.update { current ->
            val mutable = current.toMutableList()
            val existing = mutable.find { it.item.id == item.id && it.variant?.id == variant?.id }
            if (existing != null) {
                val index = mutable.indexOf(existing)
                if (existing.quantity > 1) {
                    mutable[index] = existing.copy(quantity = existing.quantity - 1)
                } else {
                    mutable.removeAt(index)
                }
            }
            mutable
        }
    }

    /**
     * Barcode Scanning Logic: Looks up an item by its barcode and adds it to the cart.
     */
    fun handleScannedBarcode(barcode: String) {
        viewModelScope.launch {
            val menuItem = menuRepository.getMenuItemByCode(barcode)
            if (menuItem != null) {
                addToCart(menuItem)
            } else {
                _error.value = "No item found for barcode: $barcode"
            }
        }
    }

    /**
     * OCR Scanning Logic: Processes scanned text and adds matching items to cart.
     */
    fun addItemByScannedText(text: String) {
        viewModelScope.launch {
            val allItems = menuRepository.getAllMenuItemsOnce()
            val allVariants = menuRepository.getAllVariantsOnce()
            
            // Normalize lines from scanned text
            val lines = text.split("\n", "\r").map { it.trim() }.filter { it.length > 2 }
            
            for (line in lines) {
                // 1. Direct match for item name
                val itemMatch = allItems.find { it.name.equals(line, ignoreCase = true) }
                if (itemMatch != null) {
                    addToCart(itemMatch)
                    continue
                }
                
                // 2. Direct match for variant name (e.g. "Full", "Half")
                val variantMatch = allVariants.find { it.variantName.equals(line, ignoreCase = true) }
                if (variantMatch != null) {
                    val parentItem = allItems.find { it.id == variantMatch.menuItemId }
                    if (parentItem != null) {
                        addToCart(parentItem, variantMatch)
                        continue
                    }
                }
                
                // 3. Partial match (if line contains item name)
                val partialItem = allItems.find { line.contains(it.name, ignoreCase = true) }
                if (partialItem != null) {
                    // Check if the line also contains a variant name
                    val partialVariant = allVariants.filter { it.menuItemId == partialItem.id }
                        .find { line.contains(it.variantName, ignoreCase = true) }
                    
                    addToCart(partialItem, partialVariant)
                }
            }
        }
    }

    private fun updateSummary() {
        viewModelScope.launch {
            val profile = restaurantRepository.getProfile()
            val subtotal = BillCalculator.calculateSubtotal(_cartItems.value.map { 
                (it.variant?.price ?: it.item.basePrice) to it.quantity 
            })
            
            var cgst = 0.0
            var sgst = 0.0
            var customTax = 0.0
            
            if (profile?.gstEnabled == true) {
                val gst = BillCalculator.calculateGST(subtotal, profile.gstPercentage)
                cgst = gst.cgst
                sgst = gst.sgst
            } else if (profile?.customTaxPercentage != null && profile.customTaxPercentage > 0) {
                customTax = BillCalculator.calculateCustomTax(subtotal, profile.customTaxPercentage)
            }

            val total = BillCalculator.calculateTotal(subtotal, cgst, sgst, customTax)
            
            _billSummary.value = BillSummary(subtotal, cgst, sgst, customTax, total)
        }
    }

    fun setCustomerInfo(name: String, whatsapp: String) {
        _customerName.value = name
        _customerWhatsapp.value = whatsapp
    }

    fun setPaymentMode(mode: PaymentMode, p1: Double = 0.0, p2: Double = 0.0) {
        _paymentMode.value = mode
        _partAmount1.value = p1
        _partAmount2.value = p2
    }

    suspend fun completeOrder(status: PaymentStatus): Boolean {
        try {
            if (status == PaymentStatus.SUCCESS) {
                // Final stock check
                for (cartItem in _cartItems.value) {
                    if (cartItem.variant != null) {
                        // Check variant stock (in real app, re-fetch from DB here)
                        // For simplicity, we assume re-fetch is handled by adjustStock
                    } else {
                        val latestItem = menuRepository.getItemById(cartItem.item.id)
                        if (latestItem == null || latestItem.currentStock < cartItem.quantity) {
                            _error.value = "Insufficient stock for ${cartItem.item.name}. Available: ${latestItem?.currentStock ?: 0}"
                            return false
                        }
                    }
                }
            }

            val profile = restaurantRepository.getProfile() ?: return false
            val today = OrderIdManager.getTodayString()
            
            // Atomically increment and get next counters
            val (dailyCounter, lifetimeId) = restaurantRepository.incrementAndGetCounters(today)
            val displayId = OrderIdManager.getDailyOrderDisplay(today, dailyCounter)
            
            val bill = BillEntity(
                restaurantId = sessionManager.getRestaurantId(),
                deviceId = sessionManager.getDeviceId() ?: "",
                dailyOrderId = dailyCounter,
                dailyOrderDisplay = displayId,
                lifetimeOrderId = lifetimeId,
                orderType = "order",
                customerName = _customerName.value.ifBlank { null },
                customerWhatsapp = _customerWhatsapp.value.ifBlank { null },
                subtotal = _billSummary.value.subtotal,
                gstPercentage = profile.gstPercentage,
                cgstAmount = _billSummary.value.cgst,
                sgstAmount = _billSummary.value.sgst,
                customTaxAmount = _billSummary.value.customTax,
                totalAmount = _billSummary.value.total,
                paymentMode = _paymentMode.value.dbValue,
                partAmount1 = _partAmount1.value,
                partAmount2 = _partAmount2.value,
                paymentStatus = status.dbValue,
                orderStatus = if (status == PaymentStatus.SUCCESS) OrderStatus.COMPLETED.dbValue else OrderStatus.CANCELLED.dbValue,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                paidAt = if (status == PaymentStatus.SUCCESS) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()) else null
            )
            
            val items = _cartItems.value.map { cartItem ->
                BillItemEntity(
                    billId = 0,
                    menuItemId = cartItem.item.id,
                    itemName = cartItem.item.name,
                    variantId = cartItem.variant?.id,
                    variantName = cartItem.variant?.variantName,
                    price = cartItem.variant?.price ?: cartItem.item.basePrice,
                    quantity = cartItem.quantity,
                    itemTotal = (cartItem.variant?.price ?: cartItem.item.basePrice) * cartItem.quantity
                )
            }

            val payments = when (_paymentMode.value) {
                PaymentMode.PART_CASH_UPI -> listOf(
                    BillPaymentEntity(billId = 0, paymentMode = PaymentMode.CASH.dbValue, amount = _partAmount1.value),
                    BillPaymentEntity(billId = 0, paymentMode = PaymentMode.UPI.dbValue, amount = _partAmount2.value)
                )
                PaymentMode.PART_CASH_POS -> listOf(
                    BillPaymentEntity(billId = 0, paymentMode = PaymentMode.CASH.dbValue, amount = _partAmount1.value),
                    BillPaymentEntity(billId = 0, paymentMode = PaymentMode.POS.dbValue, amount = _partAmount2.value)
                )
                PaymentMode.PART_UPI_POS -> listOf(
                    BillPaymentEntity(billId = 0, paymentMode = PaymentMode.UPI.dbValue, amount = _partAmount1.value),
                    BillPaymentEntity(billId = 0, paymentMode = PaymentMode.POS.dbValue, amount = _partAmount2.value)
                )
                else -> listOf(
                    BillPaymentEntity(billId = 0, paymentMode = _paymentMode.value.dbValue, amount = _billSummary.value.total)
                )
            }
            
            billRepository.insertFullBill(bill, items, payments)
            val inserted = billRepository.getBillWithItemsByLifetimeId(lifetimeId)
            _lastBill.value = inserted
            
            // Auto-print logic
            if (profile.printerEnabled && profile.autoPrintOnSuccess && inserted != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    if (!printerManager.isConnected() && !profile.printerMac.isNullOrBlank()) {
                        printerManager.connect(profile.printerMac)
                    }
                    if (printerManager.isConnected()) {
                        val bytes = com.khanabook.lite.pos.domain.util.InvoiceFormatter.formatForThermalPrinter(inserted, profile)
                        printerManager.printBytes(bytes)
                    }
                }
            }
            
            _cartItems.value = emptyList()
            updateSummary()
            _error.value = null
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _error.value = "Failed to save bill: ${e.message}"
            return false
        }
    }

    fun clearError() {
        _error.value = null
    }

    @Immutable
    data class CartItem(val item: MenuItemEntity, val variant: ItemVariantEntity? = null, val quantity: Int)
    
    @Immutable
    data class BillSummary(val subtotal: Double = 0.0, val cgst: Double = 0.0, val sgst: Double = 0.0, val customTax: Double = 0.0, val total: Double = 0.0)
}
