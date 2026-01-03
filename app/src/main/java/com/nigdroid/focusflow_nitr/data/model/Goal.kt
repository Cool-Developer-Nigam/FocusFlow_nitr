package com.nigdroid.focusflow_nitr.data.model

import com.google.firebase.Timestamp

data class Goal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val targetHours: Int = 0,
    val currentProgress: Long = 0, // in milliseconds
    val deadline: Long = 0,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val completedAt: Long? = null,
    val category: String = "",
    val productiveApps: List<String> = emptyList()
) {
    val progress: Double
        get() = (currentProgress / 3600000.0) // Convert to hours

    val progressPercentage: Int
        get() = ((progress / targetHours) * 100).toInt().coerceIn(0, 100)
}