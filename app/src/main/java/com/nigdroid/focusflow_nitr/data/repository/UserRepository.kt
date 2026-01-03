package com.nigdroid.focusflow_nitr.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Create user with email/password
     */
    suspend fun createUser(email: String, password: String, name: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID is null")

            val user = User(
                userId = userId,
                name = name,
                email = email,
                avatarUrl = "", // No avatar for email/password signup
                createdAt = Timestamp.now(),
                lastActive = Timestamp.now()
            )

            firestore.collection("users").document(userId).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with email/password
     */
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID is null")

            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java) ?: throw Exception("User not found")

            // Update last active
            firestore.collection("users").document(userId)
                .update("lastActive", Timestamp.now())
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create user profile (for Google Sign-In)
     * This is called after Firebase Auth is already done
     */
    suspend fun createUserProfile(user: User): Result<User> {
        return try {
            firestore.collection("users")
                .document(user.userId)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()

            if (document.exists()) {
                val user = document.toObject(User::class.java)
                    ?: throw Exception("User data is null")

                // Update last active
                firestore.collection("users").document(userId)
                    .update("lastActive", Timestamp.now())
                    .await()

                // Sync Google photo URL if available (keeps Firestore in sync with Firebase Auth)
                val firebaseUser = auth.currentUser
                if (firebaseUser != null && firebaseUser.photoUrl != null) {
                    val currentPhotoUrl = firebaseUser.photoUrl.toString()
                    if (user.avatarUrl != currentPhotoUrl) {
                        updateUserAvatar(userId, currentPhotoUrl)
                        Result.success(user.copy(avatarUrl = currentPhotoUrl))
                    } else {
                        Result.success(user)
                    }
                } else {
                    Result.success(user)
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.userId)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user's avatar URL (for Google Sign-In users)
     */
    suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("avatarUrl", avatarUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync Google user profile (including avatar URL) after Google Sign-In
     * This creates a new user or updates existing user with latest Google data
     */
    suspend fun syncGoogleUserProfile(userId: String): Result<User> {
        return try {
            val firebaseUser = auth.currentUser ?: throw Exception("No authenticated user")

            // Check if user already exists
            val existingUserResult = getUser(userId)

            val user = if (existingUserResult.isSuccess) {
                // Update existing user with latest Google data
                val existingUser = existingUserResult.getOrThrow()
                existingUser.copy(
                    name = firebaseUser.displayName ?: existingUser.name,
                    email = firebaseUser.email ?: existingUser.email,
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: existingUser.avatarUrl,
                    lastActive = Timestamp.now()
                )
            } else {
                // Create new user profile
                User(
                    userId = userId,
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = Timestamp.now(),
                    lastActive = Timestamp.now()
                )
            }

            // Save/update to Firestore
            firestore.collection("users")
                .document(userId)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current Firebase user's display name
     */
    fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }

    /**
     * Get current Firebase user's email
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Get current Firebase user's photo URL
     */
    fun getCurrentUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }
}