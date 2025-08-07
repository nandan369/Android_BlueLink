package com.example.bluelink.adapters

import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.R
import com.example.bluelink.databinding.ActivityDeviceDetailBinding
import com.example.bluelink.databinding.ItemDeviceServiceBinding

class DeviceServicesAdapter(private val serviceList: List<BluetoothGattService>) : RecyclerView.Adapter<DeviceServicesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDeviceServiceBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val service = serviceList[position]
        with(holder.binding) {
            itemDeviceServiceName.text = uuidToName(service.uuid.toString())
            btnRead.setOnClickListener {
                // Handle read button click
            }
            btnWrite.setOnClickListener {
                // Handle write button click
            }
            btnNotify.setOnClickListener {
                // Handle notify button click
            }
        }
    }

    override fun getItemCount(): Int = serviceList.size

    fun uuidToName(uuid: String): String {
        return when (uuid) {
            "00001800-0000-1000-8000-00805f9b34fb" -> "Generic Access"
            "00001801-0000-1000-8000-00805f9b34fb" -> "Generic Attribute"
            "0000180a-0000-1000-8000-00805f9b34fb" -> "Device Information"
            "0000180f-0000-1000-8000-00805f9b34fb" -> "Battery Service"
            "00001810-0000-1000-8000-00805f9b34fb" -> "Heart Rate"
            "00001811-0000-1000-8000-00805f9b34fb" -> "Device Information"
            "00001812-0000-1000-8000-00805f9b34fb" -> "Running Speed and Cadence"
            "00001813-0000-1000-8000-00805f9b34fb" -> "Pulse Oximeter"
            "00001814-0000-1000-8000-00805f9b34fb" -> "Pulse Rate"
            else -> "Unknown Service"
        }
    }
}