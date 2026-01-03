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
import kotlin.compareTo

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
                setDefaultData()
                _loading.value = false
                return@launch
            }

            try {
                val sessionsResult = sessionRepository.getUserSessions(userId)
                val goalsResult = goalRepository.getUserGoals(userId)

                if (sessionsResult.isSuccess && goalsResult.isSuccess) {
                    val allSessions = sessionsResult.getOrNull() ?: emptyList()
                    val goals = goalsResult.getOrNull() ?: emptyList()

                    val filteredSessions = filterSessionsByPeriod(allSessions, period)

                    if (filteredSessions.isEmpty()) {
                        setDefaultData()
                    } else {
                        _insights.value = insightsEngine.generateInsights(filteredSessions, goals)
                        _chartData.value = calculateChartData(filteredSessions, period)
                    }
                } else {
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

        val daysToShow = when (period) {
            InsightsPeriod.TODAY -> 24
            InsightsPeriod.WEEK -> 7
            InsightsPeriod.MONTH -> 30
        }

        if (period == InsightsPeriod.TODAY) {
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

                dailyData.add(
                    DailyData(
                        dayName = String.format("%02d:00", hour),
                        hours = hourSessions.sumOf { it.duration / 3600000.0 },
                        focusScore = if (hourSessions.isNotEmpty()) {
                            hourSessions.map { it.focusScore }.average().toInt()
                        } else 0
                    )
                )
            }
        } else {
            for (i in (daysToShow - 1) downTo 0) {
                calendar.add(Calendar.DAY_OF_YEAR, if (i == daysToShow - 1) -(daysToShow - 1) else 1)
                val dayStart = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                val dayEnd = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val daySessions = sessions.filter {
                    val time = it.startTime.toDate().time
                    time >= dayStart && time <= dayEnd
                }

                dailyData.add(
                    DailyData(
                        dayName = if (period == InsightsPeriod.WEEK) {
                            getDayName(calendar)
                        } else {
                            "${calendar.get(Calendar.DAY_OF_MONTH)}"
                        },
                        hours = daySessions.sumOf { it.duration / 3600000.0 },
                        focusScore = if (daySessions.isNotEmpty()) {
                            daySessions.map { it.focusScore }.average().toInt()
                        } else 0
                    )
                )
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

        _chartData.value = emptyList()
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