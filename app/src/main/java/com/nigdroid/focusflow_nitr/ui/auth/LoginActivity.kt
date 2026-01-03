package com.nigdroid.focusflow_nitr.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.local.PreferencesManager
import com.nigdroid.focusflow_nitr.databinding.ActivityLoginBinding
import com.nigdroid.focusflow_nitr.ui.main.MainActivity
import com.nigdroid.focusflow_nitr.utils.GoogleSignInHelper
import com.nigdroid.focusflow_nitr.utils.toast
import kotlinx.coroutines.launch
import kotlin.getValue

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var prefsManager: PreferencesManager
    private lateinit var googleSignInHelper: GoogleSignInHelper

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                handleGoogleSignInResult(result.data)
            }
        } else {
            resetGoogleButton()
            toast("Google Sign-In cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        googleSignInHelper = GoogleSignInHelper(this)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                prefsManager.isLoggedIn = true
                prefsManager.userId = user.userId
                toast("Welcome back, ${user.name}!")
                navigateToMain()
            }
            result.onFailure { error ->
                toast("Login failed: ${error.message}")
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.loginButton.isEnabled = !isLoading
            binding.googleButton.isEnabled = !isLoading

            binding.loginButton.text = if (isLoading) "Logging in..." else "Login"
        }
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            when {
                email.isEmpty() -> {
                    binding.emailLayout.error = "Email is required"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    binding.passwordLayout.error = "Password is required"
                    return@setOnClickListener
                }
                else -> {
                    binding.emailLayout.error = null
                    binding.passwordLayout.error = null
                    viewModel.login()
                }
            }
        }

        binding.googleButton.setOnClickListener {
            startGoogleSignIn()
        }

        binding.signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            toast("Password reset feature coming soon!")
        }
    }

    private fun startGoogleSignIn() {
        binding.googleButton.isEnabled = false
        binding.googleButton.text = "Connecting..."

        lifecycleScope.launch {
            try {
                android.util.Log.d("GoogleSignIn", "Starting Google Sign-In...")

                val intentSenderRequest = googleSignInHelper.beginSignIn()

                if (intentSenderRequest != null) {
                    googleSignInLauncher.launch(intentSenderRequest)
                } else {
                    toast("Google Sign-In setup failed. Please try again.")
                    resetGoogleButton()
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
                resetGoogleButton()
            }
        }
    }

    private suspend fun handleGoogleSignInResult(data: Intent?) {
        val result = googleSignInHelper.handleSignInResult(data)

        result.onSuccess { userId ->
            prefsManager.isLoggedIn = true
            prefsManager.userId = userId

            // Sync Google user profile (including photo URL)
            viewModel.syncGoogleUserProfile(userId)

            toast("Signed in with Google!")
            navigateToMain()
        }

        result.onFailure { error ->
            toast("Google Sign-In failed: ${error.message}")
            resetGoogleButton()
        }
    }

    private fun resetGoogleButton() {
        binding.googleButton.isEnabled = true
        binding.googleButton.text = "Sign in with Google"
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}