package com.wortart.app.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

/** A short, readable message for a failed Firestore action. */
fun friendlyDataMessage(error: Throwable): String = when {
    error is FirebaseFirestoreException &&
        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Permission denied. Check the admin flag and that the Firestore rules are published."
    error is FirebaseFirestoreException &&
        error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
        "No internet connection. Please try again."
    error is FirebaseNetworkException -> "No internet connection. Please try again."
    error is IllegalStateException -> error.message ?: "Something went wrong. Please try again."
    else -> "Something went wrong. Please try again."
}
