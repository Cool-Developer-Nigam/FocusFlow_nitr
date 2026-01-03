package com.nigdroid.focusflow_nitr.data.model

import com.google.firebase.Timestamp

data class Session(
    val sessionId: String = "",
    val userId: String = "",
    val goalId: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val duration: Long = 0,
    val focusScore: Int = 0,
    val sessionType: SessionType = SessionType.STUDY,
    val appsUsed: List<AppUsage> = emptyList()
)

enum class SessionType {
    STUDY, POMODORO, BREAK
}

data class AppUsage(
    val appName: String = "",
    val packageName: String = "",
    val timeSpent: Long = 0,
    val category: AppCategory = AppCategory.NEUTRAL
)

enum class AppCategory {
    PRODUCTIVE, NEUTRAL, DISTRACTING
}
