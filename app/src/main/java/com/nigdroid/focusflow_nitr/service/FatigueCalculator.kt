package com.nigdroid.focusflow_nitr.service

import android.content.Context
import com.nigdroid.focusflow_nitr.data.model.Session
import java.util.Calendar

class FatigueCalculator(private val context: Context) {


    fun calculateFatigue(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0

        val today = Calendar.getInstance()
        val todaySessions = sessions.filter { session ->
            val sessionCal = Calendar.getInstance().apply {
                timeInMillis = session.startTime.toDate().time
            }
            sessionCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    sessionCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }

        var fatigueScore = 0

        // Factor 1: Total study time today (40%)
        val totalMinutes = todaySessions.sumOf { it.duration } / 60000
        val timeScore = when {
            totalMinutes < 120 -> 0 // < 2 hours
            totalMinutes < 240 -> 15 // 2-4 hours
            totalMinutes < 360 -> 25 // 4-6 hours
            totalMinutes < 480 -> 35 // 6-8 hours
            else -> 40 // > 8 hours
        }
        fatigueScore += timeScore

        // Factor 2: Continuous study without breaks (30%)
        val longestSession = todaySessions.maxOfOrNull { it.duration / 60000 } ?: 0
        val breakScore = when {
            longestSession < 30 -> 0
            longestSession < 60 -> 10
            longestSession < 90 -> 20
            longestSession < 120 -> 25
            else -> 30
        }
        fatigueScore += breakScore

        // Factor 3: Time of day (20%)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDayScore = when (currentHour) {
            in 0..5 -> 20 // Very late/early
            in 6..9 -> 5 // Morning (fresh)
            in 10..15 -> 10 // Afternoon
            in 16..20 -> 12 // Evening
            else -> 18 // Night
        }
        fatigueScore += timeOfDayScore

        // Factor 4: Typing errors (if available) (10%)
        val avgFocusScore = todaySessions.map { it.focusScore }.average()
        val focusScore = when {
            avgFocusScore > 80 -> 0
            avgFocusScore > 60 -> 5
            else -> 10
        }
        fatigueScore += focusScore

        return fatigueScore.coerceIn(0, 100)
    }

    /**
     * Get fatigue level description
     */
    fun getFatigueLevel(fatigueScore: Int): FatigueLevel {
        return when (fatigueScore) {
            in 0..30 -> FatigueLevel.LOW
            in 31..60 -> FatigueLevel.MODERATE
            in 61..80 -> FatigueLevel.HIGH
            else -> FatigueLevel.CRITICAL
        }
    }

    /**
     * Get fatigue color
     */
    fun getFatigueColor(fatigueScore: Int): String {
        return when (getFatigueLevel(fatigueScore)) {
            FatigueLevel.LOW -> "#4CAF50" // Green
            FatigueLevel.MODERATE -> "#FFC107" // Yellow
            FatigueLevel.HIGH -> "#FF9800" // Orange
            FatigueLevel.CRITICAL -> "#F44336" // Red
        }
    }

    /**
     * Get recommendations based on fatigue
     */
    fun getRecommendations(fatigueScore: Int): List<String> {
        return when (getFatigueLevel(fatigueScore)) {
            FatigueLevel.LOW -> listOf(
                "You're in peak condition! Great time for difficult topics.",
                "Your focus is excellent. Make the most of it!",
                "Consider tackling challenging problems now."
            )
            FatigueLevel.MODERATE -> listOf(
                "Take a 10-minute break soon.",
                "Hydrate and stretch to maintain focus.",
                "Switch to lighter tasks if feeling tired."
            )
            FatigueLevel.HIGH -> listOf(
                "Take a 20-minute break NOW.",
                "Your productivity is decreasing.",
                "Consider ending study session soon.",
                "Get some fresh air or light exercise."
            )
            FatigueLevel.CRITICAL -> listOf(
                "STOP STUDYING! You need rest.",
                "Continuing will be counterproductive.",
                "Get proper sleep for better retention.",
                "Your brain needs recovery time."
            )
        }
    }

    /**
     * Predict optimal study times for user
     */
    fun predictOptimalTimes(sessions: List<Session>): List<OptimalTimeSlot> {
        val timeSlots = mutableMapOf<Int, MutableList<Int>>() // hour -> focus scores

        sessions.forEach { session ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = session.startTime.toDate().time
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            timeSlots.getOrPut(hour) { mutableListOf() }.add(session.focusScore)
        }

        return timeSlots.map { (hour, scores) ->
            OptimalTimeSlot(
                hour = hour,
                avgFocusScore = scores.average().toInt(),
                sessionCount = scores.size
            )
        }.sortedByDescending { it.avgFocusScore }
    }
}

enum class FatigueLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

data class OptimalTimeSlot(
    val hour: Int,
    val avgFocusScore: Int,
    val sessionCount: Int
) {
    fun getTimeRange(): String {
        val endHour = (hour + 1) % 24
        return "${hour}:00 - ${endHour}:00"
    }
}