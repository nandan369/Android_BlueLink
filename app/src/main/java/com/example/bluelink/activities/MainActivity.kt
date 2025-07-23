package com.example.bluelink.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluelink.adapters.BluetoothDeviceAdapter
import com.example.bluelink.databinding.ActivityMainBinding
import com.example.bluelink.models.BluetoothDeviceModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BluetoothDeviceAdapter
    private val scanResults = mutableListOf<BluetoothDeviceModel>()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            // Avoid duplicates
            val isKnown = scanResults.any { it.address == device.address }
            if (!isKnown) {
                val model = BluetoothDeviceModel(
                    name = device.name,
                    address = device.address,
                    rssi = rssi
                )
                scanResults.add(model)
                adapter.notifyItemInserted(scanResults.size - 1)
            }
        }
    }

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startBleScan()
        } else {
            Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        adapter = BluetoothDeviceAdapter(scanResults) { device ->
            Toast.makeText(this, "Connect to ${device.name}", Toast.LENGTH_SHORT).show()
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvDevices.adapter = adapter

        // Set up scan button
        binding.btnScan.setOnClickListener {
            checkAndRequestPermissions()
        }

        // Initialize Bluetooth adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionRequestLauncher.launch(missing.toTypedArray())
        } else if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable Location services to scan Bluetooth devices.", Toast.LENGTH_LONG).show()
        } else {
            startBleScan()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun startBleScan() {
        scanResults.clear()
        adapter.notifyDataSetChanged()

        bluetoothLeScanner?.startScan(scanCallback)
        Toast.makeText(this, "Scanning started...", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            Toast.makeText(this, "Scan stopped", Toast.LENGTH_SHORT).show()
        }, 10_000)
    }
}
