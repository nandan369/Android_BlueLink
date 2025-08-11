package com.example.bluelink.activities

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.R
import com.example.bluelink.adapters.BleTestCaseAdapter
import com.example.bluelink.adapters.LogAdapter
import com.example.bluelink.models.BleTestCasesModel
import com.example.bluelink.utils.BleTestUtils
import com.example.bluelink.utils.LogUtils
import android.bluetooth.BluetoothGatt
import android.content.BroadcastReceiver
import java.util.UUID

class TestCaseSelectionActivity : AppCompatActivity() {

    private lateinit var testCaseAdapter: BleTestCaseAdapter
    private lateinit var logAdapter: LogAdapter
    private val logList = mutableListOf<String>()
    private var connectedGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_case_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val logRecyclerView = findViewById<RecyclerView>(R.id.logRecyclerView) // ADD this to your XML
        val btnRunTests = findViewById<Button>(R.id.btnRunTests)
        val editDutName = findViewById<EditText>(R.id.editDutName)

        // Setup test case list
        testCaseAdapter = BleTestCaseAdapter(getBleTestCases())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = testCaseAdapter

        // Setup log viewer
        logAdapter = LogAdapter(logList)
        logRecyclerView.layoutManager = LinearLayoutManager(this)
        logRecyclerView.adapter = logAdapter

        // Set the listener for logs
        LogUtils.logListener = { log ->
            runOnUiThread {
                logList.add(log)
                logAdapter.notifyItemInserted(logList.size - 1)
                logRecyclerView.scrollToPosition(logList.size - 1)
            }
        }

