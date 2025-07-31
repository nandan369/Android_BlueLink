package com.example.bluelink.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.R
import com.example.bluelink.databinding.ActivityDeviceDetailBinding
import com.example.bluelink.databinding.ItemDeviceServiceBinding

class DeviceServicesAdapter(private val serviceList: List<String>) : RecyclerView.Adapter<DeviceServicesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDeviceServiceBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        with(holder.binding) {
            itemDeviceServiceName.text = serviceList[position]
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
}