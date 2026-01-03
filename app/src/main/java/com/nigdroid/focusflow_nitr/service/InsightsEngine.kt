package com.nigdroid.focusflow_nitr.service

import android.content.Context
import com.nigdroid.focusflow_nitr.data.model.Goal
import com.nigdroid.focusflow_nitr.data.model.Session
import java.util.Calendar
import kotlin.compareTo

class InsightsEngine(private val context: Context) {

    /**
     * Generate comprehensive insights
     */
    fun generateInsights(
        sessions: List<Session>,
        goals: List<Goal>
    ): InsightsData {
        val weekSessions = getThisWeekSessions(sessions)
        val todaySessions = getTodaySessions(sessions)

        return InsightsData(
            focusScore = calculateOverallFocusScore(sessions),
            productiveHours = calculateProductiveHours(todaySessions),
            peakTime = findPeakTime(sessions),
            distractionCount = countDistractions(todaySessions),
            goalProgress = calculateGoalProgress(goals),
            weeklyComparison = calculateWeeklyComparison(sessions),
            predictions = generatePredictions(sessions, goals),
            recommendations = generateRecommendations(sessions, goals)
        )
    }

    private fun calculateOverallFocusScore(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0
        return sessions.takeLast(10).map { it.focusScore }.average().toInt()
    }

    private fun calculateProductiveHours(sessions: List<Session>): Pair<Double, Double> {
        val total = sessions.sumOf { it.duration } / 3600000.0
        val target = 8.0
        return Pair(total, target)
    }

    private fun findPeakTime(sessions: List<Session>): String {
        val timeSlots = sessions.groupBy { session ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = session.startTime.toDate().time
            }
            cal.get(Calendar.HOUR_OF_DAY)
        }

        val peakHour = timeSlots.maxByOrNull { (_, sessions) ->
            sessions.map { it.focusScore }.average()
        }?.key ?: 9

        return when (peakHour) {
            in 5..8 -> "Early Morning (${peakHour}:00 AM)"
            in 9..11 -> "Mid Morning (${peakHour}:00 AM)"
            in 12..14 -> "Afternoon (${peakHour % 12}:00 PM)"
            in 15..17 -> "Evening (${peakHour % 12}:00 PM)"
            else -> "Night (${peakHour % 12}:00 PM)"
        }
    }

    private fun countDistractions(sessions: List<Session>): Int {
        return sessions.sumOf { session ->
            session.appsUsed.count { it.category == com.nigdroid.focusflow_nitr.data.model.AppCategory.DISTRACTING }
        }
    }

    private fun calculateGoalProgress(goals: List<Goal>): List<GoalProgressData> {
        return goals.map { goal ->
            GoalProgressData(
                goalName = goal.title, // Fixed: Changed from goal.name to goal.title
                progress = goal.progress,
                target = goal.targetHours.toDouble(), // Fixed: Convert Int to Double
                percentage = ((goal.progress / goal.targetHours) * 100).toInt()
            )
        }
    }

    private fun calculateWeeklyComparison(sessions: List<Session>): WeeklyComparison {
        val thisWeek = getThisWeekSessions(sessions)
        val lastWeek = getLastWeekSessions(sessions)

        val thisWeekHours = thisWeek.sumOf { it.duration } / 3600000.0
        val lastWeekHours = lastWeek.sumOf { it.duration } / 3600000.0

        val change = if (lastWeekHours > 0) {
            ((thisWeekHours - lastWeekHours) / lastWeekHours * 100).toInt()
        } else 0

        return WeeklyComparison(
            thisWeek = thisWeekHours,
            lastWeek = lastWeekHours,
            changePercentage = change
        )
    }

    private fun generatePredictions(sessions: List<Session>, goals: List<Goal>): List<String> {
        val predictions = mutableListOf<String>()

        // Goal completion prediction
        goals.forEach { goal ->
            val daysRemaining = 7 // Simplified
            val dailyRequired = (goal.targetHours - goal.progress) / daysRemaining
            val avgDaily = sessions.takeLast(7).sumOf { it.duration } / 3600000.0 / 7

            if (avgDaily >= dailyRequired) {
                predictions.add("You're on track to complete '${goal.title}' on time! 🎯") // Fixed: Changed from goal.name
            } else {
                predictions.add("Risk: Need ${String.format("%.1f", dailyRequired)}h/day for '${goal.title}'") // Fixed: Changed from goal.name
            }
        }

        // Burnout prediction
        val recentSessions = sessions.takeLast(7)
        val avgDailyHours = recentSessions.sumOf { it.duration } / 3600000.0 / 7
        if (avgDailyHours > 10) {
            predictions.add("⚠️ High burnout risk detected. Consider taking rest days.")
        }

        return predictions
    }

    private fun generateRecommendations(sessions: List<Session>, goals: List<Goal>): List<String> {
        val recommendations = mutableListOf<String>()

        // Based on peak times
        val optimalTimes = FatigueCalculator(context).predictOptimalTimes(sessions)
        if (optimalTimes.isNotEmpty()) {
            val bestTime = optimalTimes.first()
            recommendations.add("Your peak focus time is ${bestTime.getTimeRange()}. Schedule important tasks then!")
        }

        // Based on fatigue
        val fatigueScore = FatigueCalculator(context).calculateFatigue(sessions)
        if (fatigueScore > 60) {
            recommendations.add("Take a longer break. Your fatigue level is high.")
        }

        // Based on consistency
        val last7Days = sessions.takeLast(7).groupBy {
            Calendar.getInstance().apply {
                timeInMillis = it.startTime.toDate().time
            }.get(Calendar.DAY_OF_YEAR)
        }
        if (last7Days.size < 5) {
            recommendations.add("Try to study at least 5 days per week for better consistency.")
        }

        return recommendations
    }

    private fun getThisWeekSessions(sessions: List<Session>): List<Session> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val weekStart = cal.timeInMillis

        return sessions.filter { it.startTime.toDate().time >= weekStart }
    }

    private fun getLastWeekSessions(sessions: List<Session>): List<Session> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val lastWeekStart = cal.timeInMillis

        cal.add(Calendar.WEEK_OF_YEAR, 1)
        val lastWeekEnd = cal.timeInMillis

        return sessions.filter {
            val time = it.startTime.toDate().time
            time >= lastWeekStart && time < lastWeekEnd
        }
    }

    private fun getTodaySessions(sessions: List<Session>): List<Session> {
        val today = Calendar.getInstance()
        return sessions.filter { session ->
            val sessionCal = Calendar.getInstance().apply {
                timeInMillis = session.startTime.toDate().time
            }
            sessionCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    sessionCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }
    }
}

data class InsightsData(
    val focusScore: Int,
    val productiveHours: Pair<Double, Double>,
    val peakTime: String,
    val distractionCount: Int,
    val goalProgress: List<GoalProgressData>,
    val weeklyComparison: WeeklyComparison,
    val predictions: List<String>,
    val recommendations: List<String>
)

data class GoalProgressData(
    val goalName: String,
    val progress: Double,
    val target: Double,
    val percentage: Int
)

data class WeeklyComparison(
    val thisWeek: Double,
    val lastWeek: Double,
    val changePercentage: Int
)