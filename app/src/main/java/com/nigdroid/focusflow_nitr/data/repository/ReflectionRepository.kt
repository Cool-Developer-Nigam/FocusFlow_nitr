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
            reflectionRef.set(newReflection).await()
            Result.success(newReflection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReflections(userId: String, limit: Int = 50): Result<List<Reflection>> {
        return try {
            val snapshot = firestore.collection("reflections")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val reflections = snapshot.documents.mapNotNull {
                it.toObject(Reflection::class.java)
            }
            Result.success(reflections)
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }

    suspend fun deleteReflection(reflectionId: String): Result<Unit> {
        return try {
            firestore.collection("reflections")
                .document(reflectionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
