package com.wortart.app.ui.home

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = StatsSummary(
        germanScore = 782,
        streakDays = 6,
        cardsPlayed = 48,
        successPercent = 92,
        practiceTimeSeconds = 5040,
    )

    private fun show(
        userName: String? = null,
        isMember: Boolean = false,
        summary: StatsSummary = this.summary,
        calls: MutableList<String> = mutableListOf(),
    ) {
        composeRule.setContent {
            WortArtTheme {
                HomeScreen(
                    userName = userName,
                    isMember = isMember,
                    summary = summary,
                    onOpenFlashcards = { calls += "flashcards" },
                    onOpenExplore = { calls += "explore" },
                    onOpenRules = { calls += "rules" },
                    onOpenStats = { calls += "stats" },
                    onOpenSearch = { calls += "search" },
                    onOpenProfile = { calls += "profile" },
                )
            }
        }
    }

    @Test
    fun greetsTheLearnerAndShowsTheProgress() {
        show()

        composeRule.onNodeWithText("WortArt").assertExists()
        composeRule.onNodeWithText("Hallo, Learner!").assertExists()
        composeRule.onNodeWithText("Keep going! Your German journey is getting stronger.").assertExists()

        composeRule.onNodeWithText("German Score").assertExists()
        composeRule.onNodeWithText("782").assertExists()
        composeRule.onNodeWithText("Daily Streak").assertExists()
        composeRule.onNodeWithText("6 days").assertExists()
        composeRule.onNodeWithText("Cards Played").assertExists()
        composeRule.onNodeWithText("48").assertExists()
        composeRule.onNodeWithText("Success Rate").assertExists()
        composeRule.onNodeWithText("92%").assertExists()
        composeRule.onNodeWithText("Practice Time").assertExists()
        composeRule.onNodeWithText("1 h 24 min").assertExists()
    }

    @Test
    fun greetsTheSignedInUserByFirstName() {
        show(userName = "Sowrov")

        composeRule.onNodeWithText("Hallo, Sowrov!").assertExists()
    }

    @Test
    fun theCrownAppearsForMembersOnly() {
        show(isMember = false)
        composeRule.onNodeWithText("👑").assertDoesNotExist()
    }

    @Test
    fun membersSeeTheCrown() {
        show(isMember = true)
        composeRule.onNodeWithText("👑").assertExists()
    }

    @Test
    fun oneDayStreakIsSingular() {
        show(summary = summary.copy(streakDays = 1))

        composeRule.onNodeWithText("1 day").assertExists()
    }

    @Test
    fun everyQuickAccessTileOpensItsArea() {
        val calls = mutableListOf<String>()
        show(calls = calls)

        composeRule.onNodeWithText("Quick Access").performScrollTo().assertExists()
        composeRule.onNodeWithText("Flashcards").performScrollTo().performClick()
        composeRule.onNodeWithText("Explore").performScrollTo().performClick()
        composeRule.onNodeWithText("Grammar Rules").performScrollTo().performClick()
        composeRule.onNodeWithText("Statistics").performScrollTo().performClick()
        composeRule.onNodeWithText("Search").performScrollTo().performClick()
        composeRule.onNodeWithText("Member").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("flashcards", "explore", "rules", "stats", "search", "profile"), calls)
        }
    }

    @Test
    fun theProfileIconOpensTheProfile() {
        val calls = mutableListOf<String>()
        show(calls = calls)

        composeRule.onNodeWithContentDescription("Open profile").performClick()

        composeRule.runOnIdle { assertEquals(listOf("profile"), calls) }
    }
}