        btnRunTests.setOnClickListener {
            val selectedTests = testCaseAdapter.getSelectedTestCases()
            val dutName = editDutName.text.toString().trim()

            if (selectedTests.isEmpty()) {
                Toast.makeText(this, "Please select at least one test.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dutName.isEmpty()) {
                Toast.makeText(this, "Please enter DUT name to run tests.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            for (test in selectedTests) {
                when (test.id) {
                    "TC01" -> runTc01ScanTest(dutName)
                    "TC02" -> runTc02ConnectTest(dutName)
                    "TC03" -> runTc03GattReadTest(dutName)
                    "TC04" -> runTc04BondingTest(dutName)
                    else -> Toast.makeText(this, "Test ${test.title} not implemented.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /* --------------------------------------------------------
 * Property: bondReceiver
 * Usage   : registerReceiver(bondReceiver, intentFilter)
 * --------------------------------------------------------
 * BroadcastReceiver instance used to monitor Bluetooth bonding
 * state changes for the "TC04_Bonding" test case.
 *
 * Behavior:
 *   - Listens for the ACTION_BOND_STATE_CHANGED broadcast.
 *   - Retrieves the target Bluetooth device, current bond state,
 *     and previous bond state from the received intent.
 *   - Handles the following bond states:
 *       1. BOND_BONDING → Logs that bonding is in progress.
 *       2. BOND_BONDED  → Logs success and unregisters the receiver.
 *       3. BOND_NONE    → Logs failure and unregisters the receiver.
 *
 * This receiver is registered before initiating bonding and
 * automatically unregistered upon completion or failure.
 * ---------------------------------------------------------*/
    private val bondReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)

                when (bondState) {
                    BluetoothDevice.BOND_BONDING -> {
                        Log.d("Bonding", "Bonding in progress with ${device?.address}")
                        LogUtils.logToFile(context!!, "TC04_Bonding", " Bonding in progress with ${device?.address}")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        Log.d("Bonding", "Bonding successful with ${device?.address}")
                        LogUtils.logToFile(context!!, "TC04_Bonding", " Bonding successful with ${device?.address}")
                        context.unregisterReceiver(this)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Log.d("Bonding", "Bonding failed with ${device?.address}")
                        LogUtils.logToFile(context!!, "TC04_Bonding", " Bonding failed with ${device?.address}")
                        context.unregisterReceiver(this)
                    }
                }
            }
        }
    }

    /* --------------------------------------------------------
     * Function: runTc01ScanTest
     * Usage   : runTc01ScanTest(dutName)
     * --------------------------------------------------------
     *
     * Executes the BLE test case "TC01_Scan" (labeled as "Ble_scan")
     * for the specified Device Under Test (DUT) name.
     * This test verifies that the DUT is discoverable via BLE scanning
     * within the given timeout period.
     * ---------------------------------------------------------*/
    private fun runTc01ScanTest(dutName: String) {
        BleTestUtils.scanForDevice(
            context = this,
            testCaseName = "Ble_scan",
            targetDeviceName = dutName,
            timeoutMillis = 10000
        ) { success, result ->
            val logMessage = if (success && result != null) {
                "Ble_scan PASS: Found ${result.device.name}"
            } else {
                "TC01 FAIL: Device not found or scan failed"
            }
            LogUtils.logToFile(this, "Ble_scan", logMessage)
        }
    }

    /* --------------------------------------------------------
     * Function: runTc02ConnectTest
     * Usage   : runTc02ConnectTest(dutName)
     * --------------------------------------------------------
     * Executes the BLE test case "TC02_Connect" (labeled as "Ble_connect")
     * for the specified Device Under Test (DUT) name.
     * This test verifies that the DUT can be successfully discovered
     * and connected to via BLE GATT.
     * ---------------------------------------------------------*/
    private fun runTc02ConnectTest(dutName: String) {
        BleTestUtils.scanForDevice(
            context = this,
            testCaseName = "Ble_connect",
            targetDeviceName = dutName,
            timeoutMillis = 10000
        ) { success, result ->
            if (success && result != null) {
                LogUtils.logToFile(this, "Ble_connect", "Device found: ${result.device.name}")

                // Optional delay before connect
                Handler(Looper.getMainLooper()).postDelayed({
                    BleTestUtils.connectToDevice(
                        context = this,
                        testCaseName = "Ble_connect",
                        device = result.device,
                        onConnected = { gatt ->
                            LogUtils.logToFile(this, "Ble_connect", "✅ Connected to ${result.device.address}")
                            connectedGatt = gatt
                            BleTestUtils.discoverServices(gatt, context = this, testCaseName = "Ble_connect")
                        },
                        onDisconnected = {
                            LogUtils.logToFile(this, "Ble_connect", " Disconnected")
                        },
                        onFailed = {
                            LogUtils.logToFile(this, "Ble_connect", " Connection failed")
                        }
                    )
                }, 1000L) // 1-second delay

            } else {
                LogUtils.logToFile(this, "Ble_connect", " Scan failed or device not found")
            }
        }
    }

    /* --------------------------------------------------------
 * Function: runTc03GattReadTest
 * Usage   : runTc03GattReadTest(dutName)
 * --------------------------------------------------------
 *
 * Executes the BLE test case "TC03_GATT_Read" for the specified
 * Device Under Test (DUT) name.
 * This test verifies that the DUT supports and responds correctly
 * to GATT read operations on known services and characteristics.
 * ---------------------------------------------------------*/
    private fun runTc03GattReadTest(dutName: String) {
        val testCaseName = "TC03_GATT_Read"

        BleTestUtils.scanForDevice(
            context = this,
            testCaseName = testCaseName,
            targetDeviceName = dutName,
            timeoutMillis = 10000
        ) { success, result ->
            if (success && result != null) {
                BleTestUtils.connectToDevice(
                    context = this,
                    testCaseName = testCaseName,
                    device = result.device,
                    onConnected = { gatt ->
                        connectedGatt = gatt
                        BleTestUtils.discoverServices(gatt, context = this, testCaseName = testCaseName)
                        Handler(Looper.getMainLooper()).postDelayed({
                            BleTestUtils.performGattRead(
                                context = this,
                                bluetoothGatt = gatt,
                                serviceUUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb"),
                                characteristicUUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb"),
                                testCaseName = testCaseName
                            )
                        }, 1000)
                    },
                    onDisconnected = {
                        LogUtils.logToFile(this, testCaseName, "ℹ️ Device disconnected.")
                    },
                    onFailed = {
                        LogUtils.logToFile(this, testCaseName, "❌ Connection failed.")
                    }
                )

            } else {
                LogUtils.logToFile(this, testCaseName, "❌ Device not found, skipping test")
            }
        }
    }

/* --------------------------------------------------------
* Function: runTc04BondingTest
* Usage   : runTc04BondingTest(dutName)
* --------------------------------------------------------
*
* Executes the BLE test case "TC04_Bonding" for the specified
* Device Under Test (DUT) name
* ---------------------------------------------------------*/
private fun runTc04BondingTest(dutName: String) {
    val testCaseName = "TC04_Bonding"

    BleTestUtils.scanForDevice(
        context = this,
        testCaseName = testCaseName,
        targetDeviceName = dutName,
        timeoutMillis = 10000
    ) { success, result ->
        if (success && result != null) {
            LogUtils.logToFile(this, testCaseName, "✅ Device found: ${result.device.address}")

            // Register bond receiver
            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            registerReceiver(bondReceiver, filter)

            // Connect first
            BleTestUtils.connectToDevice(
                context = this,
                testCaseName = testCaseName,
                device = result.device,
                onConnected = { gatt ->
                    connectedGatt = gatt
                    LogUtils.logToFile(this, testCaseName, "🔗 Connected to device. Starting bonding...")
                    // Now initiate bonding
                    val bondStarted = BleTestUtils.initiateBonding(result.device)
                    if (bondStarted) {
                        LogUtils.logToFile(this, testCaseName, " Bonding initiated.")
                    } else {
                        LogUtils.logToFile(this, testCaseName, " Bonding initiation failed.")
                    }
                },
                onDisconnected = {
                    LogUtils.logToFile(this, testCaseName, "Disconnected from device.")
                },
                onFailed = {
                    LogUtils.logToFile(this, testCaseName, "Connection failed, bonding skipped.")
                }
            )
        } else {
            LogUtils.logToFile(this, testCaseName, "Scan failed or device not found.")
        }
    }
}

/* --------------------------------------------------------
* Function: getBleTestCases
* Usage   : getBleTestCases()
* --------------------------------------------------------
*
* The function creates and returns a list of predefined BLE test cases.
* The returned list includes common BLE operations like advertising,
* connecting, bonding, and reading characteristics.
* ---------------------------------------------------------*/
private fun getBleTestCases(): List<BleTestCasesModel> {
    return listOf(
        BleTestCasesModel("TC01", "Advertise & Discover", "Verify DUT is discoverable via scan."),
        BleTestCasesModel("TC02", "Connect to DUT", "Test GATT connection initiation."),
        BleTestCasesModel("TC04", "Bonding", "Test pairing using Just Works or Passkey."),
        BleTestCasesModel("TC03", "Read Characteristics", "Read characteristic from DUT."),
    )
}
}