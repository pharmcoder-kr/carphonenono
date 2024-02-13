package com.leehakjun.gohome

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver : BroadcastReceiver() {

    private val TAG = "BluetoothReceiver"
    private lateinit var targetBluetoothDeviceAddress: String

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            val deviceAddress = device?.address
            if (deviceAddress == targetBluetoothDeviceAddress) {
                Log.d(TAG, "Target Bluetooth device connected.")
                startApp(context)
            }
        }
    }

    fun setTargetBluetoothDeviceAddress(address: String) {
        targetBluetoothDeviceAddress = address
    }

    private fun startApp(context: Context?) {
        val launchIntent = context?.packageManager?.getLaunchIntentForPackage(context.packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}
