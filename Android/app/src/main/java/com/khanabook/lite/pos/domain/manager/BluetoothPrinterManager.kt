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


class BluetoothPrinterManager(private val context: Context) {

    companion object {
        
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    

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

    

    
    @Suppress("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasRequiredPermissions()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    
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

    
    @Suppress("MissingPermission")
    fun startScan() {
        if (!hasRequiredPermissions() || !isBluetoothEnabled()) return

        
        val paired = getPairedDevices()
        _scannedDevices.value = paired.toMutableList()
        _isScanning.value = true

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        
        
        try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(discoveryReceiver, filter)
        }

        bluetoothAdapter?.cancelDiscovery()
        bluetoothAdapter?.startDiscovery()
    }

    
    @Suppress("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
        try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
    }

    

    
    @Suppress("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        disconnect()
        _isConnecting.value = true
        return try {
            bluetoothAdapter?.cancelDiscovery()
            
            
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            } catch (e: Exception) {
                socket?.close()
                
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            }

            activeSocket = socket
            outputStream = socket?.outputStream
            _isConnected.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            false
        } finally {
            _isConnecting.value = false
        }
    }

    
    fun connect(address: String): Boolean {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return false
        return connect(device)
    }

    
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

    
    fun disconnect() {
        try {
            outputStream?.close()
            activeSocket?.close()
        } catch (_: Exception) {}
        outputStream = null
        activeSocket = null
        _isConnected.value = false
    }

    
    fun isConnected(): Boolean = activeSocket?.isConnected == true

    

    @Suppress("MissingPermission")
    fun deviceName(device: BluetoothDevice): String =
        try { device.name ?: "Unknown Device" } catch (_: Exception) { "Unknown Device" }
}
