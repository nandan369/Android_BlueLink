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

    /* --------------------------------------------------------
 * Function: hasBluetoothPermissions
 * Usage   : hasBluetoothPermissions(context)
 * --------------------------------------------------------
 * Checks if the app has the necessary Bluetooth permissions granted.
 *
 * Behavior:
 *   - For Android S (API 31) and above, checks for BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions.
 *   - For lower Android versions, checks for ACCESS_FINE_LOCATION permission (required for BLE scanning).
 * Parameters:
 *   - context : Android Context used for permission checking.
 * --------------------------------------------------------*/
    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /* --------------------------------------------------------
 * Function: scanForDevice
 * Usage   : scanForDevice(context, testCaseName, timeoutMillis, targetDeviceName, onResult)
 * --------------------------------------------------------
 * Starts a Bluetooth LE scan to find a device by name or any device if no target specified.
 * Performs permission and Bluetooth availability checks before scanning.
 * Parameters:
 *   - context          : Android Context for UI and logging.
 *   - testCaseName     : Name of the test case or operation for log identification.
 *   - timeoutMillis    : Maximum scan duration in milliseconds (default 10,000 ms).
 *   - targetDeviceName : Optional target device name to look for (case-insensitive).
 *   - onResult         : Callback with Boolean indicating success and optional ScanResult if found.
 * Behavior:
 *   - Shows Toast and logs status messages for start, device found, scan failure, timeout, and errors.
 *   - Stops scanning immediately upon finding the target device.
 *   - Handles SecurityException gracefully and stops scan on failure.
 *   - Stops scan after timeout if device not found.
 * --------------------------------------------------------*/
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
                            val successMsg = " Target device found: $name"
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

        val scanMsg = " Starting scan for ${targetDeviceName ?: "any device"}..."
        Toast.makeText(context, scanMsg, Toast.LENGTH_SHORT).show()
        Log.i(testCaseName, scanMsg)
        LogUtils.logToFile(context, testCaseName, scanMsg)
        scanner.startScan(callback)

        handler.postDelayed({
            if (!resultFound) {
                scanner.stopScan(callback)
                val timeoutMsg = " Scan timeout. Device not found."
                Toast.makeText(context, timeoutMsg, Toast.LENGTH_SHORT).show()
                Log.w(testCaseName, timeoutMsg)
                LogUtils.logToFile(context, testCaseName, timeoutMsg)
                onResult(false, null)
            }
        }, timeoutMillis)
    }

    /* --------------------------------------------------------
 * Function: connectToDevice
 * Usage   : connectToDevice(context, testCaseName, device, onConnected, onDisconnected, onFailed)
 * --------------------------------------------------------
 * Initiates a GATT connection to the specified Bluetooth device.
 * Handles connection state changes via a BluetoothGattCallback.
 * Parameters:
 *   - context        : Android Context for logging.
 *   - testCaseName   : Name of the test case for log identification.
 *   - device         : BluetoothDevice instance to connect to.
 *   - onConnected    : Callback invoked with BluetoothGatt upon successful connection.
 *   - onDisconnected : Callback invoked when the device disconnects.
 *   - onFailed       : Callback invoked if the connection fails or enters an unknown state.
 * Behavior:
 *   - Logs connection, disconnection, and failure events.
 *   - Executes onConnected callback after a 2-second delay post successful connection.
 * --------------------------------------------------------*/
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
                        val msg = " Connected to ${device.name}"
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
                        val msg = " Connection failed or unknown state for ${device.name}"
                        Log.e(testCaseName, msg)
                        LogUtils.logToFile(context, testCaseName, msg)
                        onFailed()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                val message = when (status) {
                    BluetoothGatt.GATT_SUCCESS -> " Services discovered on ${gatt?.device?.address}"
                    else -> " Service discovery failed with status $status"
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
                    LogUtils.logToFile(context, testCaseName, " Read success: Value = $value")
                } else {
                    LogUtils.logToFile(context, testCaseName, " Read failed with status $status")
                }
            }
        }

        try {
            val msg = "🔗 Initiating connection to ${device.name}..."
            Log.i(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
            device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            val msg = " Connection failed due to SecurityException: ${e.message}"
            Log.e(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
            onFailed()
        }
    }

    /* --------------------------------------------------------
 * Function: discoverServices
 * Usage   : discoverServices(gatt, context, testCaseName, onComplete)
 * --------------------------------------------------------
 * Initiates service discovery on the provided BluetoothGatt instance.
 * Logs the start or failure of service discovery.
 * Parameters:
 *   - gatt         : BluetoothGatt instance representing the connection.
 *   - context      : Android Context for logging.
 *   - testCaseName : Optional name of the test case or operation for log identification.
 *   - onComplete   : Optional callback invoked with a Boolean indicating
 *                    whether service discovery was initiated successfully.
 * --------------------------------------------------------*/
    fun discoverServices(
        gatt: BluetoothGatt?,
        context: Context,
        testCaseName: String = "ServiceDiscovery",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (gatt == null) {
            LogUtils.logToFile(context, testCaseName, " Cannot discover services: GATT is null")
            onComplete?.invoke(false)
            return
        }

        val result = gatt.discoverServices()
        LogUtils.logToFile(context, testCaseName, if (result) " Starting service discovery..." else "❌ Failed to start service discovery")
    }

    /* --------------------------------------------------------
 * Function: disconnectGatt
 * Usage   : disconnectGatt(context, testCaseName, gatt)
 * --------------------------------------------------------
 * Safely disconnects and closes an active GATT connection.
 * Parameters:
 *   - context      : Android Context for logging.
 *   - testCaseName : Name of the current test case for log tracking.
 *   - gatt         : BluetoothGatt instance representing the connection.
 * ---------------------------------------------------------*/
    fun disconnectGatt(
        context: Context,
        testCaseName: String,
        gatt: BluetoothGatt
    ) {
        try {
            gatt.disconnect()
            gatt.close()
            val msg = " GATT connection closed for ${gatt.device?.name}"
            Log.i(testCaseName, msg)
            LogUtils.logToFile(context, testCaseName, msg)
        } catch (e: Exception) {
            val err = "Error during disconnect: ${e.message}"
            Log.e(testCaseName, err)
            LogUtils.logToFile(context, testCaseName, err)
        }
    }

    /* --------------------------------------------------------
 * Function: performGattRead
 * Usage   : performGattRead(context, bluetoothGatt, serviceUUID, characteristicUUID, testCaseName)
 * --------------------------------------------------------
 *
 * Attempts to read a specified GATT characteristic from a connected
 * Bluetooth device.
 * Parameters:
 *   - context          : Android Context for logging.
 *   - bluetoothGatt    : Connected BluetoothGatt instance.
 *   - serviceUUID      : UUID of the target GATT service.
 *   - characteristicUUID: UUID of the characteristic to read.
 *   - testCaseName     : Name of the current test case for log tracking.
 * ---------------------------------------------------------*/
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
                    LogUtils.logToFile(context, testCaseName, " Failed to initiate read for $characteristicUUID")
                } else {
                    LogUtils.logToFile(context, testCaseName, " Reading characteristic $characteristicUUID")
                }
            } else {
                LogUtils.logToFile(context, testCaseName, "Characteristic ${characteristicUUID} not found in service 0x180D")
            }
        } else {
            LogUtils.logToFile(context, testCaseName, "Service ${serviceUUID} not found on device")
        }
    }

    /* --------------------------------------------------------
 * Function: initiateBonding
 * Usage   : initiateBonding(device)
 * --------------------------------------------------------
 * Initiates Bluetooth bonding (pairing) with the specified
 * BluetoothDevice instance.
 * ---------------------------------------------------------*/
    fun initiateBonding(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device) as Boolean
        } catch (e: Exception) {
            Log.e("BLE", "Error creating bond", e)
            false
        }
    }
}
