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
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val goalRepository = GoalRepository()
    private val sessionRepository = SessionRepository()
    private val userRepository = UserRepository()
    private val screenTimeMonitor = ScreenTimeMonitor(application)
    private val fatigueCalculator = FatigueCalculator(application)

    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> = _goals

    private val _focusEnergy = MutableLiveData(100) // Default to 100
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

    private val _fatigue = MutableLiveData(0) // Default to 0
    val fatigue: LiveData<Int> = _fatigue

    private val _fatigueRecommendations = MutableLiveData<List<String>>()
    val fatigueRecommendations: LiveData<List<String>> = _fatigueRecommendations

    init {
        // Set default values immediately
        setDefaultValues()

        // Then load actual data
        calculateGreeting()
        loadUserData()
        loadTodayStats()
        loadScreenTime()
    }

    private fun setDefaultValues() {
        _todayHours.value = Pair(0.0, 8.0)
        _goals.value = emptyList()
        _recentSessions.value = emptyList()
        _fatigueRecommendations.value = listOf(
            "You're in peak condition! Great time for difficult topics.",
            "Your focus is excellent. Make the most of it!"
        )
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
            try {
                val userId = userRepository.getCurrentUserId()

                if (userId == null) {
                    android.util.Log.w("HomeViewModel", "User ID is null")
                    return@launch
                }

                android.util.Log.d("HomeViewModel", "Loading data for user: $userId")

                // Load goals
                goalRepository.getUserGoals(userId).onSuccess { goals ->
                    android.util.Log.d("HomeViewModel", "Loaded ${goals.size} goals")
                    _goals.value = goals
                }.onFailure { error ->
                    android.util.Log.e("HomeViewModel", "Error loading goals", error)
                    _goals.value = emptyList()
                }

                // Load user profile
                userRepository.getUser(userId).onSuccess { user ->
                    android.util.Log.d("HomeViewModel", "Loaded user profile: streak=${user.streak}, points=${user.points}")
                    _streak.value = user.streak
                    _points.value = user.points
                }.onFailure { error ->
                    android.util.Log.e("HomeViewModel", "Error loading user", error)
                }

                // Load sessions
                sessionRepository.getUserSessions(userId).onSuccess { sessions ->
                    android.util.Log.d("HomeViewModel", "Loaded ${sessions.size} sessions")

                    val recentSessions = sessions.take(5)
                    _recentSessions.value = recentSessions

                    // Calculate fatigue
                    val fatigueScore = fatigueCalculator.calculateFatigue(sessions)
                    _fatigue.value = fatigueScore
                    _fatigueRecommendations.value = fatigueCalculator.getRecommendations(fatigueScore)

                    // Calculate focus energy
                    calculateFocusEnergy(sessions)
                }.onFailure { error ->
                    android.util.Log.e("HomeViewModel", "Error loading sessions", error)
                    _recentSessions.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Exception in loadUserData", e)
            }
        }
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()

                if (userId == null) {
                    android.util.Log.w("HomeViewModel", "User ID is null in loadTodayStats")
                    return@launch
                }

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
                    android.util.Log.d("HomeViewModel", "Today's hours: $totalHours")
                    _todayHours.value = Pair(totalHours, 8.0)
                }.onFailure { error ->
                    android.util.Log.e("HomeViewModel", "Error loading today's stats", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Exception in loadTodayStats", e)
            }
        }
    }

    private fun loadScreenTime() {
        viewModelScope.launch {
            try {
                val screenTime = screenTimeMonitor.getTodayScreenTime()
                android.util.Log.d("HomeViewModel", "Loaded ${screenTime.size} app usage items")
                _screenTimeData.value = screenTime
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading screen time", e)
                _screenTimeData.value = emptyList()
            }
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
        android.util.Log.d("HomeViewModel", "Refreshing all data")
        setDefaultValues()
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