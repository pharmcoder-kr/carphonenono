package com.leehakjun.gohome

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.skt.Tmap.TMapTapi
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var recordTextView: TextView
    private lateinit var scoreTextView: TextView

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
                    if (isTimerRunning) {
                        countDownTimer?.cancel()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
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

        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        elapsedTimeTextView = findViewById(R.id.elapsedTimeTextView)
        recordTextView = findViewById(R.id.recordTextView)
        scoreTextView = findViewById(R.id.scoreTextView)

        val settingsButton: Button = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        startButton.setOnClickListener {
            if (!isTimerRunning) {
                val currentTime = getCurrentTime()
                val workStartTime = sharedPreferences.getString("startTime", "")
                val workEndTime = sharedPreferences.getString("endTime", "")
                val workStartTime2 = sharedPreferences.getString("startTime2", "")
                val workEndTime2 = sharedPreferences.getString("endTime2", "")

                // 출근 시간 범위 확인
                workStartTime?.let { start ->
                    workEndTime?.let { end ->
                        if (currentTime in start..end) {
                            startNavigation(true) // 출근 시간에 TMap GoCompany 실행
                        }
                    }
                }

                // 퇴근 시간 범위 확인
                workStartTime2?.let { start2 ->
                    workEndTime2?.let { end2 ->
                        if (currentTime in start2..end2) {
                            startNavigation(false) // 퇴근 시간에 TMap GoHome 실행
                        }
                    }
                }

                if (!isTimerRunning) {
                    startTimer()
                    isTimerRunning = true
                }
            }
        }

        stopButton.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
                isTimerRunning = false
            }
        }

        if (isAutoStartEnabled() && !isTimerRunning) {
            startButton.performClick()
            isTimerRunning = true
        }
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", currentHour, currentMinute)
    }

    private fun startTimer() {
        // 기존 타이머 설정 코드
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
        return sharedPreferences.getBoolean("autoStartEnabled", true)
    }

    private fun startNavigation(isGoingToWork: Boolean) {
        Log.d("NavigationCheck", "startNavigation 호출됨, isGoingToWork: $isGoingToWork")

        // TMapTapi 인스턴스 초기화 및 인증
        val tmaptapi = TMapTapi(this)
        tmaptapi.setSKTMapAuthentication("z28g7ycCBJawmTWhOKudu5OVQMMoV0HJ9QNtI3Wj")

        // TMap API 인증 리스너 설정
        tmaptapi.setOnAuthenticationListener(object : TMapTapi.OnAuthenticationListenerCallback {
            override fun SKTMapApikeySucceed() {
                // 인증 성공 시 로그 출력
                Log.d("TMapAuth", "API 인증 성공")

                // TMap 인증이 성공한 후에 TMap 연동을 시작할 수 있습니다.
                if (isGoingToWork) {
                    Log.d("NavigationCheck", "invokeGoCompany 호출 전")
                    tmaptapi.invokeGoCompany()
                    Log.d("NavigationCheck", "invokeGoCompany 호출 후")
                } else {
                    Log.d("NavigationCheck", "invokeGoHome 호출 전")
                    tmaptapi.invokeGoHome()
                    Log.d("NavigationCheck", "invokeGoHome 호출 후")
                }
            }

            override fun SKTMapApikeyFailed(errorMsg: String) {
                // 인증 실패 시 로그 출력
                Log.e("TMapAuth", "API 인증 실패: $errorMsg")
            }
        })
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
