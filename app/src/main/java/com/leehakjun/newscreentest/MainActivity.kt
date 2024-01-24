package com.leehakjun.newscreentest

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var recordTextView: TextView
    private lateinit var scoreTextView: TextView // Added TextView for displaying the score

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var elapsedTime: Long = 0L
    private lateinit var powerManager: PowerManager
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
                    // Screen on, restart the timer
                    if (isTimerRunning) {
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
        scoreTextView = findViewById(R.id.scoreTextView) // Initialize the scoreTextView
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        startButton.setOnClickListener {
            if (!isTimerRunning) {
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

    private fun startTimer() {
        countDownTimer?.cancel() // Cancel the existing timer
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

        // Save the record and display on the screen
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
        // Concatenate the current record to the existing records
        recordText = "$recordText\n${formatRecord(record)}"
        // Calculate and display the score
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
            else -> (1000 - (10 + 20 + 90)).toInt() // 61초 이상은 초당 3점씩 감점으로 계산
        }
    }


    private fun updateScoreText(score: Int) {
        val scoreText = getString(R.string.score_format, score)
        scoreTextView.text = scoreText
    }

    private fun showRecords() {
        // Display the saved records on the screen
        recordTextView.text = recordText
        recordTextView.setTextColor(ContextCompat.getColor(this, R.color.red)) // Set the text color to red
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)

        // If the timer was running and the screen is interactive, restart the timer
        if (isTimerRunning && powerManager.isInteractive) {
            startTimer()
        }

        // Display the records when the app resumes
        showRecords()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenStateReceiver)

        // If the timer was running and the screen is off, pause the timer
        if (isTimerRunning && !powerManager.isInteractive) {
            countDownTimer?.cancel()
        }
    }
}
