package com.nigdroid.focusflow_nitr.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.databinding.ActivityMainBinding
import com.nigdroid.focusflow_nitr.ui.focus.FocusSessionActivity
import com.nigdroid.focusflow_nitr.ui.goals.GoalsFragment
import com.nigdroid.focusflow_nitr.ui.home.HomeFragment
import com.nigdroid.focusflow_nitr.ui.insights.InsightsFragment
import com.nigdroid.focusflow_nitr.ui.leaderboard.LeaderboardFragment
import com.nigdroid.focusflow_nitr.ui.profile.ProfileFragment
import com.nigdroid.focusflow_nitr.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        setupBottomNavigation()
        setupFab()
        checkPermissions() // ADD THIS
    }

    private fun checkPermissions() {
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(this)
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(this)

        if (!hasUsageStats || !hasAccessibility) {
            val message = when {
                !hasUsageStats && !hasAccessibility ->
                    "Please grant required permissions for full functionality"
                !hasUsageStats ->
                    "Screen time tracking requires usage stats permission"
                else ->
                    "Typing monitoring requires accessibility service"
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("Grant") {
                    PermissionHelper.checkAndRequestAllPermissions(this)
                }
                .show()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_goals -> {
                    loadFragment(GoalsFragment())
                    true
                }
                R.id.nav_insights -> {
                    loadFragment(InsightsFragment())
                    true
                }
                R.id.nav_leaderboard -> {
                    loadFragment(LeaderboardFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fabStartSession.setOnClickListener {
            startActivity(Intent(this, FocusSessionActivity::class.java))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}