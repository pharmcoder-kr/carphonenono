package com.leehakjun.gohome

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var autoStartToggleButton: ToggleButton
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private lateinit var startTimeEditText2: EditText // 퇴근 시작 설정 EditText 추가
    private lateinit var endTimeEditText2: EditText // 퇴근 종료 설정 EditText 추가
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var routineButton: Button // 루틴 설정 버튼 추가
    private lateinit var resetButton: Button // 출퇴근 시간 설정 초기화 버튼 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        autoStartToggleButton = findViewById(R.id.autoStartToggleButton)
        autoStartToggleButton.isChecked = isAutoStartEnabled()
        autoStartToggleButton.setOnCheckedChangeListener { _, isChecked ->
            saveAutoStartState(isChecked)
        }

        startTimeEditText = findViewById(R.id.startTimeEditText)
        endTimeEditText = findViewById(R.id.endTimeEditText)
        startTimeEditText2 = findViewById(R.id.startTimeEditText2) // 퇴근 시작 설정 EditText 연결
        endTimeEditText2 = findViewById(R.id.endTimeEditText2) // 퇴근 종료 설정 EditText 연결

        routineButton = findViewById(R.id.routineButton) // 루틴 설정 버튼 연결
        routineButton.setOnClickListener {
            // 모드 및 루틴 어플로 연결하는 코드
            val intent = Intent()
            intent.setClassName("com.samsung.android.app.routines", "com.samsung.android.app.routines.ui.main.RoutineLaunchActivity")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // "모드 및 루틴" 액티비티를 찾을 수 없는 경우 처리
            }
        }

        resetButton = findViewById(R.id.resetButton) // 출퇴근 시간 설정 초기화 버튼 연결
        resetButton.setOnClickListener {
            sharedPreferences.edit().clear().apply() // 설정값 초기화
            // EditText 초기화
            startTimeEditText.setText("00:00")
            endTimeEditText.setText("12:00")
            startTimeEditText2.setText("12:00")
            endTimeEditText2.setText("23:59")
        }

        // 이전에 설정한 값이 있다면 가져와서 표시
        val savedStartTime = sharedPreferences.getString("startTime", "00:00")
        val savedEndTime = sharedPreferences.getString("endTime", "12:00")
        val savedStartTime2 = sharedPreferences.getString("startTime2", "12:00") // 퇴근 시작 설정 값 가져오기
        val savedEndTime2 = sharedPreferences.getString("endTime2", "23:59") // 퇴근 종료 설정 값 가져오기

        startTimeEditText.setText(savedStartTime)
        endTimeEditText.setText(savedEndTime)
        startTimeEditText2.setText(savedStartTime2) // 퇴근 시작 설정 값 설정
        endTimeEditText2.setText(savedEndTime2) // 퇴근 종료 설정 값 설정

        startTimeEditText.setOnClickListener {
            showTimePickerDialog(startTimeEditText)
        }

        endTimeEditText.setOnClickListener {
            showTimePickerDialog(endTimeEditText)
        }

        startTimeEditText2.setOnClickListener {
            showTimePickerDialog(startTimeEditText2) // 퇴근 시작 설정 다이얼로그 열기
        }

        endTimeEditText2.setOnClickListener {
            showTimePickerDialog(endTimeEditText2) // 퇴근 종료 설정 다이얼로그 열기
        }
    }

    private fun isAutoStartEnabled(): Boolean {
        return sharedPreferences.getBoolean("autoStartEnabled", false)
    }

    private fun saveAutoStartState(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("autoStartEnabled", isEnabled).apply()
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                editText.setText(selectedTime)
                // 시간이 설정되면 해당 시간을 저장합니다.
                when (editText) {
                    startTimeEditText -> saveStartTime(selectedTime)
                    endTimeEditText -> saveEndTime(selectedTime)
                    startTimeEditText2 -> saveStartTime2(selectedTime) // 퇴근 시작 설정 값 저장
                    endTimeEditText2 -> saveEndTime2(selectedTime) // 퇴근 종료 설정 값 저장
                }
            },
            currentHour,
            currentMinute,
            false // 24시간 형식이 아닌 AM/PM 형식으로 변경
        )
        timePickerDialog.show()
    }

    // 사용자가 시간을 설정하면 이를 저장
    override fun onPause() {
        super.onPause()
        val startTime = startTimeEditText.text.toString()
        val endTime = endTimeEditText.text.toString()
        val startTime2 = startTimeEditText2.text.toString() // 퇴근 시작 설정 값 저장
        val endTime2 = endTimeEditText2.text.toString() // 퇴근 종료 설정 값 저장

        saveStartTime(startTime)
        saveEndTime(endTime)
        saveStartTime2(startTime2) // 퇴근 시작 설정 값 저장
        saveEndTime2(endTime2) // 퇴근 종료 설정 값 저장
    }

    private fun saveStartTime(startTime: String) {
        sharedPreferences.edit().putString("startTime", startTime).apply()
    }

    private fun saveEndTime(endTime: String) {
        sharedPreferences.edit().putString("endTime", endTime).apply()
    }

    private fun saveStartTime2(startTime2: String) {
        sharedPreferences.edit().putString("startTime2", startTime2).apply() // 퇴근 시작 설정 값 저장
    }

    private fun saveEndTime2(endTime2: String) {
        sharedPreferences.edit().putString("endTime2", endTime2).apply() // 퇴근 종료 설정 값 저장
    }
}
