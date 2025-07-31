package com.example.bluelink.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bluelink.adapters.DeviceServicesAdapter
import com.example.bluelink.databinding.ActivityDeviceDetailBinding

class DeviceDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceDetailBinding
    private var deviceName: String? = null
    private var deviceAddress: String? = null
    private var rssi: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceName = intent.getStringExtra("deviceName") ?: "Unknown Device"
        deviceAddress = intent.getStringExtra("deviceAddress") ?: "Unknown Address"
        rssi = intent.getIntExtra("rssi", 0)

        loadDeviceDetails(deviceName, deviceAddress, rssi)

        binding.ddLogButton.setOnClickListener {
            // Go to Log History Activity
        }

        // Set up RecyclerView
        val serviceList = listOf("Service 1", "Service 2", "Service 3")
        binding.ddRvServices.adapter = DeviceServicesAdapter(serviceList)

    }

    fun loadDeviceDetails(deviceName: String?, deviceAddress: String?, rssi: Int?) {
        binding.ddDeviceName.text = deviceName?: "Unknown Device"
        binding.ddDeviceAddress.text = deviceAddress?: "Unknown Address"
        if (rssi != null) {
            val rssiText = "RSSI: $rssi dBm"
            binding.ddRssi.text = rssiText
        } else {
            binding.ddRssi.text = "RSSI: Unknown"
        }
    }

}