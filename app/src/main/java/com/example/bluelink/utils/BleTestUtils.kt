package com.example.bluelink.utils

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.UUID

object BleTestUtils {

    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun scanForDevice(
        context: Context,
        testCaseName: String,
        timeoutMillis: Long = 10000,
        targetDeviceName: String? = null,
        onResult: (Boolean, ScanResult?) -> Unit
    ) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val msg = "Bluetooth not available or disabled."
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            Log.w(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
            onResult(false, null)
            return
        }

        if (!hasBluetoothPermissions(context)) {
            val msg = "Missing Bluetooth permissions."
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            Log.w(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
            onResult(false, null)
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        val handler = Handler(Looper.getMainLooper())
        var resultFound = false

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (!resultFound) {
                    try {
                        val name = result?.device?.name
                        val addr = result?.device?.address
                        val foundMsg = "Found device: $name ($addr)"
                        Log.d(testCaseName, foundMsg)
                        LogUtils.logToFile(context, testCaseName, foundMsg)

                        if (targetDeviceName == null || name.equals(targetDeviceName, ignoreCase = true)) {
                            resultFound = true
                            scanner.stopScan(this)
                            val successMsg = "✅ Target device found: $name"
                            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                            Log.d(testCaseName, successMsg)
                            LogUtils.logToFile(context, testCaseName, successMsg)
                            onResult(true, result)
                        }
                    } catch (e: SecurityException) {
                        scanner.stopScan(this)
                        val err = "SecurityException while accessing device info"
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        Log.e(testCaseName, err)
                        LogUtils.logToFile(context, testCaseName, err)
                        onResult(false, null)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                if (!resultFound) {
                    val msg = "Scan failed with error code: $errorCode"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.e(testCaseName, msg)
                    LogUtils.logToFile(context, testCaseName, msg)
                    onResult(false, null)
                }
            }
        }

        val scanMsg = "🔍 Starting scan for ${targetDeviceName ?: "any device"}..."
        Toast.makeText(context, scanMsg, Toast.LENGTH_SHORT).show()
        Log.i(testCaseName, scanMsg)
        LogUtils.logToFile(context, testCaseName, scanMsg)
        scanner.startScan(callback)

        handler.postDelayed({
            if (!resultFound) {
                scanner.stopScan(callback)
                val timeoutMsg = "⌛ Scan timeout. Device not found."
                Toast.makeText(context, timeoutMsg, Toast.LENGTH_SHORT).show()
                Log.w(testCaseName, timeoutMsg)
                LogUtils.logToFile(context, testCaseName, timeoutMsg)
                onResult(false, null)
            }
        }, timeoutMillis)
    }

    fun connectToDevice(
        context: Context,
        testCaseName: String,
        device: BluetoothDevice,
        onConnected: (BluetoothGatt) -> Unit,
        onDisconnected: () -> Unit,
        onFailed: () -> Unit
    ) {
        val gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(gatt, status, newState)

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        val msg = "✅ Connected to ${device.name}"
                        Log.i(testCaseName, msg)
                        LogUtils.logToFile(context, testCaseName, msg)

                        // Delay before executing test logic
                        Handler(Looper.getMainLooper()).postDelayed({
                            onConnected(gatt)
                        }, 2000) // 2 seconds delay
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        val msg = "🔌 Disconnected from ${device.name}"
                        Log.w(testCaseName, msg)
                        LogUtils.logToFile(context, testCaseName, msg)
                        onDisconnected()
                    }

                    else -> {
                        val msg = "⚠️ Connection failed or unknown state for ${device.name}"
                        Log.e(testCaseName, msg)
                        LogUtils.logToFile(context, testCaseName, msg)
                        onFailed()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                val message = when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "✅ Services discovered on ${gatt?.device?.address}"
                    else -> "❌ Service discovery failed with status $status"
                }
                LogUtils.logToFile(context, testCaseName, message)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = characteristic?.value?.joinToString(" ") { it.toUByte().toString() }
                    LogUtils.logToFile(context, testCaseName, "✅ Read success: Value = $value")
                } else {
                    LogUtils.logToFile(context, testCaseName, "❌ Read failed with status $status")
                }
            }
        }

        try {
            val msg = "🔗 Initiating connection to ${device.name}..."
            Log.i(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
            device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            val msg = "❌ Connection failed due to SecurityException: ${e.message}"
            Log.e(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
            onFailed()
        }
    }

    fun discoverServices(
        gatt: BluetoothGatt?,
        context: Context,
        testCaseName: String = "ServiceDiscovery",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (gatt == null) {
            LogUtils.logToFile(context, testCaseName, "❌ Cannot discover services: GATT is null")
            onComplete?.invoke(false)
            return
        }

        val result = gatt.discoverServices()
        LogUtils.logToFile(context, testCaseName, if (result) "🔄 Starting service discovery..." else "❌ Failed to start service discovery")
    }

    fun disconnectGatt(
        context: Context,
        testCaseName: String,
        gatt: BluetoothGatt
    ) {
        try {
            gatt.disconnect()
            gatt.close()
            val msg = "🔌 GATT connection closed for ${gatt.device?.name}"
            Log.i(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
        } catch (e: Exception) {
            val err = "❌ Error during disconnect: ${e.message}"
            Log.e(testCaseName, err)
            LogUtils.logToFile(context, testCaseName, err)
        }
    }

    fun performGattRead(
        context: Context,
        bluetoothGatt: BluetoothGatt,
        serviceUUID: UUID,
        characteristicUUID: UUID,
        testCaseName: String
    ) {
        val service = bluetoothGatt.getService(serviceUUID)

        if (service != null) {
            val characteristic = service?.getCharacteristic(characteristicUUID)
            if (characteristic != null) {
                val success = bluetoothGatt.readCharacteristic(characteristic)
                if (!success) {
                    LogUtils.logToFile(context, testCaseName, "❌ Failed to initiate read for $characteristicUUID")
                } else {
                    LogUtils.logToFile(context, testCaseName, "🔄 Reading characteristic $characteristicUUID")
                }
            } else {
                LogUtils.logToFile(context, testCaseName, "Characteristic ${characteristicUUID} not found in service 0x180D")
            }
        } else {
            LogUtils.logToFile(context, testCaseName, "Service ${serviceUUID} not found on device")
        }
    }

}
