package com.leehakjun.gohome

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.Settings
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

    companion object {
        private const val REQUEST_CODE = 101
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
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

    private val settingsChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.leehakjun.gohome.SETTINGS_CHANGED") {
                updateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_CODE)
        }

        val serviceIntent = Intent(this, BluetoothDetectionService::class.java)
        startService(serviceIntent)

        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
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

        Log.d("MainActivity", "isTimerRunning: $isTimerRunning")
        Log.d("MainActivity", "isAutoStartEnabled: ${isAutoStartEnabled()}")

        // 시작 버튼 클릭 리스너 설정
        startButton.setOnClickListener {
            if (!isTimerRunning) {
                checkWorkTimeAndLaunchShortcut()
                startTimer()
                isTimerRunning = true
            }
        }

        // 자동 시작 기능이 활성화되어 있고, 현재 시간이 출퇴근 시간 범위 내에 있으면 시작 버튼 클릭
        if (!isTimerRunning && isAutoStartEnabled() && isWithinWorkTime()) {
            checkWorkTimeAndLaunchShortcut()
            startButton.performClick()
        }

        // 종료 버튼 클릭 리스너 설정
        stopButton.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
                isTimerRunning = false
            }
        }

        // 현재 시간이 출퇴근 시간 범위에 들어가지 않으면 버튼 비활성화 및 색상 변경
        if (!isWithinWorkTime()) {
            disableButtons()
        }
    }

    private fun checkWorkTimeAndLaunchShortcut() {
        val currentTime = getCurrentTime()
        val workStartTime = sharedPreferences.getString("startTime", "00:00")
        val workEndTime = sharedPreferences.getString("endTime", "12:00")
        val workStartTime2 = sharedPreferences.getString("startTime2", "12:00")
        val workEndTime2 = sharedPreferences.getString("endTime2", "24:00")

        workStartTime?.let { start ->
            workEndTime?.let { end ->
                if (currentTime in start..end) {
                    launchShortcut("회사")
                }
            }
        }

        workStartTime2?.let { start2 ->
            workEndTime2?.let { end2 ->
                if (currentTime in start2..end2) {
                    launchShortcut("우리집")
                }
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

    private fun launchShortcut(shortcutType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val packageName: String
        val data: String
        if (shortcutType == "우리집") {
            packageName = "com.skt.tmap.ku"
            data = "tmap://goto?code=1"
        } else {
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

    private fun isWithinWorkTime(): Boolean {
        val currentTime = getCurrentTime()
        val workStartTime = sharedPreferences.getString("startTime", "00:00")
        val workEndTime = sharedPreferences.getString("endTime", "12:00")
        val workStartTime2 = sharedPreferences.getString("startTime2", "12:00")
        val workEndTime2 = sharedPreferences.getString("endTime2", "24:00")

        val isInFirstWorkTime = isInTimeRange(currentTime, workStartTime, workEndTime)
        val isInSecondWorkTime = isInTimeRange(currentTime, workStartTime2, workEndTime2)

        return isInFirstWorkTime || isInSecondWorkTime
    }

    private fun isInTimeRange(currentTime: String, startTime: String?, endTime: String?): Boolean {
        if (startTime == null || endTime == null) return false
        return currentTime in startTime..endTime
    }

    private fun disableButtons() {
        startButton.isEnabled = false
        stopButton.isEnabled = false
        startButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        stopButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction("com.leehakjun.gohome.SETTINGS_CHANGED")
        }
        registerReceiver(screenStateReceiver, filter)
        registerReceiver(settingsChangedReceiver, filter)

        if (isTimerRunning && powerManager.isInteractive) {
            startTimer()
        }

        showRecords()

        if (!hasPermissions()) {
            showPermissionRequestDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenStateReceiver)
        unregisterReceiver(settingsChangedReceiver)

        if (isTimerRunning && !powerManager.isInteractive) {
            countDownTimer?.cancel()
        }
    }

    private fun requestPermissions() {
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
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    private fun updateUI() {
        // Update UI logic here if needed
    }

    private fun showPermissionRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Request")
            .setMessage("This app requires Bluetooth and Location permissions to function.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }
}
