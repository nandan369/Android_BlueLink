package com.example.bluelink.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.databinding.ItemDeviceCharacteristicBinding
import com.example.bluelink.databinding.ItemDeviceServiceBinding

class DeviceServicesAdapter(
    private val itemList: List<Any>,
    private val characteristicListener: CharacteristicActionListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SERVICE = 1
    private val VIEW_TYPE_CHARACTERISTIC = 2

    // ViewHolder for services
    class ServiceViewHolder(val binding: ItemDeviceServiceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(service: BluetoothGattService) {
            val uuid = service.uuid.toString().substring(4, 8)
            binding.itemDeviceServiceName.text = uuidToName(uuid)
            binding.itemDeviceServiceUuid.text = uuid
            binding.itemDeviceServiceType.text = service.type.toString()
        }
    }

    // ViewHolder for characteristics
    class CharacteristicViewHolder(val binding: ItemDeviceCharacteristicBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            characteristic: BluetoothGattCharacteristic,
            listener: CharacteristicActionListener?
        ) {
            val uuid = characteristic.uuid.toString().substring(4, 8)
            binding.tvCharacteristicName.text = uuidToName(uuid)
            binding.tvCharacteristicUuidNumber.text = uuid
            binding.tvCharacteristicProperties.text = characteristic.properties.toString()

            // Set visibility of buttons based on characteristic properties
            val properties = characteristic.properties
            if (properties and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                binding.characteristicReadButton.isVisible = true
                binding.characteristicReadButton.setOnClickListener {
                    // Handle read button click
                    // Show a dialog with the read value
                    // Save the read value to SharedPreferences
                    listener?.onReadClicked(characteristic)
                }
            }

            if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                binding.characteristicWriteButton.isVisible = true
                binding.characteristicWriteButton.setOnClickListener {
                    // Handle write button click
                    // Show a dialog with the write value
                    val inputEditText = EditText(it.context)
                    val alert = AlertDialog.Builder(it.context)
                        .setTitle("Write Value")
                        .setMessage("Enter the value to write: ")
                        .setView(inputEditText)
                        .setPositiveButton("Write") { _, _ ->
                            // Handle OK button click
                            val value = inputEditText.text.toString()
                            if (value.isNotEmpty()) {
                                listener?.onWriteClicked(characteristic,value.toByteArray())
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .create()
                    alert.show()
                }
            }

            if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                binding.characteristicNotifyButton.isVisible = true
                binding.characteristicNotifyButton.setOnClickListener {
                    // Handle notify button click
                    // Show a dialog with the notify value
                    val options = arrayOf("Enable", "Disable")
                    var selectedOption = 0
                    val alert = AlertDialog.Builder(it.context)
                        .setTitle("Notify")
                        .setSingleChoiceItems(options, selectedOption) { _, which -> selectedOption = which }
                        .setPositiveButton("Apply") { _, _ ->
                            // Handle OK button click
                            val enable = selectedOption == 0
                            listener?.onNotifyClicked(characteristic, enable)
                        }
                        .setNegativeButton("Cancel", null)
                        .create()
                    alert.show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // return the appropriate ViewHolder based on the view type
        return when (viewType) {
            VIEW_TYPE_SERVICE -> {
                val binding = ItemDeviceServiceBinding.inflate(inflater, parent, false)
                ServiceViewHolder(binding)
            }

            VIEW_TYPE_CHARACTERISTIC -> {
                val binding = ItemDeviceCharacteristicBinding.inflate(inflater, parent, false)
                CharacteristicViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = itemList[position]
        when (holder) {
            is ServiceViewHolder -> holder.bind(item as BluetoothGattService)
            is CharacteristicViewHolder -> holder.bind(
                item as BluetoothGattCharacteristic,
                characteristicListener
            )

            else -> throw IllegalArgumentException("Invalid view holder type")
        }
    }

    override fun getItemCount(): Int = itemList.size

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is BluetoothGattService -> VIEW_TYPE_SERVICE
            is BluetoothGattCharacteristic -> VIEW_TYPE_CHARACTERISTIC
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    companion object {
        fun uuidToName(uuid: String): String {
            return when (uuid) {
                "1800" -> "Generic Access"
                "1801" -> "Generic Attribute"
                "180a" -> "Device Information"
                "180f" -> "Battery Service"
                "180d" -> "Heart Rate"
                "1810" -> "Blood Pressure"
                "1811" -> "Alert Notification"
                "1812" -> "Running Speed and Cadence"
                "1813" -> "Pulse Oximeter"
                "1814" -> "Pulse Rate"
                else -> "Unknown Service"
            }
        }
    }

    interface CharacteristicActionListener {
        fun onReadClicked(characteristic: BluetoothGattCharacteristic)
        fun onWriteClicked(characteristic: BluetoothGattCharacteristic, value: ByteArray)
        fun onNotifyClicked(characteristic: BluetoothGattCharacteristic, enable: Boolean)
    }
}