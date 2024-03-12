package com.leehakjun.gohome

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.mikhaellopez.circularprogressbar.CircularProgressBar

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 오버레이 뷰의 초기 위치를 설정
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.addView(overlayView, params)

        val closeButton = overlayView.findViewById<ImageView>(R.id.closeButton)
        closeButton.setOnClickListener {
            stopSelf()
        }

        // 오버레이 뷰를 드래그하여 이동할 수 있게 처리
        overlayView.setOnTouchListener { view, motionEvent ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    offsetX = motionEvent.rawX - layoutParams.x
                    offsetY = motionEvent.rawY - layoutParams.y
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = (motionEvent.rawX - offsetX).toInt()
                    layoutParams.y = (motionEvent.rawY - offsetY).toInt()
                    windowManager.updateViewLayout(overlayView, layoutParams)
                }
            }
            false // 이벤트가 여기서 종료되지 않고, 다른 처리(예: 클릭)를 위해 계속 전파되도록 함
        }

        val filter = IntentFilter("com.leehakjun.gohome.PROGRESS_UPDATE")
        registerReceiver(progressReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(progressReceiver)
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 필요한 초기화 로직을 여기에 추가
        // 예를 들어, 상태를 초기화하거나, 인텐트로부터 받은 데이터로 UI를 업데이트할 수 있습니다.
        resetUIComponentsToInitialState()

        // 이 서비스가 시스템에 의해 강제 종료된 후 재시작될 때, 마지막으로 전달된 인텐트로 onStartCommand()를 호출하려면 START_REDELIVER_INTENT를 반환
        // 인텐트 없이 서비스를 재시작하려면 START_STICKY를 반환
        // 서비스를 재시작하지 않으려면 START_NOT_STICKY를 반환
        return START_STICKY
    }

    private fun resetUIComponentsToInitialState() {
        // UI 컴포넌트를 초기 상태로 설정하는 로직
        // 예: Broadcast 인텐트를 사용하여 OverlayView 내의 CircularProgressBar와 remainingTime을 초기화
        val resetIntent = Intent("com.leehakjun.gohome.RESET_UI")
        resetIntent.putExtra("progress", 100f) // CircularProgressBar를 100%로 초기화
        resetIntent.putExtra("remainingTime", "시작하세요") // 초기 남은 시간 텍스트 설정
        sendBroadcast(resetIntent)
    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action == "com.leehakjun.gohome.PROGRESS_UPDATE") {
                val progress = intent.getFloatExtra("progress", 0f)
                val remainingTime = intent.getStringExtra("remainingTime")
                updateCircularProgressBar(progress)
                updateTimeRemainingTextView(remainingTime)
            }
        }
    }

    private fun updateCircularProgressBar(progress: Float) {
        val circularProgressBar = overlayView.findViewById<CircularProgressBar>(R.id.circularProgressBarOverlay)
        circularProgressBar.progress = progress
    }

    private fun updateTimeRemainingTextView(remainingTime: String?) {
        val timeRemainingTextView = overlayView.findViewById<TextView>(R.id.timeRemainingTextViewOverlay)
        timeRemainingTextView.text = remainingTime ?: ""
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
