package com.nigdroid.focusflow_nitr.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class TypingMonitor : AccessibilityService() {

    companion object {
        private const val TAG = "TypingMonitor"
        private var instance: TypingMonitor? = null

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        Log.d(TAG, "Service Connected!")

        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }

        this.serviceInfo = info

        Log.d(TAG, "Service configured and ready!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // User is typing
                Log.d(TAG, "Text changed detected")
                // You can add typing speed tracking logic here
            }

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // View gained focus
                Log.d(TAG, "View focused: ${event.className}")
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Window changed
                Log.d(TAG, "Window changed: ${event.packageName}")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Service destroyed")
    }
}