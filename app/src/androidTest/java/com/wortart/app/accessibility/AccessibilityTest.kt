package com.wortart.app.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.question.ChoiceQuestion
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.domain.stats.Achievements
import com.wortart.app.domain.stats.DayActivity
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.explore.ExploreScreen
import com.wortart.app.ui.explore.ExploreUiState
import com.wortart.app.ui.explore.TopicSummary
import com.wortart.app.ui.home.HomeScreen
import com.wortart.app.ui.practice.PracticeScreen
import com.wortart.app.ui.practice.PracticeStatus
import com.wortart.app.ui.practice.PracticeUiState
import com.wortart.app.ui.practice.SessionCard
import com.wortart.app.ui.stats.StatsScreen
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility checks: the screens must stay usable when the system font is set to the largest
 * size, and the important controls must be big enough to hit.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Shows [content] as if the person had set the system font size to [fontScale] times normal. */
    private fun showScaled(fontScale: Float = 2f, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                WortArtTheme { content() }
            }
        }
    }

    private val summary = StatsSummary(
        germanScore = 782,
        streakDays = 6,
        practicedToday = true,
        cardsPlayed = 48,
        correctAnswers = 44,
        successPercent = 92,
        practiceTimeSeconds = 5040,
        weeklyActivity = (19_994L..20_000L).map { DayActivity(it, 3) },
    ).let { it.copy(achievements = Achievements.from(it)) }

    private val card = VocabEntity(
        id = "sv1",
        word = "einkaufen",
        category = "separable_verb",
        level = "A2",
        topic = "Food",
        translationEn = "to shop",
        translationBn = "কিনতে যাওয়া",
        exampleDe = "Ich gehe morgen einkaufen.",
        exampleEn = "I am going to shop tomorrow.",
        exampleBn = "আমি আগামীকাল বাজার করতে যাচ্ছি।",
        auxiliary = "haben",
        prefix = "ein",
    )

    private val question = ChoiceQuestion(
        type = QuestionType.PREFIX,
        prompt = "Select the separable prefix:",
        options = listOf("ein", "ge", "auf", "vor"),
        correctAnswer = "ein",
        explanation = "ein + kaufen",
    )

    // ---- Largest system font ----

    @Test
    fun homeStaysUsableAtTheLargestFont() {
        var opened = 0
        showScaled {
            HomeScreen(
                userName = "Sowrov",
                isMember = true,
                summary = summary,
                onOpenFlashcards = { opened++ },
                onOpenExplore = {},
                onOpenRules = {},
                onOpenStats = {},
                onOpenSearch = {},
                onOpenProfile = {},
            )
        }

        composeRule.onNodeWithText("Hallo, Sowrov!").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Access").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Flashcards").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun theFlashcardSessionStaysUsableAtTheLargestFont() {
        var submits = 0
        showScaled {
            PracticeScreen(
                state = PracticeUiState(
                    status = PracticeStatus.ACTIVE,
                    cards = listOf(SessionCard(card, question)),
                    selected = "ein",
                ),
                speakingCardId = null,
                onBack = {},
                onSpeak = { _, _ -> },
                onToggleFavorite = {},
                onSelect = {},
                onSubmit = { submits++ },
                onNext = {},
                onRestart = {},
            )
        }

        composeRule.onNodeWithText("einkaufen").assertIsDisplayed()
        composeRule.onNodeWithText("Submit").assertIsEnabled().performClick()

        composeRule.runOnIdle { assertEquals(1, submits) }
    }

    @Test
    fun everyAnswerOptionCanStillBeReachedAtTheLargestFont() {
        showScaled {
            PracticeScreen(
                state = PracticeUiState(
                    status = PracticeStatus.ACTIVE,
                    cards = listOf(SessionCard(card, question)),
                ),
                speakingCardId = null,
                onBack = {},
                onSpeak = { _, _ -> },
                onToggleFavorite = {},
                onSelect = {},
                onSubmit = {},
                onNext = {},
                onRestart = {},
            )
        }

        question.options.forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun exploreStaysUsableAtTheLargestFont() {
        val opened = mutableListOf<String>()
        showScaled {
            ExploreScreen(
                state = ExploreUiState(
                    topics = listOf(TopicSummary("Food", 2), TopicSummary("Career", 1)),
                    words = listOf(card),
                    isLoaded = true,
                ),
                query = "",
                onQueryChange = {},
                onSelectLevel = {},
                onSelectTopic = {},
                onToggleFavoritesOnly = {},
                onClearFilters = {},
                onOpenWord = { opened += it },
                onToggleFavorite = {},
            )
        }

        composeRule.onNodeWithText("Explore").assertIsDisplayed()
        composeRule.onNodeWithText("einkaufen").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(listOf("sv1"), opened) }
    }

    @Test
    fun statisticsStayUsableAtTheLargestFont() {
        showScaled { StatsScreen(summary = summary, onStartPractice = {}) }

        composeRule.onNodeWithText("Statistics").assertIsDisplayed()
        composeRule.onNodeWithText("Success Rate").assertIsDisplayed()
        composeRule.onNodeWithText("Achievements").performScrollTo().assertIsDisplayed()
    }

    // ---- Touch targets ----

    @Test
    fun theBackAndHeartButtonsAreBigEnough() {
        composeRule.setContent {
            WortArtTheme {
                PracticeScreen(
                    state = PracticeUiState(
                        status = PracticeStatus.ACTIVE,
                        cards = listOf(SessionCard(card, question)),
                    ),
                    speakingCardId = null,
                    onBack = {},
                    onSpeak = { _, _ -> },
                    onToggleFavorite = {},
                    onSelect = {},
                    onSubmit = {},
                    onNext = {},
                    onRestart = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Add to favorites").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Play pronunciation of einkaufen").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theMainButtonsAreBigEnough() {
        composeRule.setContent {
            WortArtTheme {
                PracticeScreen(
                    state = PracticeUiState(
                        status = PracticeStatus.ACTIVE,
                        cards = listOf(SessionCard(card, question)),
                        selected = "ein",
                    ),
                    speakingCardId = null,
                    onBack = {},
                    onSpeak = { _, _ -> },
                    onToggleFavorite = {},
                    onSelect = {},
                    onSubmit = {},
                    onNext = {},
                    onRestart = {},
                )
            }
        }

        composeRule.onNodeWithText("Submit").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("ein").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theFilterChipsHaveAFullSizeTouchTarget() {
        composeRule.setContent {
            WortArtTheme {
                ExploreScreen(
                    state = ExploreUiState(isLoaded = true),
                    query = "",
                    onQueryChange = {},
                    onSelectLevel = {},
                    onSelectTopic = {},
                    onToggleFavoritesOnly = {},
                    onClearFilters = {},
                    onOpenWord = {},
                    onToggleFavorite = {},
                )
            }
        }

        composeRule.onNodeWithText("A1").assertTouchHeightIsEqualTo(48.dp)
        composeRule.onNodeWithText("All").assertTouchHeightIsEqualTo(48.dp)
    }
}
