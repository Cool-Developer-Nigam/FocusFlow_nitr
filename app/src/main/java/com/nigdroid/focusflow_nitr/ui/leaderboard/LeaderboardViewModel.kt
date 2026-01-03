package com.nigdroid.focusflow_nitr.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LeaderboardEntry(
    val userId: String = "",
    val name: String = "",
    val hours: Double = 0.0,
    val rank: Int = 0,
    val change: Int = 0,
    val avatarUrl: String = "",
    val points: Int = 0
)

class LeaderboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()

    private val _leaderboard = MutableLiveData<List<LeaderboardEntry>>()
    val leaderboard: LiveData<List<LeaderboardEntry>> = _leaderboard

    private val _userRank = MutableLiveData<LeaderboardEntry>()
    val userRank: LiveData<LeaderboardEntry> = _userRank

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Get all users with their total focus hours
                val usersSnapshot = firestore.collection("users")
                    .get()
                    .await()

                val entries = usersSnapshot.documents.mapNotNull { doc ->
                    try {
                        LeaderboardEntry(
                            userId = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            hours = doc.getDouble("totalFocusHours") ?: 0.0,
                            avatarUrl = doc.getString("avatarUrl") ?: "",
                            points = doc.getLong("points")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                    .sortedByDescending { it.hours }
                    .mapIndexed { index, entry ->
                        entry.copy(rank = index + 1)
                    }

                _leaderboard.value = entries

                // Find current user's rank
                val currentUserId = userRepository.getCurrentUserId()
                val userEntry = entries.find { it.userId == currentUserId }
                if (userEntry != null) {
                    _userRank.value = userEntry
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshLeaderboard() {
        loadLeaderboard()
    }
}