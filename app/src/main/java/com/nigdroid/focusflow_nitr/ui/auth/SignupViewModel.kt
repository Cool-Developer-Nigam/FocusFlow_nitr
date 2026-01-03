package com.nigdroid.focusflow_nitr.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.focusflow_nitr.data.model.User
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {
    private val repository = UserRepository()

    val name = MutableLiveData("")
    val email = MutableLiveData("")
    val password = MutableLiveData("")

    private val _signupResult = MutableLiveData<Result<User>>()
    val signupResult: LiveData<Result<User>> = _signupResult

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    /**
     * Standard email/password signup
     */
    fun signup() {
        val nameValue = name.value ?: return
        val emailValue = email.value ?: return
        val passwordValue = password.value ?: return

        if (nameValue.isBlank() || emailValue.isBlank() || passwordValue.isBlank()) {
            return
        }

        if (passwordValue.length < 6) {
            _signupResult.value = Result.failure(
                Exception("Password must be at least 6 characters")
            )
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val result = repository.createUser(emailValue, passwordValue, nameValue)
            _signupResult.value = result
            _loading.value = false
        }
    }

    /**
     * Sync Google user profile (including avatar URL) after Google Sign-In
     */
    fun syncGoogleUserProfile(userId: String) {
        viewModelScope.launch {
            val result = repository.syncGoogleUserProfile(userId)
            _signupResult.value = result
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    fun createOrUpdateGoogleUser(userId: String) {
        syncGoogleUserProfile(userId)
    }
}