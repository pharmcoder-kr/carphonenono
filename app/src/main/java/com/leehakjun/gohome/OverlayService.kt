package com.leehakjun.gohome

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.widget.TextView
import com.mikhaellopez.circularprogressbar.CircularProgressBar

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onCreate() {
        super.onCreate()

        // WindowManager 초기화
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Overlay 화면을 생성하고 추가
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, params)

        // BroadcastReceiver 등록
        val filter = IntentFilter("com.leehakjun.gohome.PROGRESS_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiver 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver)

        // 서비스가 종료될 때 WindowManager에서 View 제거
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }

    // MainActivity로부터 CircularProgressBar 값과 TextView 값 받기
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action == "com.leehakjun.gohome.PROGRESS_UPDATE") {
                val progress = intent.getFloatExtra("progress", 0f)
                val remainingTime = intent.getStringExtra("remainingTime")
                // CircularProgressBar와 TextView 업데이트
                updateCircularProgressBar(progress)
                updateTimeRemainingTextView(remainingTime)
            }
        }
    }

    // CircularProgressBar를 업데이트하는 함수
    private fun updateCircularProgressBar(progress: Float) {
        // CircularProgressBar를 overlayView에서 찾아옴
        val circularProgressBar = overlayView.findViewById<CircularProgressBar>(R.id.circularProgressBarOverlay)
        // CircularProgressBar 업데이트
        circularProgressBar.progress = progress
    }

    // TextView를 업데이트하는 함수
    private fun updateTimeRemainingTextView(remainingTime: String?) {
        // TextView를 overlayView에서 찾아옴
        val timeRemainingTextView = overlayView.findViewById<TextView>(R.id.timeRemainingTextViewOverlay)
        // TextView 업데이트
        timeRemainingTextView.text = remainingTime
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
