package com.wortart.app.ui.stats

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.domain.stats.Achievements
import com.wortart.app.domain.stats.DayActivity
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Epoch day 20000 is a Friday, so the week runs Sat, Sun, Mon, Tue, Wed, Thu, Fri.
    private val week = (19_994L..20_000L).mapIndexed { index, day -> DayActivity(day, if (index == 6) 12 else index) }

    private val active = StatsSummary(
        germanScore = 175,
        streakDays = 4,
        practicedToday = true,
        cardsPlayed = 48,
        correctAnswers = 44,
        successPercent = 92,
        practiceTimeSeconds = 5040,
        weeklyActivity = week,
    ).let { it.copy(achievements = Achievements.from(it)) }

    private fun show(summary: StatsSummary, onStartPractice: () -> Unit = {}) {
        composeRule.setContent {
            WortArtTheme { StatsScreen(summary = summary, onStartPractice = onStartPractice) }
        }
    }

    @Test
    fun showsTheRingTheTilesAndTheStreak() {
        show(active)

        composeRule.onNodeWithText("Statistics").assertExists()
        composeRule.onNodeWithText("92%").assertExists()
        composeRule.onNodeWithText("Success Rate").assertExists()

        composeRule.onNodeWithTag("statsGermanScore").assertTextEquals("175")
        composeRule.onNodeWithTag("statsCardsPlayed").assertTextEquals("48")
        composeRule.onNodeWithTag("statsPracticeTime").assertTextEquals("1 h 24 min")
        composeRule.onNodeWithTag("statsCorrectAnswers").assertTextEquals("44")

        composeRule.onNodeWithText("Daily Streak").performScrollTo().assertExists()
        composeRule.onNodeWithText("4 days").assertExists()
        composeRule.onNodeWithText("You practiced today. Great job!").assertExists()
    }

    @Test
    fun theRingHasADescriptionForScreenReaders() {
        show(active)

        composeRule.onNodeWithContentDescription("Success rate 92 percent").assertExists()
    }

    @Test
    fun everyDayOfTheWeekHasADescription() {
        show(active)

        composeRule.onNodeWithContentDescription("Fri: 12 cards").performScrollTo().assertExists()
        composeRule.onNodeWithContentDescription("Thu: 5 cards").assertExists()
        composeRule.onNodeWithContentDescription("Sat: 0 cards").assertExists()
    }

    @Test
    fun earnedAchievementsAreMarkedAndTheRestAreLocked() {
        show(active)

        composeRule.onNodeWithText("Achievements").performScrollTo().assertExists()
        composeRule.onNodeWithContentDescription("10 Cards, Answer 10 flashcards, earned").assertExists()
        composeRule.onNodeWithContentDescription("100 Cards, Answer 100 flashcards, not earned yet").assertExists()
        composeRule.onNodeWithContentDescription("1 Hour, Practice for one hour, earned").assertExists()
    }

    @Test
    fun aNewLearnerIsInvitedToStartPracticing() {
        var starts = 0
        show(StatsSummary(achievements = Achievements.from(StatsSummary())), onStartPractice = { starts++ })

        composeRule.onNodeWithText("No practice yet").assertExists()
        composeRule.onNodeWithText("Answer your first flashcard to see your progress here.").assertExists()
        composeRule.onNodeWithText("0%").assertExists()
        composeRule.onNodeWithText("Start practicing").performClick()

        composeRule.runOnIdle { assertEquals(1, starts) }
    }

    @Test
    fun theStartCardDisappearsOnceCardsWereAnswered() {
        show(active)

        composeRule.onNodeWithText("No practice yet").assertDoesNotExist()
        composeRule.onNodeWithText("Start practicing").assertDoesNotExist()
    }

    @Test
    fun aStreakAtRiskAsksForPracticeToday() {
        show(active.copy(streakDays = 1, practicedToday = false))

        composeRule.onNodeWithText("1 day").performScrollTo().assertExists()
        composeRule.onNodeWithText("Practice today to keep your streak alive.").assertExists()
    }

    @Test
    fun thePointsFormulaIsExplained() {
        show(active)

        composeRule.onNodeWithText("Score: 10 points per correct answer, 20 per day of your streak.")
            .performScrollTo()
            .assertExists()
    }
}
