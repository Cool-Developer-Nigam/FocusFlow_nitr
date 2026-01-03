package com.nigdroid.focusflow_nitr.data.repository


import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.data.model.Goal

import kotlinx.coroutines.tasks.await

class GoalRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getUserGoals(userId: String): Result<List<Goal>> {
        return try {
            val snapshot = firestore.collection("goals")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val goals = snapshot.documents.mapNotNull { it.toObject(Goal::class.java) }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoalProgress(goalId: String, progress: Double): Result<Unit> {
        return try {
            firestore.collection("goals").document(goalId)
                .update("progress", progress)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}