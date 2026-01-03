package com.nigdroid.focusflow_nitr.ui.auth



import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.local.PreferencesManager
import com.nigdroid.focusflow_nitr.databinding.ActivitySignupBinding
import com.nigdroid.focusflow_nitr.ui.main.MainActivity
import com.nigdroid.focusflow_nitr.utils.GoogleSignInHelper
import com.nigdroid.focusflow_nitr.utils.toast
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()
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
        binding = ActivitySignupBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        googleSignInHelper = GoogleSignInHelper(this)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.signupResult.observe(this) { result ->
            result.onSuccess { user ->
                prefsManager.isLoggedIn = true
                prefsManager.userId = user.userId
                toast("Welcome, ${user.name}!")
                navigateToMain()
            }
            result.onFailure { error ->
                toast("Signup failed: ${error.message}")
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.signupButton.isEnabled = !isLoading
            binding.googleButton.isEnabled = !isLoading

            binding.signupButton.text = if (isLoading) "Creating account..." else "Sign Up"
        }
    }

    private fun setupListeners() {
        binding.signupButton.setOnClickListener {
            viewModel.signup()
        }

        binding.googleButton.setOnClickListener {
            startGoogleSignIn()
        }

        binding.loginLink.setOnClickListener {
            finish()
        }
    }

    private fun startGoogleSignIn() {
        binding.googleButton.isEnabled = false
        binding.googleButton.text = "Connecting..."

        lifecycleScope.launch {
            try {
                android.util.Log.d("GoogleSignIn", "Starting Google Sign-In from Signup...")

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