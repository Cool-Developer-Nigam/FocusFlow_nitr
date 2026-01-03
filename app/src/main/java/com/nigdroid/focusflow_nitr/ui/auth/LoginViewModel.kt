package com.nigdroid.focusflow_nitr.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.focusflow_nitr.data.model.User
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import kotlinx.coroutines.launch


class LoginViewModel : ViewModel() {
    private val repository = UserRepository()

    val email = MutableLiveData("")
    val password = MutableLiveData("")

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    /**
     * Standard email/password login
     */
    fun login() {
        val emailValue = email.value ?: return
        val passwordValue = password.value ?: return

        if (emailValue.isBlank() || passwordValue.isBlank()) {
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val result = repository.loginUser(emailValue, passwordValue)
            _loginResult.value = result
            _loading.value = false
        }
    }

    /**
     * Sync Google user profile (including avatar URL) after Google Sign-In
     */
    fun syncGoogleUserProfile(userId: String) {
        viewModelScope.launch {
            val result = repository.syncGoogleUserProfile(userId)
            _loginResult.value = result
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    fun createOrUpdateGoogleUser(userId: String) {
        syncGoogleUserProfile(userId)
    }
}