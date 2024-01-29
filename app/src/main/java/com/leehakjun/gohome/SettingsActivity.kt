package com.leehakjun.gohome

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var autoStartToggleButton: ToggleButton
    private lateinit var routineButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        autoStartToggleButton = findViewById(R.id.autoStartToggleButton)
        autoStartToggleButton.isChecked = isAutoStartEnabled()
        autoStartToggleButton.setOnCheckedChangeListener { _, isChecked ->
            saveAutoStartState(isChecked)
        }

        routineButton = findViewById(R.id.routineButton)
        routineButton.setOnClickListener {
            val intent = Intent()
            intent.setClassName("com.samsung.android.app.routines", "com.samsung.android.app.routines.ui.main.RoutineLaunchActivity")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // "모드 및 루틴" 액티비티를 찾을 수 없는 경우 처리
            }
        }
    }

    private fun isAutoStartEnabled(): Boolean {
        return sharedPreferences.getBoolean("autoStartEnabled", false)
    }

    private fun saveAutoStartState(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("autoStartEnabled", isEnabled).apply()
    }
}
