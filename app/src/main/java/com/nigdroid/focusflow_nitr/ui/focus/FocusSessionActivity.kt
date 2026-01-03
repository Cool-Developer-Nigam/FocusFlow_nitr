package com.nigdroid.focusflow_nitr.ui.focus

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.model.Session
import com.nigdroid.focusflow_nitr.data.model.SessionType
import com.nigdroid.focusflow_nitr.data.repository.SessionRepository
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import com.nigdroid.focusflow_nitr.databinding.ActivityFocusSessionBinding
import com.nigdroid.focusflow_nitr.utils.toast
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FocusSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFocusSessionBinding
    private val sessionRepository = SessionRepository()
    private val userRepository = UserRepository()

    private var timer: CountDownTimer? = null
    private var sessionStartTime: Timestamp? = null
    private var totalDuration = 25 * 60 * 1000L // 25 minutes default
    private var remainingTime = totalDuration
    private var isPaused = false
    private var currentSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        startSession()
    }

    private fun setupUI() {
        binding.progressBar.max = 100
        binding.durationText.text = formatTime(totalDuration)
        binding.sessionTypeText.text = "Focus Session"
    }

    private fun setupListeners() {
        binding.pauseButton.setOnClickListener {
            if (isPaused) resumeSession() else pauseSession()
        }

        binding.stopButton.setOnClickListener {
            showStopConfirmation()
        }

        binding.addTimeButton.setOnClickListener {
            addTime(5 * 60 * 1000L) // Add 5 minutes
        }
    }

    private fun startSession() {
        sessionStartTime = Timestamp.now()

        lifecycleScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch

            val session = Session(
                userId = userId,
                goalId = "", // Can be set from goal selection
                startTime = sessionStartTime!!,
                sessionType = SessionType.STUDY
            )

            sessionRepository.createSession(session).onSuccess { createdSession ->
                currentSessionId = createdSession.sessionId
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timer?.cancel()

        timer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateUI(millisUntilFinished)
            }

            override fun onFinish() {
                completeSession()
            }
        }.start()

        binding.pauseButton.text = "Pause"
        isPaused = false
    }

    private fun pauseSession() {
        timer?.cancel()
        binding.pauseButton.text = "Resume"
        isPaused = true
    }

    private fun resumeSession() {
        startTimer()
    }

    private fun addTime(milliseconds: Long) {
        remainingTime += milliseconds
        totalDuration += milliseconds

        if (!isPaused) {
            timer?.cancel()
            startTimer()
        } else {
            updateUI(remainingTime)
        }
    }

    private fun updateUI(millisUntilFinished: Long) {
        binding.durationText.text = formatTime(millisUntilFinished)

        val progress = ((totalDuration - millisUntilFinished).toFloat() / totalDuration * 100).toInt()

        ObjectAnimator.ofInt(binding.progressBar, "progress", progress).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            start()
        }

        binding.progressText.text = "$progress%"
    }

    private fun completeSession() {
        lifecycleScope.launch {
            currentSessionId?.let { sessionId ->
                val duration = totalDuration - remainingTime
                val focusScore = calculateFocusScore(duration, totalDuration)

                // Update session in Firebase
                // You'll need to add an updateSession method to SessionRepository

                toast("Session completed! +50 points earned")
                showCompletionDialog(duration, focusScore)
            }
        }
    }

    private fun calculateFocusScore(actualDuration: Long, plannedDuration: Long): Int {
        val completionRate = (actualDuration.toFloat() / plannedDuration * 100).toInt()
        return when {
            completionRate >= 90 -> 100
            completionRate >= 75 -> 85
            completionRate >= 50 -> 70
            else -> 50
        }.coerceIn(0, 100)
    }

    private fun showStopConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("End Session?")
            .setMessage("Are you sure you want to end this session early?")
            .setPositiveButton("Yes") { _, _ ->
                completeSession()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showCompletionDialog(duration: Long, focusScore: Int) {
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60

        AlertDialog.Builder(this)
            .setTitle("🎉 Session Complete!")
            .setMessage(
                "Great work! You focused for: ${hours}h ${minutes}m\n\n" +
                        "Focus Score: $focusScore/100\n" +
                        "+50 points earned 🪙"
            )
            .setPositiveButton("Done") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}