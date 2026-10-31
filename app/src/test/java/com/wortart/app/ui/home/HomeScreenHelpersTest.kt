package com.wortart.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeScreenHelpersTest {

    @Test
    fun theGreetingUsesTheFirstWordOfTheName() {
        assertEquals("Sowrov", firstNameOf("Sowrov Hin"))
        assertEquals("Ann", firstNameOf("  Ann  "))
        assertEquals("Madonna", firstNameOf("Madonna"))
    }

    @Test
    fun withoutAUsableNameThereIsNoFirstName() {
        assertNull(firstNameOf(null))
        assertNull(firstNameOf(""))
        assertNull(firstNameOf("   "))
    }
}
