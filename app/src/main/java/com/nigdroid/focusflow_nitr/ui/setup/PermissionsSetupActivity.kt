package com.nigdroid.focusflow_nitr.ui.setup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.databinding.ActivityPermissionSetupBinding
import com.nigdroid.focusflow_nitr.ui.main.MainActivity
import com.nigdroid.focusflow_nitr.utils.PermissionHelper

class PermissionsSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionSetupBinding
    private val handler = Handler(Looper.getMainLooper())
    private var checkRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        startPermissionCheck()
        updatePermissionStatus()
    }

    private fun setupListeners() {
        binding.grantUsageStatsButton.setOnClickListener {
            PermissionHelper.openUsageStatsSettings(this)
        }

        binding.enableAccessibilityButton.setOnClickListener {
            // Show detailed instructions before opening
            showAccessibilityInstructions()
        }

        binding.continueButton.setOnClickListener {
            if (allPermissionsGranted()) {
                navigateToMain()
            } else {
                // Show which permissions are missing
                showMissingPermissionsMessage()
            }
        }

        binding.skipButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun showAccessibilityInstructions() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Enable Typing Monitor")
            .setMessage(
                "Follow these steps:\n\n" +
                        "1. In the next screen, scroll down to find 'FocusFlow' or 'Typing Monitor'\n\n" +
                        "2. Tap on it to open the service settings\n\n" +
                        "3. Toggle the switch to ON\n\n" +
                        "4. Confirm when prompted\n\n" +
                        "Then return to this app."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                PermissionHelper.openAccessibilitySettings(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMissingPermissionsMessage() {
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(this)
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(this)

        val message = when {
            !hasUsageStats && !hasAccessibility ->
                "Please grant both permissions to continue"
            !hasUsageStats ->
                "Please grant Usage Stats permission to continue"
            !hasAccessibility ->
                "Please enable Accessibility Service to continue"
            else -> ""
        }

        if (message.isNotEmpty()) {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun startPermissionCheck() {
        checkRunnable = object : Runnable {
            override fun run() {
                updatePermissionStatus()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(checkRunnable!!)
    }

    private fun updatePermissionStatus() {
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(this)
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(this)

        // Update Usage Stats status
        if (hasUsageStats) {
            binding.usageStatsStatus.text = "✅ Granted"
            binding.usageStatsStatus.setTextColor(getColor(R.color.success_green))
            binding.grantUsageStatsButton.isEnabled = false
            binding.grantUsageStatsButton.text = "Granted"
            binding.grantUsageStatsButton.alpha = 0.5f
        } else {
            binding.usageStatsStatus.text = "❌ Not Granted"
            binding.usageStatsStatus.setTextColor(getColor(R.color.error_red))
            binding.grantUsageStatsButton.isEnabled = true
            binding.grantUsageStatsButton.text = "Grant Permission"
            binding.grantUsageStatsButton.alpha = 1.0f
        }

        // Update Accessibility status
        if (hasAccessibility) {
            binding.accessibilityStatus.text = "✅ Enabled"
            binding.accessibilityStatus.setTextColor(getColor(R.color.success_green))
            binding.enableAccessibilityButton.isEnabled = false
            binding.enableAccessibilityButton.text = "Enabled"
            binding.enableAccessibilityButton.alpha = 0.5f
        } else {
            binding.accessibilityStatus.text = "❌ Not Enabled"
            binding.accessibilityStatus.setTextColor(getColor(R.color.error_red))
            binding.enableAccessibilityButton.isEnabled = true
            binding.enableAccessibilityButton.text = "Enable Service"
            binding.enableAccessibilityButton.alpha = 1.0f
        }

        // Update continue button - ALWAYS ENABLED but shows message if permissions missing
        binding.continueButton.isEnabled = true

        if (allPermissionsGranted()) {
            binding.continueButton.text = "Continue to App ✓"
            binding.continueButton.setBackgroundColor(getColor(R.color.success_green))
        } else {
            binding.continueButton.text = "Continue Anyway"
            binding.continueButton.setBackgroundColor(getColor(R.color.purple_primary))
        }

        // Update skip button visibility
        binding.skipButton.visibility = if (allPermissionsGranted()) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return PermissionHelper.hasUsageStatsPermission(this) &&
                PermissionHelper.isAccessibilityServiceEnabled(this)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        checkRunnable?.let { handler.removeCallbacks(it) }
    }
}