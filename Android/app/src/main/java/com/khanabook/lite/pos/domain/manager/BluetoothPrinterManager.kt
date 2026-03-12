package com.khanabook.lite.pos.domain.manager

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth printer scanning, pairing, and printing.
 * Handles both paired (bonded) devices and live BT discovery.
 */
class BluetoothPrinterManager(private val context: Context) {

    companion object {
        // Standard Serial Port Profile (SPP) UUID — used by virtually all BT thermal printers
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // —— State —————————————————————————————————————————————————————————————————————

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    // —— Permission helpers ————————————————————————————————————————————————————————

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    // —— Discovery ———————————————————————————————————————————————————————————————————

    /**
     * Returns all devices currently bonded (paired) to the phone.
     * This never requires BLUETOOTH_SCAN — only BLUETOOTH_CONNECT.
     */
    @Suppress("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasRequiredPermissions()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    // BroadcastReceiver for live discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    device?.let { found ->
                        val current = _scannedDevices.value.toMutableList()
                        if (current.none { it.address == found.address }) {
                            current.add(found)
                            _scannedDevices.value = current
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Starts BT discovery. Populates [scannedDevices] live.
     * Pre-fills with already-paired devices so they always appear at the top.
     */
    @Suppress("MissingPermission")
    fun startScan() {
        if (!hasRequiredPermissions() || !isBluetoothEnabled()) return

        // Always start with paired devices already visible
        val paired = getPairedDevices()
        _scannedDevices.value = paired.toMutableList()
        _isScanning.value = true

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        
        // Prevent duplicate registration
        try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(discoveryReceiver, filter)
        }

        bluetoothAdapter?.cancelDiscovery()
        bluetoothAdapter?.startDiscovery()
    }

    /** Stops an ongoing BT scan. */
    @Suppress("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
        try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
    }

    // —— Connection ——————————————————————————————————————————————————————————————————

    /**
     * Connects to the chosen [device] over RFCOMM/SPP.
     * Tries secure first, then insecure as fallback (common for cheap printers).
     */
    @Suppress("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        disconnect()
        _isConnecting.value = true
        return try {
            bluetoothAdapter?.cancelDiscovery()
            
            // Try secure first
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            } catch (e: Exception) {
                socket?.close()
                // Fallback to insecure
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            }

            activeSocket = socket
            outputStream = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            false
        } finally {
            _isConnecting.value = false
        }
    }

    /** Helper to connect directly by MAC address (for auto-reconnect) */
    fun connect(address: String): Boolean {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return false
        return connect(device)
    }

    /** Sends raw bytes to the connected printer.  */
    fun printBytes(data: ByteArray): Boolean {
        return try {
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Closes the active socket. */
    fun disconnect() {
        try {
            outputStream?.close()
            activeSocket?.close()
        } catch (_: Exception) {}
        outputStream = null
        activeSocket = null
    }

    /** Whether a socket connection is alive. */
    fun isConnected(): Boolean = activeSocket?.isConnected == true

    // —— Device name helper ——————————————————————————————————————————————————————————

    @Suppress("MissingPermission")
    fun deviceName(device: BluetoothDevice): String =
        try { device.name ?: "Unknown Device" } catch (_: Exception) { "Unknown Device" }
}
