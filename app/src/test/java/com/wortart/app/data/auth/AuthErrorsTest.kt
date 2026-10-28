package com.wortart.app.data.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthErrorsTest {

    @Test
    fun closingTheAccountChooserShowsNoMessage() {
        assertNull(friendlyAuthMessage(GetCredentialCancellationException()))
    }

    @Test
    fun missingGoogleAccountIsExplained() {
        val message = friendlyAuthMessage(NoCredentialException())

        assertEquals("No Google account found on this device. Add one in Settings.", message)
    }

    @Test
    fun networkProblemsAreExplained() {
        val message = friendlyAuthMessage(FirebaseNetworkException("offline"))

        assertEquals("No internet connection. Please try again.", message)
    }

    @Test
    fun ourOwnMessagesAreShownAsTheyAre() {
        assertEquals("Accounts are not set up yet.", friendlyAuthMessage(IllegalStateException("Accounts are not set up yet.")))
    }

    @Test
    fun unknownErrorsGetAGenericMessage() {
        assertEquals("Something went wrong. Please try again.", friendlyAuthMessage(RuntimeException("x")))
        assertNotNull(friendlyAuthMessage(IllegalStateException()))
    }
}
