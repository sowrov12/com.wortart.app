package com.wortart.app.data.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException

/**
 * A short message for the learner, or null when nothing should be shown
 * (the person simply closed the Google account chooser).
 */
fun friendlyAuthMessage(error: Throwable): String? = when (error) {
    is GetCredentialCancellationException -> null
    is NoCredentialException -> "No Google account found on this device. Add one in Settings."
    is FirebaseNetworkException -> "No internet connection. Please try again."
    is IllegalStateException -> error.message ?: "Something went wrong. Please try again."
    else -> "Something went wrong. Please try again."
}
