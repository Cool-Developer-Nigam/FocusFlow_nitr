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
        // Set default achievements immediately
        setDefaultAchievements()

        // Then load actual data
        loadProfile()
        loadReflections()
    }

    private fun setDefaultAchievements() {
        // Set default locked achievements
        val defaultAchievements = listOf(
            Achievement(
                achievementId = "1",
                name = "7-Day Streak",
                description = "Study for 7 consecutive days",
                iconName = "🔥",
                points = 100,
                requirement = 7,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            ),
            Achievement(
                achievementId = "2",
                name = "100 Hours Club",
                description = "Complete 100 total focus hours",
                iconName = "⏰",
                points = 500,
                requirement = 100,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            ),
            Achievement(
                achievementId = "3",
                name = "Early Bird",
                description = "Start studying before 6 AM",
                iconName = "🌅",
                points = 50,
                requirement = 1,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            ),
            Achievement(
                achievementId = "4",
                name = "Goal Crusher",
                description = "Complete 10 goals",
                iconName = "🎯",
                points = 200,
                requirement = 10,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            ),
            Achievement(
                achievementId = "5",
                name = "Focus Master",
                description = "Maintain 90+ focus score for 5 sessions",
                iconName = "💎",
                points = 300,
                requirement = 5,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            )
        )
        _achievements.value = defaultAchievements
        android.util.Log.d("ProfileViewModel", "Default achievements set: ${defaultAchievements.size}")
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    android.util.Log.d("ProfileViewModel", "Loading profile for user: $userId")

                    userRepository.getUser(userId).onSuccess { user ->
                        android.util.Log.d("ProfileViewModel", "User loaded: ${user.name}, streak=${user.streak}, hours=${user.totalFocusHours}")
                        _user.value = user
                        loadAchievements(user)
                    }.onFailure { error ->
                        android.util.Log.e("ProfileViewModel", "Error loading user", error)
                    }
                } else {
                    android.util.Log.e("ProfileViewModel", "User ID is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Exception loading profile", e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun loadAchievements(user: User) {
        val achievements = listOf(
            Achievement(
                achievementId = "1",
                name = "7-Day Streak",
                description = "Study for 7 consecutive days",
                iconName = "🔥",
                points = 100,
                requirement = 7,
                isUnlocked = user.streak >= 7,
                progress = user.streak.coerceAtMost(7),
                unlockedAt = if (user.streak >= 7) System.currentTimeMillis() else 0
            ),
            Achievement(
                achievementId = "2",
                name = "100 Hours Club",
                description = "Complete 100 total focus hours",
                iconName = "⏰",
                points = 500,
                requirement = 100,
                isUnlocked = user.totalFocusHours >= 100,
                progress = user.totalFocusHours.toInt().coerceAtMost(100),
                unlockedAt = if (user.totalFocusHours >= 100) System.currentTimeMillis() else 0
            ),
            Achievement(
                achievementId = "3",
                name = "Early Bird",
                description = "Start studying before 6 AM",
                iconName = "🌅",
                points = 50,
                requirement = 1,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            ),
            Achievement(
                achievementId = "4",
                name = "Goal Crusher",
                description = "Complete 10 goals",
                iconName = "🎯",
                points = 200,
                requirement = 10,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            ),
            Achievement(
                achievementId = "5",
                name = "Focus Master",
                description = "Maintain 90+ focus score for 5 sessions",
                iconName = "💎",
                points = 300,
                requirement = 5,
                isUnlocked = false,
                progress = 0,
                unlockedAt = 0
            )
        )
        _achievements.value = achievements
        android.util.Log.d("ProfileViewModel", "Achievements loaded: ${achievements.size}, unlocked: ${achievements.count { it.isUnlocked }}")
    }

    fun loadReflections() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId == null) {
                    android.util.Log.e("ProfileViewModel", "User ID is null when loading reflections")
                    _reflections.value = emptyList()
                    return@launch
                }

                android.util.Log.d("ProfileViewModel", "Loading reflections for user: $userId")

                reflectionRepository.getUserReflections(userId, 30).onSuccess { reflections ->
                    android.util.Log.d("ProfileViewModel", "Reflections loaded: ${reflections.size}")
                    _reflections.value = reflections
                }.onFailure { error ->
                    android.util.Log.e("ProfileViewModel", "Error loading reflections", error)
                    _reflections.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Exception loading reflections", e)
                _reflections.value = emptyList()
            }
        }
    }

    fun addReflection(mood: String, textNote: String, voiceNoteUrl: String = "") {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId == null) {
                    android.util.Log.e("ProfileViewModel", "Cannot add reflection: User ID is null")
                    return@launch
                }

                val reflection = Reflection(
                    userId = userId,
                    date = Timestamp.now(),
                    mood = mood,
                    textNote = textNote,
                    voiceNoteUrl = voiceNoteUrl
                )

                android.util.Log.d("ProfileViewModel", "Creating reflection for user: $userId")

                reflectionRepository.createReflection(reflection).onSuccess {
                    android.util.Log.d("ProfileViewModel", "Reflection created successfully")
                    loadReflections() // Reload to show new reflection
                }.onFailure { error ->
                    android.util.Log.e("ProfileViewModel", "Error creating reflection", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Exception adding reflection", e)
            }
        }
    }

    fun deleteReflection(reflectionId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "Deleting reflection: $reflectionId")

                reflectionRepository.deleteReflection(reflectionId).onSuccess {
                    android.util.Log.d("ProfileViewModel", "Reflection deleted successfully")
                    loadReflections()
                }.onFailure { error ->
                    android.util.Log.e("ProfileViewModel", "Error deleting reflection", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Exception deleting reflection", e)
            }
        }
    }

    fun refreshProfile() {
        android.util.Log.d("ProfileViewModel", "Refreshing profile data")
        loadProfile()
        loadReflections()
    }
}