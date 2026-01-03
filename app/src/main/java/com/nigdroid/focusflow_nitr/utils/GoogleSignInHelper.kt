package com.nigdroid.focusflow_nitr.utils

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleSignInHelper(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    // This will automatically get the Web Client ID from your google-services.json
    private val webClientId: String by lazy {
        try {
            // This resource is automatically generated from google-services.json
            val resourceId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                throw Exception("Web Client ID not found in google-services.json")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // FALLBACK: If the above fails, manually paste your Web Client ID here
            "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com"
        }
    }

    private val signInRequest = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
        )
        .setAutoSelectEnabled(false)
        .build()

    /**
     * Start Google Sign-In flow
     */
    suspend fun beginSignIn(): IntentSenderRequest? {
        return try {
            val result = oneTapClient.beginSignIn(signInRequest).await()
            IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Handle sign-in result
     */
    suspend fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            if (data == null) {
                return Result.failure(Exception("Sign-in data is null"))
            }

            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val userId = authResult.user?.uid
                    ?: return Result.failure(Exception("User ID is null"))

                Result.success(userId)
            } else {
                Result.failure(Exception("No ID token received"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
