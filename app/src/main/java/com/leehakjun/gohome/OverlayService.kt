package com.leehakjun.gohome

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
        resetOverlayBackgroundToInitial()
        // 기타 초기화 로직...

        return START_STICKY
    }

    private fun resetOverlayBackgroundToInitial() {
        val overlayLayout = overlayView.findViewById<ConstraintLayout>(R.id.overlayLayout)
        // 매번 새로운 Drawable 인스턴스를 생성하여 배경으로 설정
        val newBackground = ContextCompat.getDrawable(this, R.drawable.rounded_background) as GradientDrawable
        // 초록색으로 배경색 설정, 여기서 R.color.green은 colors.xml에 정의된 색상 리소스
        newBackground.setColor(ContextCompat.getColor(this, R.color.roundedGreen))
        overlayLayout.background = newBackground
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
                val missionFailed = intent.getBooleanExtra("missionFailed", false)
                updateCircularProgressBar(progress)
                updateTimeRemainingTextView(remainingTime)

                if (missionFailed) {
                    val overlayLayout = overlayView.findViewById<ConstraintLayout>(R.id.overlayLayout)
                    val background = overlayLayout.background as? GradientDrawable
                    background?.setColor(ContextCompat.getColor(this@OverlayService, R.color.deepPink))
                }

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
