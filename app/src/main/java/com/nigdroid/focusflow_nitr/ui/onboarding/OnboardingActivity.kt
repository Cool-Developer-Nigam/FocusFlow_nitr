package com.nigdroid.focusflow_nitr.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.local.PreferencesManager
import com.nigdroid.focusflow_nitr.databinding.ActivityOnboardingBinding
import com.nigdroid.focusflow_nitr.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        binding.getStartedButton.setOnClickListener {
            prefsManager.isFirstLaunch = false
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}