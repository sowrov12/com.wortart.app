package com.wortart.app.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Google Sign-In through Credential Manager, connected to Firebase Auth. */
class FirebaseAuthRepository(context: Context) : AuthRepository {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override val isConfigured: Boolean = true

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(user?.let { AuthUser(uid = it.uid, email = it.email, displayName = it.displayName) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(context: Context): Result<Unit> = resultOf {
        val idToken = requestGoogleIdToken(context)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun reauthenticate(context: Context): Result<Unit> = resultOf {
        val user = auth.currentUser ?: error("Not signed in.")
        val idToken = requestGoogleIdToken(context)
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun deleteCurrentUser(): Result<Unit> = resultOf {
        val user = auth.currentUser ?: error("Not signed in.")
        user.delete().await()
        Unit
    }

    override suspend fun signOut(context: Context) {
        auth.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Signing out of Firebase is what matters; clearing the chooser state is best effort.
        }
    }

    private suspend fun requestGoogleIdToken(context: Context): String {
        val webClientId = webClientId()
            ?: error("Google Sign-In is not configured: add google-services.json with a web client ID.")

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected sign-in response.")
    }

    /**
     * The web client ID is generated from google-services.json as the string resource
     * `default_web_client_id`. It is looked up by name so the app still compiles without it.
     */
    private fun webClientId(): String? {
        val id = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        return if (id == 0) null else appContext.getString(id)
    }
}

/** Runs [block] and wraps failures in a [Result]; cancellation is always passed on. */
internal inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
