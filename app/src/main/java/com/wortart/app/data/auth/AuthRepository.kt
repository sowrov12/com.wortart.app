package com.wortart.app.data.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The signed-in Firebase user (just what the app needs). */
data class AuthUser(val uid: String, val email: String?, val displayName: String? = null)

/** Sign-in state and account actions. The real implementation uses Firebase Auth. */
interface AuthRepository {

    /** False when `google-services.json` is missing, so Firebase is not available. */
    val isConfigured: Boolean

    /** Emits the signed-in user, or null when signed out. */
    val currentUser: Flow<AuthUser?>

    /** Opens the Google account chooser (needs an Activity [context]). */
    suspend fun signInWithGoogle(context: Context): Result<Unit>

    /** Asks the user to confirm their Google account again (required before deleting it). */
    suspend fun reauthenticate(context: Context): Result<Unit>

    /** Deletes the Firebase Auth account of the signed-in user. */
    suspend fun deleteCurrentUser(): Result<Unit>

    suspend fun signOut(context: Context)
}

/** Used when Firebase is not set up: nobody can sign in. */
object UnconfiguredAuthRepository : AuthRepository {
    override val isConfigured: Boolean = false
    override val currentUser: Flow<AuthUser?> = flowOf(null)

    private fun notConfigured() =
        Result.failure<Unit>(IllegalStateException("Accounts are not set up yet."))

    override suspend fun signInWithGoogle(context: Context): Result<Unit> = notConfigured()
    override suspend fun reauthenticate(context: Context): Result<Unit> = notConfigured()
    override suspend fun deleteCurrentUser(): Result<Unit> = notConfigured()
    override suspend fun signOut(context: Context) = Unit
}
