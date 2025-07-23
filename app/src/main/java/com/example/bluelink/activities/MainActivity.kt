package com.example.bluelink.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluelink.adapters.BluetoothDeviceAdapter
import com.example.bluelink.databinding.ActivityMainBinding
import com.example.bluelink.models.BluetoothDeviceModel
import com.example.bluelink.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BluetoothDeviceAdapter
    private val mockDeviceList = mutableListOf<BluetoothDeviceModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        adapter = BluetoothDeviceAdapter(mockDeviceList) { device ->
            Toast.makeText(this, "Connect to ${device.name}", Toast.LENGTH_SHORT).show()
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvDevices.adapter = adapter

        // Scan button mock behavior
        binding.btnScan.setOnClickListener {
            loadMockDevices()
        }
    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//    }


     //Mock data to test UI
    private fun loadMockDevices() {
        mockDeviceList.clear()
        mockDeviceList.add(BluetoothDeviceModel("BLE Sensor", "00:11:22:33:44:55", -65))
        mockDeviceList.add(BluetoothDeviceModel(null, "11:22:33:44:55:66", -70))
        mockDeviceList.add(BluetoothDeviceModel("Smart Watch", "AA:BB:CC:DD:EE:FF", -80))
        adapter.notifyDataSetChanged()
    }
}


