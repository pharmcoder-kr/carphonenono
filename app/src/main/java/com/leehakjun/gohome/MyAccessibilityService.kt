package com.leehakjun.gohome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MyAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName.toString()

            if (packageName == "com.skt.tmap.ku") {
                // 티맵이 활성화되었을 때
                pauseTimerInMainActivity()
            } else {
                // 티맵이 활성화되지 않았을 때
                resumeTimerInMainActivity()
            }
        }
    }

    override fun onInterrupt() {
    }

    private fun pauseTimerInMainActivity() {
        val pauseIntent = Intent("com.leehakjun.gohome.TIMER_PAUSE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(pauseIntent)
    }

    private fun resumeTimerInMainActivity() {
        val resumeIntent = Intent("com.leehakjun.gohome.TIMER_RESUME")
        LocalBroadcastManager.getInstance(this).sendBroadcast(resumeIntent)
    }
}
