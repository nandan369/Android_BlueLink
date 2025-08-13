package com.example.bluelink.activities

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.example.bluelink.adapters.DeviceServicesAdapter
import com.example.bluelink.databinding.ActivityDeviceDetailBinding
import java.util.UUID

class DeviceDetailActivity : AppCompatActivity(),
    DeviceServicesAdapter.CharacteristicActionListener {
    private lateinit var binding: ActivityDeviceDetailBinding
    private lateinit var deviceServicesAdapter: DeviceServicesAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private var deviceName: String? = null
    private var deviceAddress: String? = null
    private var rssi: Int? = 0
    private lateinit var connectedDevice: BluetoothDevice

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothGatt = GattHolder.gatt!!
        connectedDevice = bluetoothGatt.device

        loadDeviceDetails(bluetoothGatt)

        binding.ddLogButton.setOnClickListener {
            // Set up RecyclerView
            val serviceAndCharacteristicList = extractCharacteristics(bluetoothGatt?.services)
            deviceServicesAdapter = DeviceServicesAdapter(serviceAndCharacteristicList, this)
            binding.ddRvServices.adapter = deviceServicesAdapter
        }


    }

    fun extractCharacteristics(serviceList: List<BluetoothGattService>?): List<Any> {
        val itemList = mutableListOf<Any>()
        if (serviceList != null) {
            for (service in serviceList) {
                itemList.add(service)
                for (characteristic in service.characteristics) {
                    itemList.add(characteristic)
                }
            }
        }
        return itemList
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun loadDeviceDetails(bluetoothGatt: BluetoothGatt) {
        binding.ddDeviceName.text = bluetoothGatt.device.name ?: "Unknown Device"
        binding.ddDeviceAddress.text = bluetoothGatt.device.address ?: "Unknown Address"

        val rssi = intent.getIntExtra("deviceRssi", 0)
        if (rssi != 0) {
            val rssiText = "RSSI: $rssi dBm"
            binding.ddRssi.text = rssiText
        } else {
            binding.ddRssi.text = "RSSI: Unknown"
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReadClicked(characteristic: BluetoothGattCharacteristic) {
        if (::bluetoothGatt.isInitialized) {
            val success = bluetoothGatt.readCharacteristic(characteristic)
            if (success) {
                Toast.makeText(this, "Reading Characteristic...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Failed to read characteristic for ${characteristic.uuid}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onWriteClicked(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        // TODO: Implement write logic
        if (::bluetoothGatt.isInitialized) {
            characteristic.value = value // Set the value to write
            characteristic.writeType =
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // Or WRITE_TYPE_NO_RESPONSE
            val success = bluetoothGatt.writeCharacteristic(characteristic)
            if (success) {
                // Result in BluetoothGattCallback.onCharacteristicWrite
                Toast.makeText(this, "Writing characteristic...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Failed to initiate write for ${characteristic.uuid}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onNotifyClicked(
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean
    ) {
        // TODO("Not yet implemented")
        if (::bluetoothGatt.isInitialized) {
            val success = bluetoothGatt.setCharacteristicNotification(characteristic, enable)
            if (success) {
                // You also need to write to the Client Characteristic Configuration Descriptor (CCCD)
                val cccdUuid =
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Standard CCCD UUID
                val descriptor = characteristic.getDescriptor(cccdUuid)
                if (descriptor != null) {
                    val value = if (enable) {
                        if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        } else {
                            null
                        }
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }

                    if (value != null) {
                        descriptor.value = value
                        val descWriteSuccess = bluetoothGatt.writeDescriptor(descriptor)
                        if (!descWriteSuccess) {
                            Toast.makeText(
                                this,
                                "Failed to write CCCD for ${characteristic.uuid}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val action = if (enable) "Enabling" else "Disabling"
                            Toast.makeText(
                                this,
                                "$action notifications for ${characteristic.uuid}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "CCCD not found for ${characteristic.uuid}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Failed to set notification for ${characteristic.uuid}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // BluetoothGattCallback methods
    private val gattCallback = object : BluetoothGattCallback() {

        // Response from Characteristic Read
        // Read function for API level 33 and below
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val value = characteristic.value ?: byteArrayOf()
            onCharacteristicReadBody(gatt, characteristic, value, status)
        }

        // Read function for API level 34 and above
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            onCharacteristicReadBody(gatt, characteristic, value, status)
        }

        // Helper function for onCharacteristicReads
        fun onCharacteristicReadBody(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // TODO: cast value to the correct type from byte
                    val pretty = decodeRead(characteristic, value)
                    // TODO: Save the read value to a SharedPreferences or Room database
                    Toast.makeText(
                        this@DeviceDetailActivity,
                        "Characteristic value: $value",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@DeviceDetailActivity,
                        "Failed to read characteristic!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        private fun decodeRead(
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
//            val short = shortUuid(characteristic.uuid)
        }

        // Response from Characteristic Write
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(
                        this@DeviceDetailActivity,
                        "Characteristic value written successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // TODO: Save the written value to a SharedPreferences or Room database
                } else {
                    Toast.makeText(
                        this@DeviceDetailActivity,
                        "Failed to write characteristic!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Response if notification is enabled/disabled
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(
                        this@DeviceDetailActivity,
                        "Descriptor written successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@DeviceDetailActivity,
                        "Failed to write descriptor!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Response when the notification is enabled
        // Change function for API level 33 and below
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: byteArrayOf()
            onCharacteristicChangedBody(gatt, characteristic, value)
        }

        // Change function for API level 34 and above
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            onCharacteristicChangedBody(gatt, characteristic, value)
        }

        private fun onCharacteristicChangedBody(
            gatt: BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            runOnUiThread {
                val changedValue = value
                Toast.makeText(
                    this@DeviceDetailActivity,
                    "Characteristic value changed: $changedValue",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: Save the changed value to a SharedPreferences or Room database
            }
        }
    }

}