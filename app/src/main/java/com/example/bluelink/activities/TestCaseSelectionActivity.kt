package com.example.bluelink.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.R
import com.example.bluelink.adapters.BleTestCaseAdapter
import com.example.bluelink.models.BleTestCasesModel

class TestCaseSelectionActivity : AppCompatActivity() {

    private lateinit var testCaseAdapter: BleTestCaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_case_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val btnRunTests = findViewById<Button>(R.id.btnRunTests)

        val testCases = getBleTestCases()
        testCaseAdapter = BleTestCaseAdapter(testCases)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = testCaseAdapter

        fun getSelectedTestCases(): List<BleTestCasesModel> {
            return testCases.filter { it.isSelected }
        }

        btnRunTests.setOnClickListener {
            val selectedTests = testCaseAdapter.getSelectedTestCases()
            if (selectedTests.isEmpty()) {
                Toast.makeText(this, "Please select at least one test.", Toast.LENGTH_SHORT).show()
            } else {
                val selectedTests: List<BleTestCasesModel> = testCaseAdapter.getSelectedTestCases()
                val testNames = selectedTests.joinToString(separator = ", ") { it.title }
                Toast.makeText(this, "Running: $testNames", Toast.LENGTH_LONG).show()
            }
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
