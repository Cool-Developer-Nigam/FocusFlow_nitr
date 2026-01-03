package com.nigdroid.focusflow_nitr.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.nigdroid.focusflow_nitr.data.model.Achievement
import com.nigdroid.focusflow_nitr.data.model.Reflection
import com.nigdroid.focusflow_nitr.data.model.User
import com.nigdroid.focusflow_nitr.data.repository.ReflectionRepository
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val reflectionRepository = ReflectionRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _achievements = MutableLiveData<List<Achievement>>()
    val achievements: LiveData<List<Achievement>> = _achievements

    private val _reflections = MutableLiveData<List<Reflection>>()
    val reflections: LiveData<List<Reflection>> = _reflections

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    init {
        loadProfile()
        loadReflections()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _loading.value = true
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {
                userRepository.getUser(userId).onSuccess { user ->
                    _user.value = user
                    loadAchievements(user)
                }
            }
            _loading.value = false
        }
    }

    private fun loadAchievements(user: User) {
        // Mock achievements - you can load from Firebase later
        val achievements = listOf(
            Achievement(
                achievementId = "1",
                name = "7-Day Streak",
                description = "Study for 7 consecutive days",
                iconName = "🔥",
                points = 100,
                requirement = 7,
                isUnlocked = user.streak >= 7,
                progress = user.streak
            ),
            Achievement(
                achievementId = "2",
                name = "100 Hours Club",
                description = "Complete 100 total focus hours",
                iconName = "⏰",
                points = 500,
                requirement = 100,
                isUnlocked = user.totalFocusHours >= 100,
                progress = user.totalFocusHours.toInt()
            ),
            Achievement(
                achievementId = "3",
                name = "Early Bird",
                description = "Start studying before 6 AM",
                iconName = "🌅",
                points = 50,
                requirement = 1,
                isUnlocked = false,
                progress = 0
            ),
            Achievement(
                achievementId = "4",
                name = "Goal Crusher",
                description = "Complete 10 goals",
                iconName = "🎯",
                points = 200,
                requirement = 10,
                isUnlocked = false,
                progress = 3
            )
        )
        _achievements.value = achievements
    }

    fun loadReflections() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch

            reflectionRepository.getUserReflections(userId, 30).onSuccess { reflections ->
                _reflections.value = reflections
            }
        }
    }

    fun addReflection(mood: String, textNote: String, voiceNoteUrl: String = "") {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch

            val reflection = Reflection(
                userId = userId,
                date = Timestamp.now(),
                mood = mood,
                textNote = textNote,
                voiceNoteUrl = voiceNoteUrl
            )

            reflectionRepository.createReflection(reflection).onSuccess {
                loadReflections() // Reload to show new reflection
            }
        }
    }

    fun deleteReflection(reflectionId: String) {
        viewModelScope.launch {
            reflectionRepository.deleteReflection(reflectionId).onSuccess {
                loadReflections()
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
        loadReflections()
    }

    fun formatDate(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    fun getMoodEmoji(mood: String): String {
        return when (mood.lowercase()) {
            "great", "happy", "excellent" -> "😊"
            "good", "okay", "fine" -> "😐"
            "tired", "exhausted", "stressed" -> "😫"
            "sad", "bad", "terrible" -> "😢"
            else -> "😐"
        }
    }
}