package com.leehakjun.gohome

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
        val devices = mutableListOf<Pair<String, String>>() // Pair를 사용하여 이름과 주소를 함께 저장합니다.
        for (device in pairedDevices) {
            devices.add(Pair(device.name, device.address)) // 이름과 주소를 Pair로 묶어 리스트에 추가합니다.
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices.map { it.first }) // 이름만 리스트뷰에 표시합니다.
        deviceListView.adapter = adapter

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = devices[position]
            saveSelectedBluetoothDevice(selectedDevice.first, selectedDevice.second) // 이름과 주소를 함께 저장합니다.
            updateSelectedBluetoothText(selectedDevice.first)
            Toast.makeText(this, "Selected device: ${selectedDevice.first}", Toast.LENGTH_SHORT).show()
            onBackPressed()
        }
    }

    private fun saveSelectedBluetoothDevice(deviceName: String, deviceAddress: String) {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("bluetooth_device", deviceName)
        editor.putString("bluetooth_device_address", deviceAddress) // 주소도 함께 저장합니다.
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

    override fun onResume() {
        super.onResume()
        updateButtonText()
    }

    private fun updateButtonText() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val selectedBluetoothName = sharedPreferences.getString("bluetooth_device", "")
        val selectedBluetoothDeviceAddress = sharedPreferences.getString("bluetooth_device_address", "")
        Log.d("BluetoothSettingsActivity", "Selected Bluetooth Device Name: $selectedBluetoothName")
        Log.d("BluetoothSettingsActivity", "Selected Bluetooth Device Address: $selectedBluetoothDeviceAddress")

        if (selectedBluetoothName.isNullOrEmpty()) {
            selectedBluetoothText.text = "선택된 차량 블루투스가 없습니다"
        } else {
            selectedBluetoothText.text = "선택된 차량 블루투스: $selectedBluetoothName"
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }

}
