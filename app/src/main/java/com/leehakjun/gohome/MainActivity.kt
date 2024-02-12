package com.leehakjun.gohome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

        // 블루투스 및 위치 권한 요청
        requestPermissions()

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

        if (!isTimerRunning && isAutoStartEnabled()) {
            startButton.performClick()
        }

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
                            launchShortcut("회사") // 출근 시간에 "회사" 숏컷 실행
                        }
                    }
                }

                // 퇴근 시간 범위 확인
                workStartTime2?.let { start2 ->
                    workEndTime2?.let { end2 ->
                        if (currentTime in start2..end2) {
                            launchShortcut("우리집") // 퇴근 시간에 "우리집" 숏컷 실행
                        }
                    }
                }

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

    private fun launchShortcut(shortcutType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val packageName: String
        val data: String
        if (shortcutType == "우리집") {
            packageName = "com.skt.tmap.ku"
            data = "tmap://goto?code=1"
        } else { // "회사" 숏컷
            packageName = "com.skt.tmap.ku"
            data = "tmap://goto?code=2"
        }
        intent.setClassName(packageName, "com.skt.tmap.ku.IntroActivity")
        intent.data = Uri.parse(data)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LaunchShortcut", "Error launching shortcut: ${e.message}")
        }
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

        // 사용자에게 권한 요청 다이얼로그를 다시 표시
        if (!hasPermissions()) {
            showPermissionRequestDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenStateReceiver)

        if (isTimerRunning && !powerManager.isInteractive) {
            countDownTimer?.cancel()
        }
    }

    private fun requestPermissions() {
        // 블루투스 및 위치 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                101
            )
        }
    }

    private fun hasPermissions(): Boolean {
        // 퍼미션이 모두 허용되었는지 확인
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    private fun showPermissionRequestDialog() {
        // 사용자에게 권한을 요청하는 다이얼로그 표시
        AlertDialog.Builder(this)
            .setTitle("권한 요청")
            .setMessage("이 앱을 사용하기 위해서는 블루투스 및 위치 권한이 필요합니다.")
            .setPositiveButton("확인") { _, _ ->
                // 사용자가 확인 버튼을 누를 경우, 권한을 다시 요청
                requestPermissions()
            }
            .setNegativeButton("취소") { dialog, _ ->
                // 사용자가 취소 버튼을 누를 경우, 앱을 종료하거나 추가적인 처리를 수행할 수 있음
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }
}
