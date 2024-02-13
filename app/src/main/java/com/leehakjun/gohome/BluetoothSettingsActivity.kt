package com.leehakjun.gohome

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BluetoothSettingsActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 101
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var selectedBluetoothText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_settings)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        deviceListView = findViewById(R.id.deviceListView)
        selectedBluetoothText = findViewById(R.id.selectedBluetoothText)

        requestBluetoothPermission()
    }

    private fun requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_PERMISSION
            )
        } else {
            scanBluetoothDevices()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanBluetoothDevices()
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth permission denied. Cannot scan for devices.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun scanBluetoothDevices() {
        val pairedDevices = bluetoothAdapter.bondedDevices
        val devices = mutableListOf<String>()
        for (device in pairedDevices) {
            devices.add(device.name)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        deviceListView.adapter = adapter

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = devices[position]
            saveSelectedBluetoothDevice(selectedDevice)
            updateSelectedBluetoothText(selectedDevice)
            Toast.makeText(this, "Selected device: $selectedDevice", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSelectedBluetoothDevice(deviceName: String) {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("bluetooth_device", deviceName)
        editor.apply()
    }

    private fun updateSelectedBluetoothText(deviceName: String?) {
        if (deviceName != null) {
            selectedBluetoothText.text = "Selected Bluetooth Device: $deviceName"
            selectedBluetoothText.visibility = TextView.VISIBLE
        } else {
            selectedBluetoothText.text = "Selected Bluetooth Device: None"
            selectedBluetoothText.visibility = TextView.VISIBLE
        }
    }
}
