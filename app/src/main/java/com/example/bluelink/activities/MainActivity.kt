package com.example.bluelink.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
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

    private var bluetoothGatt: BluetoothGatt? = null

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
                    rssi = rssi,
                    device = device
                )
                scanResults.add(model)
                adapter.notifyItemInserted(scanResults.size - 1)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to ${gatt.device.address}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                gatt.discoverServices()  // Move to Stage 2
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Services discovered: ${services.size}", Toast.LENGTH_SHORT).show()
                }
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
        adapter = BluetoothDeviceAdapter(scanResults) { deviceModel ->
            bluetoothLeScanner?.stopScan(scanCallback) // Stop scan first

            bluetoothGatt = deviceModel.device.connectGatt(
                this,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

            GattHolder.gatt = bluetoothGatt

            Toast.makeText(this, "Connecting to ${deviceModel.name}", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, DeviceDetailActivity::class.java)
            startActivity(intent)
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

object GattHolder {
    var gatt: BluetoothGatt? = null
}