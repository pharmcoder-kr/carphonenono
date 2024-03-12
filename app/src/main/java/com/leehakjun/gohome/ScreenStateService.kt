package com.leehakjun.gohome

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ScreenStateService : Service() {

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> notifyScreenState("SCREEN_OFF")
                Intent.ACTION_SCREEN_ON -> notifyScreenState("SCREEN_ON")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
        // Optional: Convert this service to a foreground service to ensure it keeps running
    }

    private fun notifyScreenState(state: String) {
        val intent = Intent("com.leehakjun.gohome.SCREEN_STATE")
        intent.putExtra("ScreenState", state)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(screenStateReceiver)
        super.onDestroy()
    }
}
