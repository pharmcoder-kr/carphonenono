package com.leehakjun.gohome

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.lang.reflect.Method

class BluetoothDetectionService : Service() {
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

        // Bluetooth 상태 변경 이벤트 수신 등록
        val filter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        // 나머지 코드 생략

        return START_STICKY
    }
    private val TAG = "BluetoothDetection"

    private val NOTIFICATION_CHANNEL_ID = "BluetoothDetectionChannel"
    private val NOTIFICATION_CHANNEL_NAME = "Bluetooth Detection"
    private val NOTIFICATION_ID = 12345

    private fun handleBluetoothConnected(context: Context?, device: BluetoothDevice?) {
        // 연결된 장치의 주소와 target Bluetooth 장치의 주소 비교하기
        val sharedPreferences = context?.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val targetBluetoothDeviceAddress = sharedPreferences?.getString("bluetooth_device_address", "")
        if (device?.address == targetBluetoothDeviceAddress) {
            Log.d(TAG, "Connected to target Bluetooth device: ${device?.name} - ${device?.address}")
            // 여기서 필요한 로직 실행
            // SharedPreferences에 저장된 target 블루투스의 주소와 일치하면 com.leehakjun.gohome 어플리케이션을 띄웁니다.
            val packageName = "com.leehakjun.gohome"
            val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                // startButton을 클릭하는 동작을 추가한 인텐트를 전달
                putExtra("autoStartBluetooth", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context?.startActivity(mainActivityIntent)
            if (mainActivityIntent.resolveActivity(context!!.packageManager) != null) {
                Log.d(TAG, "Launching $packageName...")
                context.startActivity(mainActivityIntent)
                Log.d(TAG, "$packageName started.")
            } else {
                Log.e(TAG, "$packageName not found.")
            }
        }
    }

    private fun handleBluetoothDisconnected(context: Context?, device: BluetoothDevice?) {
        val sharedPreferences = context?.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val targetBluetoothDeviceAddress = sharedPreferences?.getString("bluetooth_device_address", "")
        if (device?.address == targetBluetoothDeviceAddress) {
            Log.d(TAG, "Disconnected from target Bluetooth device: ${device?.name} - ${device?.address}")
            // 브로드캐스트 인텐트 생성 및 전송
            val intent = Intent("com.leehakjun.gohome.ACTION_STOP")
            context?.sendBroadcast(intent)
        }
    }









    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_CONNECTED -> {
                            // Bluetooth가 연결된 경우
                            Log.d(TAG, "Bluetooth connected")
                            // 연결된 장치의 정보 가져오기
                            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            handleBluetoothConnected(context, device)
                        }
                        BluetoothAdapter.STATE_DISCONNECTED -> {
                            // Bluetooth가 연결이 끊긴 경우
                            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            handleBluetoothDisconnected(context, device)
                        }
                    }
                }
            }
        }
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val method: Method = device.javaClass.getMethod("isConnected")
            val connected = method.invoke(device) as Boolean
            connected
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}
