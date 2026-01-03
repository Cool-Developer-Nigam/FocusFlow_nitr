package com.nigdroid.focusflow_nitr.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("FocusFlowPrefs", Context.MODE_PRIVATE)

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("IS_FIRST_LAUNCH", true)
        set(value) = prefs.edit().putBoolean("IS_FIRST_LAUNCH", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("IS_LOGGED_IN", false)
        set(value) = prefs.edit().putBoolean("IS_LOGGED_IN", value).apply()

    var userId: String?
        get() = prefs.getString("USER_ID", null)
        set(value) = prefs.edit().putString("USER_ID", value).apply()

    var dailyGoalHours: Int
        get() = prefs.getInt("DAILY_GOAL_HOURS", 2) // Default to 2 hours
        set(value) = prefs.edit().putInt("DAILY_GOAL_HOURS", value).apply()

    var hasCreatedGoal: Boolean
        get() = prefs.getBoolean("HAS_CREATED_GOAL", false)
        set(value) = prefs.edit().putBoolean("HAS_CREATED_GOAL", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}