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
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var missionTextView: TextView
    private lateinit var powerManager: PowerManager
    private lateinit var sharedPreferences: SharedPreferences
    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var elapsedTime: Long = 0L
    private var recordText: String = ""
    private lateinit var circularProgressBar: CircularProgressBar
    private lateinit var timeRemainingTextView: TextView

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

    private val screenUnlockedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                startButton.isEnabled = false // startButton 비활성화
                stopButton.isEnabled = false // stopButton 비활성화
                startButton.postDelayed({
                    startButton.isEnabled = true // 1초 후 startButton 활성화
                    stopButton.isEnabled = true // 1초 후 stopButton 활성화
                }, 1000)
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
        missionTextView = findViewById(R.id.missionTextView) // 미션 텍스트뷰 초기화

        val settingsButton: Button = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // 시작 버튼 클릭 리스너 설정
        startButton.setOnClickListener {
            if (!isTimerRunning) {
                startTimer()
                checkWorkTimeAndLaunchShortcut()
                isTimerRunning = true
                updateUI() // UI 업데이트
                // CircularProgressBar를 초록색으로 초기화
                circularProgressBar.progressBarColor = ContextCompat.getColor(this, R.color.green)
            }

        }
        circularProgressBar = findViewById(R.id.circularProgressBar)
        timeRemainingTextView = findViewById(R.id.timeRemainingTextView)

        // CircularProgressBar를 100%로 초기화
        circularProgressBar.progress = 100f

        // timeRemainingTextView의 디폴트 텍스트 설정
        timeRemainingTextView.text = "카폰노노\n1분미션\n타이머"
        // 종료 버튼 클릭 리스너 설정
        stopButton.setOnClickListener {
            if (isTimerRunning) {

                isTimerRunning = false
                // 미션 결과 확인 및 업데이트
                val missionSuccess = if (elapsedTime <= 60000L) true else false
                Log.d("ElapsedTime", "Elapsed Time: $elapsedTime") // elapsedTime 값 확인
                Log.d("MissionSuccess", "Mission Success: $missionSuccess") // missionsuccess 값 확인
                updateTimerText(elapsedTime)
                updateMissionText(missionSuccess) // 미션 텍스트 업데이트
                stopTimer()
            }
        }


        // 현재 시간이 출퇴근 시간 범위에 들어가지 않으면 버튼 비활성화 및 색상 변경
        if (!isWithinWorkTime()) {
            disableButtons()
        }
        // 타겟 블루투스가 연결되었을 때의 동작 추가
        if (intent.hasExtra("autoStartBluetooth")) {
            if (intent.getBooleanExtra("autoStartBluetooth", false)) {
                startButton.performClick() // startButton 클릭 이벤트 프로그래밍적으로 트리거
            }
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
                updateCircularProgressBar(60000L - elapsedTime)
            }

            override fun onFinish() {
                // Not applicable for a countdown timer that runs indefinitely.
            }
        }
        countDownTimer?.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        elapsedTime = 0L
        updateTimerText(elapsedTime)
    }
    private fun updateCircularProgressBar(millisUntilFinished: Long) {
        if (millisUntilFinished > 0) {
            val progress = millisUntilFinished.toFloat() / 60000 * 100
            circularProgressBar.progress = progress
            val remainingSeconds = millisUntilFinished / 1000
            timeRemainingTextView.text = "${remainingSeconds}초"
        } else {
            circularProgressBar.progress = 100f // 100%로 설정
            circularProgressBar.progressBarColor = ContextCompat.getColor(this, R.color.red) // 빨간색으로 설정
            timeRemainingTextView.text = "미션 실패"
        }
    }

    private fun updateTimerText(time: Long) {
        val seconds = time / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        elapsedTimeTextView.text = formattedTime
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
            addAction(Intent.ACTION_USER_PRESENT) // 화면 잠금 해제 이벤트 감지
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction("com.leehakjun.gohome.SETTINGS_CHANGED")
        }
        registerReceiver(screenUnlockedReceiver, filter) // BroadcastReceiver 등록
        registerReceiver(screenStateReceiver, filter)
        registerReceiver(settingsChangedReceiver, filter)

        if (isTimerRunning && powerManager.isInteractive) {
            startTimer()
        }

        if (!hasPermissions()) {
            showPermissionRequestDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenUnlockedReceiver) // BroadcastReceiver 해제
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
        // 미션 결과를 초기화하기 위해 start 버튼이 클릭될 때 호출
        missionTextView.text = ""
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

    private fun isAutoStartEnabled(): Boolean {
        return sharedPreferences.getBoolean("autoStartEnabled", false)
    }

    private fun updateMissionText(missionSuccess: Boolean) {
        if (missionSuccess) {
            missionTextView.text = "축하합니다! 1분 미션 성공!"
            missionTextView.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            missionTextView.text = "1분 미션 실패!"
            missionTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
        }
    }
}
