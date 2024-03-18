package com.leehakjun.gohome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MyAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("AccessibilityService", "Event received: ${event?.eventType}, Package: ${event?.packageName}")

        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName.toString()
                if (packageName == "com.skt.tmap.ku") {
                    Log.d("AccessibilityService", "TMap is active - pausing timer")
                    pauseTimerInMainActivity()
                } else {
                    Log.d("AccessibilityService", "TMap is not active - resuming timer")
                    resumeTimerInMainActivity()
                }
            }

        }
    }

    override fun onInterrupt() {
    }

    private fun pauseTimerInMainActivity() {
        val pauseIntent = Intent("com.leehakjun.gohome.TIMER_PAUSE")
        sendBroadcast(pauseIntent)
    }

    private fun resumeTimerInMainActivity() {
        Log.d("AccessibilityService", "Sending TIMER_RESUME intent")
        val resumeIntent = Intent("com.leehakjun.gohome.TIMER_RESUME")
        sendBroadcast(resumeIntent)
    }

}
