package com.leehakjun.gohome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class TimerControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { receivedIntent ->
            val action = when (receivedIntent.action) {
                "SOME_EXTERNAL_ACTION_PAUSE" -> "com.leehakjun.gohome.TIMER_PAUSE"
                "SOME_EXTERNAL_ACTION_RESUME" -> "com.leehakjun.gohome.TIMER_RESUME"
                else -> return
            }
            // MainActivity로 인텐트 전송
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(Intent(action))
        }
    }
}
