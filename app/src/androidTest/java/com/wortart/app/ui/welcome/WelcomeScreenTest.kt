package com.wortart.app.ui.welcome

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheBrandAndTheTagline() {
        composeRule.setContent { WortArtTheme { WelcomeScreen(onGetStarted = {}, onLogin = {}) } }

        composeRule.onNodeWithText("WortArt").assertExists()
        composeRule.onNodeWithText("German Grammar & Vocab Master").assertExists()
        composeRule.onNodeWithText("Mehr lernen. Mehr können.").assertExists()
    }

    @Test
    fun getStartedAndLoginReportTheChoice() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            WortArtTheme {
                WelcomeScreen(onGetStarted = { calls += "start" }, onLogin = { calls += "login" })
            }
        }

        composeRule.onNodeWithText("Get Started").performClick()
        composeRule.onNodeWithText("Login").performClick()

        composeRule.runOnIdle { assertEquals(listOf("start", "login"), calls) }
    }
}
