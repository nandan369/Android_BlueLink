package com.example.bluelink.activities

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
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

        val bluetoothGatt = GattHolder.gatt
        val connectedDevice = bluetoothGatt?.device

        loadDeviceDetails(connectedDevice)

        binding.ddLogButton.setOnClickListener {
            // Go to Log History Activity
        }

        // Set up RecyclerView
        val serviceList = bluetoothGatt?.services
        binding.ddRvServices.adapter = DeviceServicesAdapter(serviceList as List<BluetoothGattService>)

    }

    fun loadDeviceDetails(connectedDevice: BluetoothDevice?) {
        binding.ddDeviceName.text = connectedDevice.name?: "Unknown Device"
        binding.ddDeviceAddress.text = connectedDevice.address?: "Unknown Address"
        val rssi = connectedDevice.
        if (rssi != null) {
            val rssiText = "RSSI: $rssi dBm"
            binding.ddRssi.text = rssiText
        } else {
            binding.ddRssi.text = "RSSI: Unknown"
        }
    }

}