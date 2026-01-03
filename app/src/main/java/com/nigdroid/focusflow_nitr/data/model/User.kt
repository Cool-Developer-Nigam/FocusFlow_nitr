package com.nigdroid.focusflow_nitr.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "", // Google profile photo URL or custom avatar
    val streak: Int = 0,
    val points: Int = 0,
    val level: Int = 1,
    val totalFocusHours: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val lastActive: Timestamp = Timestamp.now()
)