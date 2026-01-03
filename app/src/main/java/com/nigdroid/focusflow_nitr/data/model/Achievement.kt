package com.nigdroid.focusflow_nitr.data.model

data class Achievement(
    val achievementId: String = "",
    val name: String = "",
    val description: String = "",
    val iconName: String = "",
    val points: Int = 0,
    val requirement: Int = 0,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val unlockedAt: Long = 0
)