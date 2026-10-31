package com.wortart.app.ui.practice

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.question.ChoiceQuestion
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PracticeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun entity(id: String, word: String, category: String) = VocabEntity(
        id = id,
        word = word,
        category = category,
        level = "A2",
        topic = "General",
        translationEn = "to shop; to buy",
        translationBn = "কিনতে যাওয়া",
        exampleDe = "Ich gehe morgen $word.",
        exampleEn = "I am going to shop tomorrow.",
        exampleBn = "আমি আগামীকাল বাজার করতে যাচ্ছি।",
        auxiliary = "haben",
        prefix = "ein",
    )

    private val prefixCard = SessionCard(
        card = entity("sv1", "einkaufen", "separable_verb"),
        question = ChoiceQuestion(
            type = QuestionType.PREFIX,
            prompt = "Select the separable prefix:",
            options = listOf("ein", "ge", "auf", "vor"),
            correctAnswer = "ein",
            explanation = "ein + kaufen",
        ),
    )

    private val auxiliaryCard = SessionCard(
        card = entity("iv1", "fahren", "irregular_verb"),
        question = ChoiceQuestion(
            type = QuestionType.AUXILIARY,
            prompt = "Which auxiliary verb is correct?",
            options = listOf("haben", "sein"),
            correctAnswer = "sein",
            explanation = "Perfekt: ist gefahren",
        ),
    )

    private val meaningCard = SessionCard(
        card = entity("a1", "schnell", "adjective"),
        question = ChoiceQuestion(
            type = QuestionType.MEANING,
            prompt = "What is the correct meaning?",
            options = listOf("slow / ধীর", "fast / দ্রুত", "big / বড়", "beautiful / সুন্দর"),
            correctAnswer = "fast / দ্রুত",
            explanation = "schnell = fast / দ্রুত",
        ),
    )

    private val cards = listOf(prefixCard, auxiliaryCard, meaningCard)

    private fun active(
        index: Int = 0,
        selected: String? = null,
        submitted: Boolean = false,
        correctCount: Int = 0,
    ) = PracticeUiState(
        status = PracticeStatus.ACTIVE,
        cards = cards,
        index = index,
        selected = selected,
        submitted = submitted,
        correctCount = correctCount,
    )

    private fun show(
        state: PracticeUiState,
        speakingCardId: String? = null,
        onBack: () -> Unit = {},
        onSpeak: (String, String) -> Unit = { _, _ -> },
        onToggleFavorite: () -> Unit = {},
        onSelect: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onNext: () -> Unit = {},
        onRestart: () -> Unit = {},
    ) {
        composeRule.setContent {
            WortArtTheme {
                PracticeScreen(
                    state = state,
                    speakingCardId = speakingCardId,
                    onBack = onBack,
                    onSpeak = onSpeak,
                    onToggleFavorite = onToggleFavorite,
                    onSelect = onSelect,
                    onSubmit = onSubmit,
                    onNext = onNext,
                    onRestart = onRestart,
                )
            }
        }
    }

    // ---- The card and the question ----

    @Test
    fun showsProgressCardPromptAndOptions() {
        show(active())

        composeRule.onNodeWithText("Flashcards").assertExists()
        composeRule.onNodeWithText("1 / 3").assertExists()
        composeRule.onNodeWithText("einkaufen").assertExists()
        composeRule.onNodeWithText("Verb | Separable").assertExists()
        composeRule.onNodeWithText("to shop / কিনতে যাওয়া").assertExists()
        composeRule.onNodeWithText("Select the separable prefix:").assertExists()
        listOf("ein", "ge", "auf", "vor").forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun theAuxiliaryQuestionOffersHabenAndSein() {
        show(active(index = 1))

        composeRule.onNodeWithText("2 / 3").assertExists()
        composeRule.onNodeWithText("Which auxiliary verb is correct?").assertExists()
        composeRule.onNodeWithText("haben").assertExists()
        composeRule.onNodeWithText("sein").assertExists()
    }

    @Test
    fun theMeaningQuestionListsMeaningsInBothLanguages() {
        show(active(index = 2))

        composeRule.onNodeWithText("What is the correct meaning?").assertExists()
        listOf("slow / ধীর", "fast / দ্রুত", "big / বড়", "beautiful / সুন্দর").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    // ---- Selecting and submitting ----

    @Test
    fun submitIsDisabledUntilAnOptionIsMarked() {
        show(active())
        composeRule.onNodeWithText("Submit").assertIsNotEnabled()
    }

    @Test
    fun submitIsEnabledOnceAnOptionIsMarked() {
        show(active(selected = "ein"))
        composeRule.onNodeWithText("Submit").assertIsEnabled()
    }

    @Test
    fun tappingAnOptionMarksItAndSubmitSendsTheAnswer() {
        val chosen = mutableListOf<String>()
        var submits = 0
        show(active(selected = "ein"), onSelect = { chosen += it }, onSubmit = { submits++ })

        composeRule.onNodeWithText("auf").performClick()
        composeRule.onNodeWithText("Submit").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("auf"), chosen)
            assertEquals(1, submits)
        }
    }

    // ---- After submitting ----

    @Test
    fun aCorrectAnswerShowsGoodFeedbackAndNext() {
        show(active(selected = "ein", submitted = true, correctCount = 1))

        composeRule.onNodeWithText("✓ Correct!").performScrollTo().assertExists()
        composeRule.onNodeWithText("ein + kaufen").assertExists()
        composeRule.onNodeWithText("Next").assertIsEnabled()
        composeRule.onNodeWithText("Submit").assertDoesNotExist()
    }

    @Test
    fun aWrongAnswerNamesTheCorrectOne() {
        show(active(selected = "auf", submitted = true))

        composeRule.onNodeWithText("✗ Not quite — the answer is ein").performScrollTo().assertExists()
    }

    @Test
    fun optionsAreLockedAfterSubmitting() {
        val chosen = mutableListOf<String>()
        show(active(selected = "ein", submitted = true, correctCount = 1), onSelect = { chosen += it })

        composeRule.onNodeWithText("auf").performClick()

        composeRule.runOnIdle { assertEquals(emptyList<String>(), chosen) }
    }

    @Test
    fun nextMovesOn() {
        var nexts = 0
        show(active(selected = "ein", submitted = true, correctCount = 1), onNext = { nexts++ })

        composeRule.onNodeWithText("Next").performClick()

        composeRule.runOnIdle { assertEquals(1, nexts) }
    }

    @Test
    fun theLastCardEndsWithFinish() {
        show(active(index = 2, selected = "fast / দ্রুত", submitted = true, correctCount = 1))

        composeRule.onNodeWithText("Finish").assertExists()
        composeRule.onNodeWithText("Next").assertDoesNotExist()
    }

    // ---- Top bar and audio ----

    @Test
    fun backFavoriteAndAudioWork() {
        var backs = 0
        var favorites = 0
        val spoken = mutableListOf<Pair<String, String>>()
        show(
            active(),
            onBack = { backs++ },
            onToggleFavorite = { favorites++ },
            onSpeak = { id, word -> spoken += id to word },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        composeRule.onNodeWithContentDescription("Play pronunciation of einkaufen").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backs)
            assertEquals(1, favorites)
            assertEquals(listOf("sv1" to "einkaufen"), spoken)
        }
    }

    // ---- Other states ----

    @Test
    fun anEmptySessionOffersToTryAgain() {
        var restarts = 0
        show(PracticeUiState(status = PracticeStatus.EMPTY), onRestart = { restarts++ })

        composeRule.onNodeWithText("No flashcards yet").assertExists()
        composeRule.onNodeWithText("Try again").performClick()

        composeRule.runOnIdle { assertEquals(1, restarts) }
    }

    @Test
    fun aFinishedSessionShowsTheResultAndTheNextSteps() {
        var restarts = 0
        var backs = 0
        show(
            PracticeUiState(status = PracticeStatus.FINISHED, cards = cards, correctCount = 2, index = 2),
            onRestart = { restarts++ },
            onBack = { backs++ },
        )

        composeRule.onNodeWithText("Session complete").assertExists()
        composeRule.onNodeWithText("2 / 3 correct").assertExists()
        composeRule.onNodeWithText("67% success rate").assertExists()

        composeRule.onNodeWithText("Practice again").performClick()
        composeRule.onNodeWithText("Back to Home").performClick()

        composeRule.runOnIdle {
            assertEquals(1, restarts)
            assertEquals(1, backs)
        }
    }
}
