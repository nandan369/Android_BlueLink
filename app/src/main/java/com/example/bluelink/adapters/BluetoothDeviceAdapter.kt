package com.example.bluelink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.databinding.ItemDeviceBinding
import com.example.bluelink.models.BluetoothDeviceModel

class BluetoothDeviceAdapter(
    private val deviceList: List<BluetoothDeviceModel>,
    private val onConnectClick: (BluetoothDeviceModel) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        with(holder.binding) {
            tvDeviceName.text = device.name ?: "Unnamed Device"
            tvDeviceAddress.text = device.address
            btnConnect.isEnabled = device.name != null
            tvRssi.text = "RSSI: ${device.rssi} dBm"
            btnConnect.setOnClickListener {
                onConnectClick(device)
            }
        }
    }

    override fun getItemCount(): Int = deviceList.size
}
