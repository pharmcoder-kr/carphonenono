package com.leehakjun.gohome

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.skt.Tmap.TMapTapi

class MainActivity : AppCompatActivity() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var recordTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var autoStartToggleButton: ToggleButton
    private lateinit var routineButton: Button // Added routineButton

    private lateinit var powerManager: PowerManager
    private lateinit var sharedPreferences: SharedPreferences

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var elapsedTime: Long = 0L
    private var recordText: String = ""

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen off, pause the timer
                    if (isTimerRunning) {
                        countDownTimer?.cancel()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Screen on, restart the timer if auto-start is enabled
                    if (isTimerRunning && isAutoStartEnabled()) {
                        startTimer()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        elapsedTimeTextView = findViewById(R.id.elapsedTimeTextView)
        recordTextView = findViewById(R.id.recordTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        autoStartToggleButton = findViewById(R.id.autoStartToggleButton)
        routineButton = findViewById(R.id.routineButton) // Initialize the routineButton

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        sharedPreferences = getPreferences(Context.MODE_PRIVATE)

        autoStartToggleButton.isChecked = isAutoStartEnabled()

        startButton.setOnClickListener {
            if (!isTimerRunning) {
                Log.d("TMapDebug", "Start 버튼 클릭됨")

                val tmaptapi = TMapTapi(this@MainActivity)
                tmaptapi.setSKTMapAuthentication("z28g7ycCBJawmTWhOKudu5OVQMMoV0HJ9QNtI3Wj")

                tmaptapi.setOnAuthenticationListener(object : TMapTapi.OnAuthenticationListenerCallback {
                    override fun SKTMapApikeySucceed() {
                        Log.d("TMapAuth", "API 인증 성공")
                        tmaptapi.invokeGoHome()
                        Log.d("TMapDebug", "invokeGoHome 호출됨")
                    }

                    override fun SKTMapApikeyFailed(errorMsg: String) {
                        Log.e("TMapAuth", "API 인증 실패: $errorMsg")
                    }
                })

                startTimer()
                isTimerRunning = true
            }
        }

        stopButton.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
                isTimerRunning = false
            }
        }

        autoStartToggleButton.setOnCheckedChangeListener { _, isChecked ->
            saveAutoStartState(isChecked)
        }

        routineButton.setOnClickListener {
            // Create an intent to open the "모드 및 루틴" screen
            val intent = Intent()
            intent.setClassName("com.samsung.android.app.routines", "com.samsung.android.app.routines.ui.main.RoutineLaunchActivity")

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Handle the case where the "모드 및 루틴" activity is not found
                // You can display a message or take alternative actions here
            }
        }

        if (isAutoStartEnabled() && !isTimerRunning) {
            startTimer()
            isTimerRunning = true
        }
    }

    private fun startTimer() {
        // TMap API 연동 코드 추가
        if (isAutoStartEnabled()) {
            val tmaptapi = TMapTapi(this@MainActivity)
            tmaptapi.setSKTMapAuthentication("z28g7ycCBJawmTWhOKudu5OVQMMoV0HJ9QNtI3Wj")

            tmaptapi.setOnAuthenticationListener(object : TMapTapi.OnAuthenticationListenerCallback {
                override fun SKTMapApikeySucceed() {
                    Log.d("TMapAuth", "API 인증 성공")
                    tmaptapi.invokeGoHome()
                    Log.d("TMapDebug", "invokeGoHome 호출됨")
                }

                override fun SKTMapApikeyFailed(errorMsg: String) {
                    Log.e("TMapAuth", "API 인증 실패: $errorMsg")
                }
            })
        }
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime += 1000
                updateTimerText(elapsedTime)
            }

            override fun onFinish() {
                // Not applicable for a countdown timer that runs indefinitely.
            }
        }
        countDownTimer?.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        saveRecord(elapsedTime)
        showRecords()
        elapsedTime = 0L
        updateTimerText(elapsedTime)
    }

    private fun updateTimerText(time: Long) {
        val seconds = time / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        elapsedTimeTextView.text = formattedTime
    }

    private fun saveRecord(record: Long) {
        recordText = "$recordText\n${formatRecord(record)}"
        val score = calculateScore(record)
        updateScoreText(score)
    }

    private fun formatRecord(record: Long): String {
        val seconds = record / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }

    private fun calculateScore(elapsedTime: Long): Int {
        val seconds = elapsedTime / 1000
        return when {
            seconds <= 10 -> (1000 - seconds).toInt()
            seconds <= 30 -> (1000 - (10 + (seconds - 10) * 2)).toInt()
            seconds <= 60 -> (1000 - (10 + 20 + (seconds - 30) * 3)).toInt()
            else -> (1000 - (10 + 20 + 90)).toInt()
        }
    }

    private fun updateScoreText(score: Int) {
        val scoreText = getString(R.string.score_format, score)
        scoreTextView.text = scoreText
    }

    private fun showRecords() {
        recordTextView.text = recordText
        recordTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
    }

    private fun isAutoStartEnabled(): Boolean {
        return sharedPreferences.getBoolean("autoStartEnabled", false)
    }

    private fun saveAutoStartState(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("autoStartEnabled", isEnabled).apply()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)

        if (isTimerRunning && powerManager.isInteractive) {
            startTimer()
        }

        showRecords()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenStateReceiver)

        if (isTimerRunning && !powerManager.isInteractive) {
            countDownTimer?.cancel()
        }
    }
}
