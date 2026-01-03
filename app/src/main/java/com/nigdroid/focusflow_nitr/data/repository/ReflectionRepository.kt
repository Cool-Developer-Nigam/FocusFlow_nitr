package com.nigdroid.focusflow_nitr.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nigdroid.focusflow_nitr.data.model.Reflection
import kotlinx.coroutines.tasks.await

class ReflectionRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createReflection(reflection: Reflection): Result<Reflection> {
        return try {
            val reflectionRef = firestore.collection("reflections").document()
            val newReflection = reflection.copy(reflectionId = reflectionRef.id)

            android.util.Log.d("ReflectionRepository", "Creating reflection with ID: ${reflectionRef.id}")

            reflectionRef.set(newReflection).await()

            android.util.Log.d("ReflectionRepository", "Reflection created successfully")

            Result.success(newReflection)
        } catch (e: Exception) {
            android.util.Log.e("ReflectionRepository", "Error creating reflection", e)
            Result.failure(e)
        }
    }

    suspend fun getUserReflections(userId: String, limit: Int = 50): Result<List<Reflection>> {
        return try {
            android.util.Log.d("ReflectionRepository", "Fetching reflections for user: $userId")

            val snapshot = firestore.collection("reflections")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val reflections = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Reflection::class.java)?.copy(reflectionId = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("ReflectionRepository", "Error parsing reflection: ${doc.id}", e)
                    null
                }
            }

            android.util.Log.d("ReflectionRepository", "Fetched ${reflections.size} reflections")

            Result.success(reflections)
        } catch (e: Exception) {
            android.util.Log.e("ReflectionRepository", "Error fetching reflections", e)
            Result.failure(e)
        }
    }

    suspend fun getReflectionByDate(userId: String, date: Timestamp): Result<Reflection?> {
        return try {
            val snapshot = firestore.collection("reflections")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()

            val reflection = snapshot.documents.firstOrNull()?.toObject(Reflection::class.java)
            Result.success(reflection)
        } catch (e: Exception) {
            android.util.Log.e("ReflectionRepository", "Error getting reflection by date", e)
            Result.failure(e)
        }
    }

    suspend fun updateReflection(reflection: Reflection): Result<Unit> {
        return try {
            firestore.collection("reflections")
                .document(reflection.reflectionId)
                .set(reflection)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReflectionRepository", "Error updating reflection", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReflection(reflectionId: String): Result<Unit> {
        return try {
            android.util.Log.d("ReflectionRepository", "Deleting reflection: $reflectionId")

            firestore.collection("reflections")
                .document(reflectionId)
                .delete()
                .await()

            android.util.Log.d("ReflectionRepository", "Reflection deleted successfully")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReflectionRepository", "Error deleting reflection", e)
            Result.failure(e)
        }
    }
}