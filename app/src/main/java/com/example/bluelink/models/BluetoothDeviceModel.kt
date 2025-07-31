package com.example.bluelink.models
import android.bluetooth.BluetoothDevice

data class BluetoothDeviceModel(
    val name: String?,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice
)
