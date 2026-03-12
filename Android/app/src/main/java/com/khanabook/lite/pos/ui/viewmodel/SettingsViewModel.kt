package com.khanabook.lite.pos.ui.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khanabook.lite.pos.data.local.entity.*
import com.khanabook.lite.pos.data.repository.*
import com.khanabook.lite.pos.data.local.relation.MenuWithVariants
import com.khanabook.lite.pos.domain.manager.BluetoothPrinterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val categoryRepository: CategoryRepository,
    private val menuRepository: MenuRepository,
    private val userRepository: UserRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    val profile: StateFlow<RestaurantProfileEntity?> = restaurantRepository.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var btManager: BluetoothPrinterManager? = null

    private val _btDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val btDevices: StateFlow<List<BluetoothDevice>> = _btDevices.asStateFlow()

    private val _btIsScanning = MutableStateFlow(false)
    val btIsScanning: StateFlow<Boolean> = _btIsScanning.asStateFlow()

    private val _btIsConnecting = MutableStateFlow(false)
    val btIsConnecting: StateFlow<Boolean> = _btIsConnecting.asStateFlow()

    private val _btConnectResult = MutableStateFlow<Boolean?>(null)
    val btConnectResult: StateFlow<Boolean?> = _btConnectResult.asStateFlow()

    fun initBluetooth(context: Context) {
        if (btManager == null) {
            btManager = BluetoothPrinterManager(context)
            // Auto-reconnect if mac exists
            viewModelScope.launch(Dispatchers.IO) {
                val mac = restaurantRepository.getProfile()?.printerMac
                if (!mac.isNullOrBlank() && btManager?.isConnected() == false) {
                    btManager?.connect(mac)
                }
            }
        }
    }

    fun testPrint() {
        val mgr = btManager ?: return
        if (!mgr.isConnected()) {
            _btConnectResult.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val testData = (
                "\u001b\u0040" + // Initialize
                "\u001b\u0061\u0001" + // Center
                "KHANABOOK\n" +
                "PRINTER TEST OK\n" +
                "--------------------------------\n" +
                "\n\n\n\n" +
                "\u001d\u0056\u0042\u0000" // Cut
            ).toByteArray(Charsets.US_ASCII)
            mgr.printBytes(testData)
        }
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        initBluetooth(context)
        return btManager?.isBluetoothEnabled() == true
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        initBluetooth(context)
        return btManager?.hasRequiredPermissions() == true
    }

    fun startBluetoothScan(context: Context) {
        initBluetooth(context)
        val mgr = btManager ?: return
        viewModelScope.launch {
            mgr.scannedDevices.collect { _btDevices.value = it }
        }
        viewModelScope.launch {
            mgr.isScanning.collect { _btIsScanning.value = it }
        }
        mgr.startScan()
    }

    fun stopBluetoothScan() {
        btManager?.stopScan()
        _btIsScanning.value = false
    }

    @Suppress("MissingPermission")
    fun connectToPrinter(context: Context, device: BluetoothDevice) {
        initBluetooth(context)
        val mgr = btManager ?: return
        _btConnectResult.value = null
        _btIsConnecting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val ok = mgr.connect(device)
            _btIsConnecting.value = false
            _btConnectResult.value = ok
            if (ok) {
                val name = try { device.name ?: "BT Printer" } catch (_: Exception) { "BT Printer" }
                val mac  = device.address
                val current = restaurantRepository.getProfile()
                current?.copy(printerName = name, printerMac = mac)?.let {
                    restaurantRepository.saveProfile(it)
                }
            }
        }
    }

    fun clearBtConnectResult() {
        _btConnectResult.value = null
    }

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(profile: RestaurantProfileEntity) {
        viewModelScope.launch {
            restaurantRepository.saveProfile(profile)
            userRepository.updateAdminPhoneNumber(profile.whatsappNumber ?: "")
            
            userRepository.currentUser.value?.let { current ->
                userRepository.setCurrentUser(current.copy(
                    email = profile.whatsappNumber ?: "",
                    whatsappNumber = profile.whatsappNumber
                ))
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            categoryRepository.insertCategory(CategoryEntity(name = name, isVeg = true, createdAt = now))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun addItem(categoryId: Int, name: String, price: Double, type: String, stock: Double) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            menuRepository.insertItem(MenuItemEntity(
                categoryId = categoryId,
                name = name,
                basePrice = price,
                foodType = type,
                currentStock = stock,
                createdAt = now
            ))
        }
    }

    fun updateItem(item: MenuItemEntity) {
        viewModelScope.launch {
            menuRepository.updateItem(item)
        }
    }

    fun toggleItemAvailability(id: Int, isAvailable: Boolean) {
        viewModelScope.launch {
            menuRepository.toggleItemAvailability(id, isAvailable)
        }
    }

    fun deleteItem(item: MenuItemEntity) {
        viewModelScope.launch {
            menuRepository.deleteItem(item)
        }
    }

    fun resetDailyCounter() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            restaurantRepository.resetDailyCounter(0, today)
        }
    }

    fun getItemsByCategory(categoryId: Int) = menuRepository.getItemsByCategoryFlow(categoryId)

    fun getMenuWithVariantsByCategory(categoryId: Int): kotlinx.coroutines.flow.Flow<List<MenuWithVariants>> {
        return menuRepository.getMenuWithVariantsByCategoryFlow(categoryId)
    }

    fun updateItemThreshold(itemId: Int, threshold: Double) {
        viewModelScope.launch {
            inventoryRepository.updateThreshold(itemId, threshold)
        }
    }

    fun updateItemStock(itemId: Int, stock: Double) {
        viewModelScope.launch {
            inventoryRepository.adjustStock(itemId, stock - (menuRepository.getItemOnce(itemId)?.currentStock ?: 0.0), "Manual adjustment in Settings")
        }
    }

    fun updateVariantThreshold(variantId: Int, threshold: Double) {
        viewModelScope.launch {
            inventoryRepository.updateVariantThreshold(variantId, threshold)
        }
    }
}
