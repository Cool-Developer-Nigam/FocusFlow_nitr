package com.nigdroid.focusflow_nitr.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.data.model.Session

import kotlinx.coroutines.tasks.await

class SessionRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createSession(session: Session): Result<Session> {
        return try {
            val sessionRef = firestore.collection("sessions").document()
            val newSession = session.copy(sessionId = sessionRef.id)
            sessionRef.set(newSession).await()
            Result.success(newSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSession(sessionId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("sessions")
                .document(sessionId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSessions(userId: String): Result<List<Session>> {
        return try {
            val snapshot = firestore.collection("sessions")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { it.toObject(Session::class.java) }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
