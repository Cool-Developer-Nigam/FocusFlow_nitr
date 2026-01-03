package com.nigdroid.focusflow_nitr.utils

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

object PermissionHelper {

    /**
     * Check if Usage Stats permission is granted
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Check if Accessibility Service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = ComponentName(context, com.nigdroid.focusflow_nitr.service.TypingMonitor::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        return if (enabledServices.isNullOrEmpty()) {
            false
        } else {
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            while (colonSplitter.hasNext()) {
                val componentString = colonSplitter.next()
                val enabledService = ComponentName.unflattenFromString(componentString)
                if (enabledService != null && enabledService == service) {
                    return true
                }
            }
            false
        }
    }

    /**
     * Open Usage Stats settings
     */
    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * Open Accessibility settings - DIRECTLY to our service
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            // Try to open directly to our service settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Add extra to highlight our service
            val serviceName = ComponentName(
                context,
                com.nigdroid.focusflow_nitr.service.TypingMonitor::class.java
            ).flattenToString()

            intent.putExtra(":settings:fragment_args_key", serviceName)
            intent.putExtra(":settings:show_fragment_args", android.os.Bundle().apply {
                putString("package", context.packageName)
            })

            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general accessibility settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }
    }

    /**
     * Show dialog to request Usage Stats permission
     */
    fun showUsageStatsPermissionDialog(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Screen Time Permission Required")
            .setMessage(
                "FocusFlow needs access to usage statistics to track your screen time and app usage.\n\n" +
                        "This helps you:\n" +
                        "• Monitor productive vs distracting apps\n" +
                        "• Track daily focus hours\n" +
                        "• Get personalized insights\n\n" +
                        "Your data stays private and is only used to help you focus better."
            )
            .setPositiveButton("Grant Permission") { _, _ ->
                openUsageStatsSettings(activity)
            }
            .setNegativeButton("Not Now", null)
            .setCancelable(false)
            .show()
    }

    /**
     * Show dialog to request Accessibility Service
     */
    fun showAccessibilityPermissionDialog(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Typing Monitor Permission")
            .setMessage(
                "FocusFlow needs accessibility access to monitor your typing patterns.\n\n" +
                        "This helps you:\n" +
                        "• Track typing speed over time\n" +
                        "• Identify when you're getting fatigued\n" +
                        "• Improve productivity insights\n\n" +
                        "We only track statistics - not what you type. Your privacy is protected.\n\n" +
                        "In the next screen:\n" +
                        "1. Find 'FocusFlow' or 'TypingMonitor'\n" +
                        "2. Tap it to open settings\n" +
                        "3. Toggle the switch to ON"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAccessibilitySettings(activity)
            }
            .setNegativeButton("Not Now", null)
            .setCancelable(false)
            .show()
    }

    /**
     * Check all required permissions and show dialogs if needed
     */
    fun checkAndRequestAllPermissions(activity: AppCompatActivity) {
        // Check Usage Stats first
        if (!hasUsageStatsPermission(activity)) {
            showUsageStatsPermissionDialog(activity)
            return
        }

        // Then check Accessibility
        if (!isAccessibilityServiceEnabled(activity)) {
            showAccessibilityPermissionDialog(activity)
            return
        }
    }

    /**
     * Get permission status message
     */
    fun getPermissionStatus(context: Context): String {
        val usageStats = hasUsageStatsPermission(context)
        val accessibility = isAccessibilityServiceEnabled(context)

        return when {
            usageStats && accessibility -> "✅ All permissions granted"
            usageStats -> "⚠️ Accessibility service not enabled"
            accessibility -> "⚠️ Usage stats not granted"
            else -> "❌ Permissions required"
        }
    }
}
