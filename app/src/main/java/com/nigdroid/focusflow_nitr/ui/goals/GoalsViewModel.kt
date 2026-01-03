package com.nigdroid.focusflow_nitr.ui.goals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.data.model.Goal
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoalsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _activeGoals = MutableLiveData<List<Goal>>()
    val activeGoals: LiveData<List<Goal>> = _activeGoals

    private val _completedGoals = MutableLiveData<List<Goal>>()
    val completedGoals: LiveData<List<Goal>> = _completedGoals

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadGoals()
    }

    fun loadGoals() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "User not logged in"
            _activeGoals.value = emptyList()
            _completedGoals.value = emptyList()
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // SIMPLIFIED QUERY - NO INDEX REQUIRED
                // Load ALL goals for user, then filter in code
                val allGoalsSnapshot = firestore.collection("goals")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val allGoals = allGoalsSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Goal::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("GoalsViewModel", "Error parsing goal: ${e.message}")
                        null
                    }
                }

                // Filter in code - NO INDEX NEEDED
                val active = allGoals
                    .filter { !it.isCompleted && it.isActive }
                    .sortedByDescending { it.createdAt.toDate() }

                val completed = allGoals
                    .filter { it.isCompleted }
                    .sortedByDescending { it.completedAt ?: 0 }
                    .take(10)

                _activeGoals.value = active
                _completedGoals.value = completed
                _error.value = null

                android.util.Log.d("GoalsViewModel", "Loaded ${active.size} active goals, ${completed.size} completed goals")

            } catch (e: Exception) {
                android.util.Log.e("GoalsViewModel", "Error loading goals", e)
                _error.value = "Failed to load goals: ${e.message}"
                _activeGoals.value = emptyList()
                _completedGoals.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("goals")
                    .document(goalId)
                    .delete()
                    .await()
                _error.value = null
                loadGoals()
            } catch (e: Exception) {
                android.util.Log.e("GoalsViewModel", "Error deleting goal", e)
                _error.value = "Failed to delete goal: ${e.message}"
            }
        }
    }

    fun markGoalAsComplete(goalId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("goals")
                    .document(goalId)
                    .update(
                        mapOf(
                            "isCompleted" to true,
                            "completedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                _error.value = null
                loadGoals()
            } catch (e: Exception) {
                android.util.Log.e("GoalsViewModel", "Error completing goal", e)
                _error.value = "Failed to complete goal: ${e.message}"
            }
        }
    }
}