package com.wortart.app.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun practiceIsShownFirstAndTabsSwitchContent() {
        composeRule.setContent {
            WortArtTheme {
                MainScreen(
                    practice = { Text("practice-content") },
                    explore = { Text("explore-content") },
                    rules = { Text("rules-content") },
                    stats = { Text("stats-content") },
                )
            }
        }

        composeRule.onNodeWithText("practice-content").assertExists()
        composeRule.onNodeWithText("explore-content").assertDoesNotExist()

        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("explore-content").assertExists()
        composeRule.onNodeWithText("practice-content").assertDoesNotExist()

        composeRule.onNodeWithText("Rules").performClick()
        composeRule.onNodeWithText("rules-content").assertExists()
        composeRule.onNodeWithText("explore-content").assertDoesNotExist()

        composeRule.onNodeWithText("Stats").performClick()
        composeRule.onNodeWithText("stats-content").assertExists()
        composeRule.onNodeWithText("rules-content").assertDoesNotExist()

        composeRule.onNodeWithText("Practice").performClick()
        composeRule.onNodeWithText("practice-content").assertExists()
    }
}
