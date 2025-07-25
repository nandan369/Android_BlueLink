package com.example.bluelink.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bluelink.databinding.ActivityDeviceDetailBinding

class DeviceDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceDetailBinding
    private lateinit var deviceName: String
    private lateinit var deviceAddress: String
    private var rssi: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceName = intent.getStringExtra("deviceName") ?: "Unknown Device"
        deviceAddress = intent.getStringExtra("deviceAddress") ?: "Unknown Address"
        rssi = intent.getIntExtra("rssi", 0)
        val rssiText = "RSSI: $rssi dBm"

        binding.ddDeviceName.text = deviceName
        binding.ddDeviceAddress.text = deviceAddress
        binding.ddRssi.text = rssiText

        binding.button.setOnClickListener {
            // Go to Log History Activity
        }

    }
}