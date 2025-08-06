package com.example.bluelink.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.R
import com.example.bluelink.adapters.BleTestCaseAdapter
import com.example.bluelink.models.BleTestCasesModel
import com.example.bluelink.utils.BleTestUtils
import com.example.bluelink.utils.LogUtils
import android.util.Log

class TestCaseSelectionActivity : AppCompatActivity() {

    private lateinit var testCaseAdapter: BleTestCaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_case_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val btnRunTests = findViewById<Button>(R.id.btnRunTests)
        val editDutName = findViewById<EditText>(R.id.editDutName) // Make sure this exists in your XML

        val testCases = getBleTestCases()
        testCaseAdapter = BleTestCaseAdapter(testCases)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = testCaseAdapter

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

            // Loop through selected test cases and run each
            for (test in selectedTests) {
                when (test.id) {
                    "TC01" -> runTc01ScanTest(dutName)
                    // Add more test cases here like:
                    // "TC04" -> runTc04ConnectTest(dutName)
                    else -> Toast.makeText(this, "Test ${test.title} not implemented.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun runTc01ScanTest(dutName: String) {

        BleTestUtils.scanForDevice(
            context = this,
            testCaseName = "TC01",
            targetDeviceName = dutName,
            timeoutMillis = 10000
        ) { success, result ->
            val logMessage = if (success && result != null) {
                "TC01 PASS: Found ${result.device.name}"
            } else {
                "TC01 FAIL: Device not found or scan failed"
            }
            LogUtils.logToFile(this, "TC01", logMessage)
            Log.d("TestCase_TC01", logMessage)
        }
    }

    private fun getBleTestCases(): List<BleTestCasesModel> {
        return listOf(
            BleTestCasesModel("TC01", "Advertise & Discover", "Verify DUT is discoverable via scan."),
            BleTestCasesModel("TC04", "Connect to DUT", "Test GATT connection initiation."),
            BleTestCasesModel("TC06", "Bonding", "Test pairing using Just Works or Passkey."),
            BleTestCasesModel("TC10", "Read Characteristics", "Read characteristic from DUT."),
            BleTestCasesModel("TC13", "Notification Test", "Send notification and validate reception."),
            BleTestCasesModel("TC18", "Range Test", "Test BLE connection at various distances.")
        )
    }
}
