package com.wortart.app.ui.stats

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(summary: StatsSummary) {
        composeRule.setContent { WortArtTheme { StatsScreen(summary) } }
    }

    @Test
    fun showsAllStatistics() {
        show(
            StatsSummary(
                germanScore = 175,
                streakDays = 4,
                practicedToday = true,
                cardsPlayed = 10,
                correctAnswers = 8,
                successPercent = 80,
                practiceTimeSeconds = 3900,
                perfectPronunciations = 3,
            ),
        )

        composeRule.onNodeWithText("Your Progress").assertExists()
        composeRule.onNodeWithText("GERMAN SCORE").assertExists()
        composeRule.onNodeWithText("175").assertExists()
        composeRule.onNodeWithText("DAILY STREAK").assertExists()
        composeRule.onNodeWithText("4 days").assertExists()
        composeRule.onNodeWithText("You practiced today. Great job!").assertExists()
        composeRule.onNodeWithText("10").assertExists()
        composeRule.onNodeWithText("Cards played").assertExists()
        composeRule.onNodeWithText("80%").assertExists()
        composeRule.onNodeWithText("Success rate").assertExists()
        composeRule.onNodeWithText("1 h 05 min").assertExists()
        composeRule.onNodeWithText("Practice time").assertExists()
        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText("Perfect pronunciations").assertExists()
    }

    @Test
    fun newLearnerSeesZerosAndAnInvitation() {
        show(StatsSummary())

        composeRule.onNodeWithText("0 days").assertExists()
        composeRule.onNodeWithText("Answer a flashcard to start your streak.").assertExists()
        composeRule.onNodeWithText("0%").assertExists()
        composeRule.onNodeWithText("0 min").assertExists()
        // Cards played and perfect pronunciations.
        composeRule.onAllNodesWithText("0").assertCountEquals(2)
    }

    @Test
    fun streakAtRiskAsksForPracticeToday() {
        show(StatsSummary(streakDays = 1, practicedToday = false))

        composeRule.onNodeWithText("1 day").assertExists()
        composeRule.onNodeWithText("Practice today to keep your streak alive.").assertExists()
    }
}
