package com.leehakjun.gohome

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class BluetoothDetectionService : Service() {

    private val TAG = "com.leehakjun.gohome.BluetoothDetection"

    private val NOTIFICATION_CHANNEL_ID = "BluetoothDetectionChannel"
    private val NOTIFICATION_CHANNEL_NAME = "Bluetooth Detection"
    private val NOTIFICATION_ID = 12345

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BluetoothDetectionService started")

        // Foreground Service로 설정
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 블루투스 상태 감지 및 처리
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.d(TAG, "BluetoothAdapter is null. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val targetBluetoothDeviceAddress = sharedPreferences.getString("bluetooth_device_address", "")

        if (bluetoothAdapter.isEnabled && !targetBluetoothDeviceAddress.isNullOrEmpty()) {
            Log.d(TAG, "BluetoothAdapter is enabled and target Bluetooth device name is not empty")

            // 연결 상태 확인
            var isDeviceConnected = false
            for (device in bluetoothAdapter.bondedDevices) {
                val bondState = device.bondState
                if (device.address == targetBluetoothDeviceAddress && bondState == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "Target Bluetooth device connected: ${device.name} - ${device.address}")
                    isDeviceConnected = true
                    break
                }
            }

            if (!isDeviceConnected) {
                Log.d(TAG, "Target Bluetooth device is not connected.")

                // 디바이스 목록 확인
                for (device in bluetoothAdapter.bondedDevices) {
                    Log.d(TAG, "Bonded device: ${device.name} - ${device.address}")
                }
            }
        } else {
            Log.d(TAG, "BluetoothAdapter is not enabled or target Bluetooth device name is empty")
        }

        return START_NOT_STICKY
    }

    private fun startApp() {
        Log.d(TAG, "Starting the application")
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "Failed to start the application", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Bluetooth Detection Service")
            .setContentText("Running...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }
}
