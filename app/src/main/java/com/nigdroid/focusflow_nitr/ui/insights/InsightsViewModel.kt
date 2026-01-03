package com.nigdroid.focusflow_nitr.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nigdroid.focusflow_nitr.data.repository.GoalRepository
import com.nigdroid.focusflow_nitr.data.repository.SessionRepository
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import com.nigdroid.focusflow_nitr.service.InsightsData
import com.nigdroid.focusflow_nitr.service.InsightsEngine
import kotlinx.coroutines.launch
import java.util.Calendar

enum class InsightsPeriod {
    TODAY, WEEK, MONTH
}

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepository = SessionRepository()
    private val goalRepository = GoalRepository()
    private val userRepository = UserRepository()
    private val insightsEngine = InsightsEngine(application)

    private val _insights = MutableLiveData<InsightsData>()
    val insights: LiveData<InsightsData> = _insights

    private val _chartData = MutableLiveData<List<DailyData>>()
    val chartData: LiveData<List<DailyData>> = _chartData

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var currentPeriod = InsightsPeriod.WEEK

    init {
        loadInsightsForPeriod(InsightsPeriod.WEEK)
    }

    fun loadInsightsForPeriod(period: InsightsPeriod) {
        currentPeriod = period
        viewModelScope.launch {
            _loading.value = true
            val userId = userRepository.getCurrentUserId()

            if (userId == null) {
                android.util.Log.e("InsightsViewModel", "User ID is null")
                setDefaultData()
                _loading.value = false
                return@launch
            }

            try {
                android.util.Log.d("InsightsViewModel", "Loading insights for period: $period, userId: $userId")

                val sessionsResult = sessionRepository.getUserSessions(userId)
                val goalsResult = goalRepository.getUserGoals(userId)

                if (sessionsResult.isSuccess && goalsResult.isSuccess) {
                    val allSessions = sessionsResult.getOrNull() ?: emptyList()
                    val goals = goalsResult.getOrNull() ?: emptyList()

                    android.util.Log.d("InsightsViewModel", "Loaded ${allSessions.size} sessions, ${goals.size} goals")

                    val filteredSessions = filterSessionsByPeriod(allSessions, period)
                    android.util.Log.d("InsightsViewModel", "Filtered to ${filteredSessions.size} sessions for period")

                    if (filteredSessions.isEmpty()) {
                        android.util.Log.d("InsightsViewModel", "No sessions found, showing default data")
                        setDefaultData()
                    } else {
                        val insightsData = insightsEngine.generateInsights(filteredSessions, goals)
                        _insights.value = insightsData

                        val chartData = calculateChartData(filteredSessions, period)
                        _chartData.value = chartData

                        android.util.Log.d("InsightsViewModel", "Insights generated successfully with ${chartData.size} data points")
                    }
                } else {
                    android.util.Log.e("InsightsViewModel", "Failed to load sessions or goals")
                    setDefaultData()
                }
            } catch (e: Exception) {
                android.util.Log.e("InsightsViewModel", "Error loading insights", e)
                setDefaultData()
            } finally {
                _loading.value = false
            }
        }
    }

    private fun filterSessionsByPeriod(
        sessions: List<com.nigdroid.focusflow_nitr.data.model.Session>,
        period: InsightsPeriod
    ): List<com.nigdroid.focusflow_nitr.data.model.Session> {
        val calendar = Calendar.getInstance()
        val cutoffTime = when (period) {
            InsightsPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            InsightsPeriod.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            InsightsPeriod.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
        }

        return sessions.filter { it.startTime.toDate().time >= cutoffTime }
    }

    private fun calculateChartData(
        sessions: List<com.nigdroid.focusflow_nitr.data.model.Session>,
        period: InsightsPeriod
    ): List<DailyData> {
        val calendar = Calendar.getInstance()
        val dailyData = mutableListOf<DailyData>()

        when (period) {
            InsightsPeriod.TODAY -> {
                // Hourly data for today
                for (hour in 0..23) {
                    val hourStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis

                    val hourEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis

                    val hourSessions = sessions.filter {
                        val time = it.startTime.toDate().time
                        time >= hourStart && time <= hourEnd
                    }

                    val totalHours = hourSessions.sumOf { it.duration / 3600000.0 }
                    val avgFocusScore = if (hourSessions.isNotEmpty()) {
                        hourSessions.map { it.focusScore }.average().toInt()
                    } else 0

                    dailyData.add(
                        DailyData(
                            dayName = String.format("%02d:00", hour),
                            hours = totalHours,
                            focusScore = avgFocusScore
                        )
                    )
                }
            }
            InsightsPeriod.WEEK -> {
                // Daily data for last 7 days
                val today = Calendar.getInstance()
                for (i in 6 downTo 0) {
                    val dayCalendar = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                    }

                    val dayStart = dayCalendar.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis

                    val dayEnd = dayCalendar.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis

                    val daySessions = sessions.filter {
                        val time = it.startTime.toDate().time
                        time >= dayStart && time <= dayEnd
                    }

                    val totalHours = daySessions.sumOf { it.duration / 3600000.0 }
                    val avgFocusScore = if (daySessions.isNotEmpty()) {
                        daySessions.map { it.focusScore }.average().toInt()
                    } else 0

                    dailyData.add(
                        DailyData(
                            dayName = getDayName(dayCalendar),
                            hours = totalHours,
                            focusScore = avgFocusScore
                        )
                    )
                }
            }
            InsightsPeriod.MONTH -> {
                // Weekly data for last 4 weeks
                for (weekOffset in 3 downTo 0) {
                    val weekStart = Calendar.getInstance().apply {
                        add(Calendar.WEEK_OF_YEAR, -weekOffset)
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis

                    val weekEnd = Calendar.getInstance().apply {
                        add(Calendar.WEEK_OF_YEAR, -weekOffset)
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        add(Calendar.DAY_OF_YEAR, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis

                    val weekSessions = sessions.filter {
                        val time = it.startTime.toDate().time
                        time >= weekStart && time <= weekEnd
                    }

                    val totalHours = weekSessions.sumOf { it.duration / 3600000.0 }
                    val avgFocusScore = if (weekSessions.isNotEmpty()) {
                        weekSessions.map { it.focusScore }.average().toInt()
                    } else 0

                    dailyData.add(
                        DailyData(
                            dayName = "Week ${4 - weekOffset}",
                            hours = totalHours,
                            focusScore = avgFocusScore
                        )
                    )
                }
            }
        }

        return dailyData
    }

    private fun setDefaultData() {
        _insights.value = InsightsData(
            focusScore = 0,
            productiveHours = Pair(0.0, 8.0),
            peakTime = "Not enough data",
            distractionCount = 0,
            goalProgress = emptyList(),
            weeklyComparison = com.nigdroid.focusflow_nitr.service.WeeklyComparison(0.0, 0.0, 0),
            predictions = listOf(
                "Complete focus sessions to unlock predictions",
                "Track your progress for 7 days to see patterns",
                "Set goals to get personalized insights"
            ),
            recommendations = listOf(
                "Start a focus session to begin tracking",
                "Set daily goals for better productivity",
                "Track at least 5 sessions for insights"
            )
        )

        // Generate empty chart data based on current period
        val emptyChartData = when (currentPeriod) {
            InsightsPeriod.TODAY -> (0..23).map { hour ->
                DailyData(String.format("%02d:00", hour), 0.0, 0)
            }
            InsightsPeriod.WEEK -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map { day ->
                DailyData(day, 0.0, 0)
            }
            InsightsPeriod.MONTH -> (1..4).map { week ->
                DailyData("Week $week", 0.0, 0)
            }
        }

        _chartData.value = emptyChartData
    }

    private fun getDayName(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> ""
        }
    }
}

data class DailyData(
    val dayName: String,
    val hours: Double,
    val focusScore: Int
)