package com.leehakjun.gohome

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName.toString()

            // 티맵 앱의 패키지 이름 확인
            val isTMap = packageName.equals("com.skt.skaf.l001mtm091", ignoreCase = true) ||
                    packageName.equals("com.skt.tmap.ku", ignoreCase = true)

            // MainActivity의 인스턴스를 사용하여 타이머를 제어
            val mainActivity = MainActivity.getInstance()
            if (isTMap) {
                // 티맵 앱이 최상단에 있을 경우 타이머 일시 정지
                mainActivity?.pauseTimer()
            } else {
                // 티맵 앱이 최상단에 있지 않을 경우 타이머 재시작
                mainActivity?.resumeTimer()
            }
        }
    }

    override fun onInterrupt() {
        // 필요한 처리 구현
    }
}
