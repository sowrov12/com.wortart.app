package com.wortart.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
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
class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(initial: MainTab = MainTab.Home, onTab: (MainTab) -> Unit = {}) {
        composeRule.setContent {
            WortArtTheme {
                var selected by remember { mutableStateOf(initial) }
                MainScreen(
                    selectedTab = selected,
                    onSelectTab = {
                        selected = it
                        onTab(it)
                    },
                    home = { Text("home-content") },
                    explore = { Text("explore-content") },
                    rules = { Text("rules-content") },
                    stats = { Text("stats-content") },
                    profile = { Text("profile-content") },
                )
            }
        }
    }

    @Test
    fun homeIsShownFirstAndTabsSwitchContent() {
        show()

        composeRule.onNodeWithText("home-content").assertExists()
        composeRule.onNodeWithText("explore-content").assertDoesNotExist()

        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("explore-content").assertExists()
        composeRule.onNodeWithText("home-content").assertDoesNotExist()

        composeRule.onNodeWithText("Rules").performClick()
        composeRule.onNodeWithText("rules-content").assertExists()
        composeRule.onNodeWithText("explore-content").assertDoesNotExist()

        composeRule.onNodeWithText("Stats").performClick()
        composeRule.onNodeWithText("stats-content").assertExists()
        composeRule.onNodeWithText("rules-content").assertDoesNotExist()

        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("profile-content").assertExists()
        composeRule.onNodeWithText("stats-content").assertDoesNotExist()

        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("home-content").assertExists()
    }

    @Test
    fun theCallerCanStartOnAnyTab() {
        show(initial = MainTab.Stats)

        composeRule.onNodeWithText("stats-content").assertExists()
        composeRule.onNodeWithText("home-content").assertDoesNotExist()
    }

    @Test
    fun selectingATabReportsIt() {
        val picked = mutableListOf<MainTab>()
        show(onTab = { picked += it })

        composeRule.onNodeWithText("Rules").performClick()
        composeRule.onNodeWithText("Profile").performClick()

        composeRule.runOnIdle { assertEquals(listOf(MainTab.Rules, MainTab.Profile), picked) }
    }
}
