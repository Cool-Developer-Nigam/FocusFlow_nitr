package com.nigdroid.focusflow_nitr.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.nigdroid.focusflow_nitr.data.model.AppCategory
import com.nigdroid.focusflow_nitr.data.model.AppUsage
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ScreenTimeMonitor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    /**
     * Get screen time data for today
     */
    fun getTodayScreenTime(): List<AppUsage> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return getScreenTime(startTime, endTime)
    }

    /**
     * Get screen time for a specific date range
     */
    fun getScreenTime(startTime: Long, endTime: Long): List<AppUsage> {
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return usageStatsList
            ?.filter { it.totalTimeInForeground > 0 }
            ?.map { stats ->
                AppUsage(
                    appName = getAppName(stats.packageName),
                    packageName = stats.packageName,
                    timeSpent = stats.totalTimeInForeground,
                    category = categorizeApp(stats.packageName)
                )
            }
            ?.sortedByDescending { it.timeSpent }
            ?: emptyList()
    }

    /**
     * Get total screen time in milliseconds
     */
    fun getTotalScreenTime(startTime: Long, endTime: Long): Long {
        return getScreenTime(startTime, endTime).sumOf { it.timeSpent }
    }

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Categorize apps as Productive, Neutral, or Distracting
     */
    private fun categorizeApp(packageName: String): AppCategory {
        return when {
            isProductiveApp(packageName) -> AppCategory.PRODUCTIVE
            isDistractingApp(packageName) -> AppCategory.DISTRACTING
            else -> AppCategory.NEUTRAL
        }
    }

    /**
     * Check if app is productive
     */
    private fun isProductiveApp(packageName: String): Boolean {
        val productiveKeywords = listOf(
            "study", "learn", "education", "book", "note", "document",
            "pdf", "calculator", "dictionary", "translate", "khan",
            "duolingo", "coursera", "udemy", "calendar", "notion"
        )
        return productiveKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    /**
     * Check if app is distracting
     */
    private fun isDistractingApp(packageName: String): Boolean {
        val distractingKeywords = listOf(
            "facebook", "instagram", "twitter", "tiktok", "snapchat",
            "youtube", "netflix", "game", "reddit", "whatsapp"
        )
        return distractingKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    /**
     * Get productive time today
     */
    fun getProductiveTime(startTime: Long, endTime: Long): Long {
        return getScreenTime(startTime, endTime)
            .filter { it.category == AppCategory.PRODUCTIVE }
            .sumOf { it.timeSpent }
    }

    /**
     * Get distracting time today
     */
    fun getDistractingTime(startTime: Long, endTime: Long): Long {
        return getScreenTime(startTime, endTime)
            .filter { it.category == AppCategory.DISTRACTING }
            .sumOf { it.timeSpent }
    }

    /**
     * Format time in hours and minutes
     */
    fun formatTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
