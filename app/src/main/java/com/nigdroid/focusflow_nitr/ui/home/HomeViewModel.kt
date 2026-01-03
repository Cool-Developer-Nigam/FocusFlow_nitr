package com.nigdroid.focusflow_nitr.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nigdroid.focusflow_nitr.data.model.Goal
import com.nigdroid.focusflow_nitr.data.model.Session
import com.nigdroid.focusflow_nitr.data.repository.GoalRepository
import com.nigdroid.focusflow_nitr.data.repository.SessionRepository
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import com.nigdroid.focusflow_nitr.service.FatigueCalculator
import com.nigdroid.focusflow_nitr.service.ScreenTimeMonitor
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val goalRepository = GoalRepository()
    private val sessionRepository = SessionRepository()
    private val userRepository = UserRepository()
    private val screenTimeMonitor = ScreenTimeMonitor(application)
    private val fatigueCalculator = FatigueCalculator(application)

    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> = _goals

    private val _focusEnergy = MutableLiveData(0)
    val focusEnergy: LiveData<Int> = _focusEnergy

    private val _greeting = MutableLiveData<String>()
    val greeting: LiveData<String> = _greeting

    private val _todayHours = MutableLiveData<Pair<Double, Double>>()
    val todayHours: LiveData<Pair<Double, Double>> = _todayHours

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak

    private val _points = MutableLiveData(0)
    val points: LiveData<Int> = _points

    private val _recentSessions = MutableLiveData<List<Session>>()
    val recentSessions: LiveData<List<Session>> = _recentSessions

    private val _screenTimeData = MutableLiveData<List<com.nigdroid.focusflow_nitr.data.model.AppUsage>>()
    val screenTimeData: LiveData<List<com.nigdroid.focusflow_nitr.data.model.AppUsage>> = _screenTimeData

    private val _fatigue = MutableLiveData<Int>()
    val fatigue: LiveData<Int> = _fatigue

    private val _fatigueRecommendations = MutableLiveData<List<String>>()
    val fatigueRecommendations: LiveData<List<String>> = _fatigueRecommendations

    init {
        loadUserData()
        calculateGreeting()
        loadTodayStats()
        loadScreenTime()
    }

    private fun calculateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _greeting.value = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch

            // Load goals
            goalRepository.getUserGoals(userId).onSuccess { goals ->
                _goals.value = goals
            }

            // Load user profile
            userRepository.getUser(userId).onSuccess { user ->
                _streak.value = user.streak
                _points.value = user.points
            }

            // Load sessions
            sessionRepository.getUserSessions(userId).onSuccess { sessions ->
                _recentSessions.value = sessions.take(5)

                // Calculate fatigue
                val fatigueScore = fatigueCalculator.calculateFatigue(sessions)
                _fatigue.value = fatigueScore
                _fatigueRecommendations.value = fatigueCalculator.getRecommendations(fatigueScore)

                // Calculate focus energy
                calculateFocusEnergy(sessions)
            }
        }
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch

            sessionRepository.getUserSessions(userId).onSuccess { sessions ->
                val today = Calendar.getInstance()
                val todaySessions = sessions.filter { session ->
                    val sessionCal = Calendar.getInstance().apply {
                        timeInMillis = session.startTime.toDate().time
                    }
                    sessionCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                            sessionCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                }

                val totalHours = todaySessions.sumOf { it.duration / 3600000.0 }
                _todayHours.value = Pair(totalHours, 8.0)
            }
        }
    }

    private fun loadScreenTime() {
        viewModelScope.launch {
            val screenTime = screenTimeMonitor.getTodayScreenTime()
            _screenTimeData.value = screenTime
        }
    }

    private fun calculateFocusEnergy(sessions: List<Session>) {
        val today = Calendar.getInstance()
        val todaySessions = sessions.filter { session ->
            val sessionCal = Calendar.getInstance().apply {
                timeInMillis = session.startTime.toDate().time
            }
            sessionCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    sessionCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }

        if (todaySessions.isEmpty()) {
            _focusEnergy.value = 100
            return
        }

        // Calculate based on time, quality, and fatigue
        val totalHours = todaySessions.sumOf { it.duration / 3600000.0 }
        val avgFocusScore = todaySessions.map { it.focusScore }.average()
        val fatigueScore = _fatigue.value ?: 0

        val energy = ((avgFocusScore * 0.6) + ((8 - totalHours.coerceAtMost(8.0)) / 8 * 40) - (fatigueScore * 0.2))
            .toInt()
            .coerceIn(0, 100)

        _focusEnergy.value = energy
    }

    fun refreshData() {
        loadUserData()
        loadTodayStats()
        loadScreenTime()
    }

    fun getTodayProductiveTime(): String {
        val productive = _screenTimeData.value
            ?.filter { it.category == com.nigdroid.focusflow_nitr.data.model.AppCategory.PRODUCTIVE }
            ?.sumOf { it.timeSpent } ?: 0L
        return screenTimeMonitor.formatTime(productive)
    }

    fun getTodayDistractingTime(): String {
        val distracting = _screenTimeData.value
            ?.filter { it.category == com.nigdroid.focusflow_nitr.data.model.AppCategory.DISTRACTING }
            ?.sumOf { it.timeSpent } ?: 0L
        return screenTimeMonitor.formatTime(distracting)
    }
}
