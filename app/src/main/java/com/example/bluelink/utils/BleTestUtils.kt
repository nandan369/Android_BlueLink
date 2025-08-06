package com.example.bluelink.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
}
