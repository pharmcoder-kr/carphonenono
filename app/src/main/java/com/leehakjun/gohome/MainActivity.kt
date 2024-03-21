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
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
    // stopReceiver의 새로운 구현
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // stop 버튼 클릭 이벤트를 수동으로 처리
            if (isTimerRunning) {
                runOnUiThread {
                    stopButton.performClick()
                }
            }

            // MainActivity를 전면으로 가져오는 Intent 생성 및 실행
            val showIntent = Intent(context, MainActivity::class.java)
            showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            showIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context?.startActivity(showIntent)
        }
    }
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.getStringExtra("ScreenState")) {
                "SCREEN_OFF" -> if (isTimerRunning) {
                    // 타이머 일시정지 로직
                    countDownTimer?.cancel()
                }
                "SCREEN_ON" -> if (isTimerRunning) {
                    // 타이머 재시작 로직
                    startTimer()
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


                // OverlayService 시작
                val overlayIntent = Intent(this, OverlayService::class.java)
                startService(overlayIntent)


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

                val overlayIntent = Intent(this, OverlayService::class.java)
                stopService(overlayIntent)
            }

        }

        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            // 사용자에게 종료 여부를 확인하는 AlertDialog 생성 및 표시
            AlertDialog.Builder(this).apply {
                setTitle("앱 종료") // 팝업 제목
                setMessage("앱을 종료하시겠습니까?") // 팝업 메시지
                setPositiveButton("예") { dialog, which ->
                    finishAndRemoveTask() // 사용자가 '예'를 선택하면 앱 종료
                }
                setNegativeButton("아니오") { dialog, which ->
                    dialog.dismiss() // 사용자가 '아니오'를 선택하면 팝업만 닫힘
                }
                create().show() // AlertDialog를 생성하고 표시
            }
        }

        // 현재 시간이 출퇴근 시간 범위에 들어가지 않으면 버튼 비활성화 및 색상 변경

        // 타겟 블루투스가 연결되었을 때의 동작 추가
        if (intent.hasExtra("autoStartBluetooth")) {
            if (intent.getBooleanExtra("autoStartBluetooth", false)) {
                startButton.performClick() // startButton 클릭 이벤트 프로그래밍적으로 트리거
            }
        }
        // 타겟 블루투스가 연결 해제되었을 때의 동작 추가
        if (intent.hasExtra("autoStopBluetooth")) {
            if (intent.getBooleanExtra("autoStopBluetooth", false)) {
                stopButton.performClick() // stopButton 클릭 이벤트 프로그래밍적으로 트리거
            }
        }

        // ScreenStateService 시작
        val screenStateServiceIntent = Intent(this, ScreenStateService::class.java)
        startService(screenStateServiceIntent)

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(
            screenStateReceiver,
            IntentFilter("com.leehakjun.gohome.SCREEN_STATE")
        )
        // 브로드캐스트 리시버 등록
        val filter = IntentFilter("com.leehakjun.gohome.ACTION_STOP")
        registerReceiver(stopReceiver, filter)
        // stopReceiver 등록
        val stopFilter = IntentFilter("com.leehakjun.gohome.ACTION_STOP")
        registerReceiver(stopReceiver, stopFilter)
    }

    // CircularProgressBar와 TextView의 값을 Overlay에 전송하는 함수
// CircularProgressBar와 TextView의 값을 Overlay에 전송하는 함수
    private fun sendValuesToOverlay() {
        val progress = circularProgressBar.progress
        val remainingTime = timeRemainingTextView.text.toString()

        // Log.d 추가하여 전송되는 값 로깅
        Log.d("OverlayUpdate", "Sending to overlay: Progress - $progress, Remaining Time - $remainingTime")
        val intent = Intent("com.leehakjun.gohome.PROGRESS_UPDATE")
        intent.putExtra("progress", circularProgressBar.progress)
        intent.putExtra("remainingTime", timeRemainingTextView.text)

        // 미션 실패 조건에 따라 배경색 변경 신호 추가
        intent.putExtra("missionFailed", circularProgressBar.progress == 100f)
        sendBroadcast(intent) // LocalBroadcastManager.getInstance(this).sendBroadcast(intent) 대신 사용
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("autoStartBluetooth", false)) {
            startButton.performClick()
        }
    }
    private fun checkWorkTimeAndLaunchShortcut() {
        val currentTime = getCurrentTime()
        val workStartTime = sharedPreferences.getString("startTime", "00:00")
        val workEndTime = sharedPreferences.getString("endTime", "12:00")
        val workStartTime2 = sharedPreferences.getString("startTime2", "12:00")
        val workEndTime2 = sharedPreferences.getString("endTime2", "24:00")

        val isInFirstWorkPeriod = workStartTime?.let { start ->
            workEndTime?.let { end ->
                currentTime in start..end
            }
        } ?: false

        val isInSecondWorkPeriod = workStartTime2?.let { start2 ->
            workEndTime2?.let { end2 ->
                currentTime in start2..end2
            }
        } ?: false

        when {
            isInFirstWorkPeriod -> launchShortcut("회사")
            isInSecondWorkPeriod -> launchShortcut("우리집")
            else -> launchTmapMainPage()
        }
    }

    private fun launchTmapMainPage() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName("com.skt.tmap.ku", "com.skt.tmap.activity.TmapIntroActivity")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LaunchTmapMainPage", "Error launching Tmap main page: ${e.message}")
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
                sendValuesToOverlay() // 타이머의 틱마다 Overlay로 현재 상태 업데이트
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
            .setMessage("이 어플은 블루투스와 위치권한을 사용합니다.")
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
    override fun onDestroy() {
        super.onDestroy()
        // 서비스 종료 인텐트 생성
        val serviceIntent = Intent(this, OverlayService::class.java)
        // 서비스 종료
        stopService(serviceIntent)
        countDownTimer?.cancel() // 타이머 중지
        // BroadcastReceiver 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(screenStateReceiver)
        unregisterReceiver(stopReceiver) // stopReceiver 해제
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

