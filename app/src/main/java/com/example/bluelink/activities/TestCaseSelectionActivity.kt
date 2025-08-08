package com.example.bluelink.activities

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
                    else -> Toast.makeText(this, "Test ${test.title} not implemented.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                            // Save gatt if you want to disconnect later
                            connectedGatt = gatt

                            BleTestUtils.discoverServices(gatt, context = this, testCaseName = "Ble_connect")
                        },
                        onDisconnected = {
                            LogUtils.logToFile(this, "Ble_connect", "🔌 Disconnected")
                        },
                        onFailed = {
                            LogUtils.logToFile(this, "Ble_connect", "❌ Connection failed")
                        }
                    )
                }, 1000L) // 1-second delay

            } else {
                LogUtils.logToFile(this, "Ble_connect", "❌ Scan failed or device not found")
            }
        }
    }


    private fun getBleTestCases(): List<BleTestCasesModel> {
        return listOf(
            BleTestCasesModel("TC01", "Advertise & Discover", "Verify DUT is discoverable via scan."),
            BleTestCasesModel("TC02", "Connect to DUT", "Test GATT connection initiation."),
            BleTestCasesModel("TC06", "Bonding", "Test pairing using Just Works or Passkey."),
            BleTestCasesModel("TC10", "Read Characteristics", "Read characteristic from DUT."),
            BleTestCasesModel("TC13", "Notification Test", "Send notification and validate reception."),
            BleTestCasesModel("TC18", "Range Test", "Test BLE connection at various distances.")
        )
    }
}