package com.wortart.app.ui.flashcard

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
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.domain.question.ChoiceQuestionBuilder
import com.wortart.app.ui.components.ChoiceQuestionView
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChoiceFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun entity(
        id: String,
        word: String,
        category: String,
        auxiliary: String? = null,
        partizip2: String? = null,
        prefix: String? = null,
    ) = VocabEntity(
        id = id,
        word = word,
        category = category,
        level = "A1",
        topic = "General",
        translationEn = "english-$word",
        translationBn = "bangla-$word",
        exampleDe = "Beispiel $word",
        exampleEn = "Example $word",
        exampleBn = "উদাহরণ $word",
        auxiliary = auxiliary,
        partizip2 = partizip2,
        prefix = prefix,
    )

    private val gehen = entity("iv1", "gehen", WordCategories.IRREGULAR_VERB, "sein", "gegangen")
    private val einkaufen = entity("sv1", "einkaufen", WordCategories.SEPARABLE_VERB, "haben", prefix = "ein")
    private val schnell = entity("a1", "schnell", WordCategories.ADJECTIVE)

    @Test
    fun correctAnswerShowsPositiveFeedbackAndExplanation() {
        val question = ChoiceQuestionBuilder.build(gehen)!!
        composeRule.setContent {
            WortArtTheme {
                var selected by remember { mutableStateOf<String?>(null) }
                ChoiceQuestionView(question, selected, onSelect = { selected = it })
            }
        }

        composeRule.onNodeWithText("haben").assertExists()
        composeRule.onNodeWithText("sein").performClick()

        composeRule.onNodeWithText("✓ Correct!").assertExists()
        composeRule.onNodeWithText("Perfekt: ist gegangen").assertExists()
    }

    @Test
    fun wrongAnswerRevealsTheCorrectOne() {
        val question = ChoiceQuestionBuilder.build(gehen)!!
        composeRule.setContent {
            WortArtTheme {
                var selected by remember { mutableStateOf<String?>(null) }
                ChoiceQuestionView(question, selected, onSelect = { selected = it })
            }
        }

        composeRule.onNodeWithText("haben").performClick()

        composeRule.onNodeWithText("✗ Not quite — the answer is sein").assertExists()
    }

    @Test
    fun answeredQuestionIgnoresFurtherClicks() {
        val question = ChoiceQuestionBuilder.build(gehen)!!
        var clicks = 0
        composeRule.setContent {
            WortArtTheme {
                var selected by remember { mutableStateOf<String?>(null) }
                ChoiceQuestionView(question, selected, onSelect = { clicks++; selected = it })
            }
        }

        composeRule.onNodeWithText("sein").performClick()
        composeRule.onNodeWithText("haben").performClick()

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun deckShowsQuestionOnlyForVerbsAndRemembersAnswers() {
        val verbQuestion = ChoiceQuestionBuilder.build(gehen)!!
        var answeredCorrectly: Boolean? = null

        composeRule.setContent {
            WortArtTheme {
                FlashcardDeck(
                    words = listOf(gehen, schnell),
                    onSpeak = {},
                    onToggleFavorite = {},
                    onAnswered = { _, isCorrect -> answeredCorrectly = isCorrect },
                )
            }
        }

        composeRule.onNodeWithText(verbQuestion.prompt).assertExists()
        composeRule.onNodeWithText("sein").performClick()
        composeRule.runOnIdle { assertEquals(true, answeredCorrectly) }

        // Adjectives have no answer buttons yet.
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(verbQuestion.prompt).assertDoesNotExist()

        // Going back keeps the earlier answer.
        composeRule.onNodeWithText("Previous").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("✓ Correct!").assertExists()
    }

    @Test
    fun separableVerbShowsPrefixOptions() {
        val question = ChoiceQuestionBuilder.build(einkaufen)!!
        composeRule.setContent {
            WortArtTheme {
                FlashcardDeck(words = listOf(einkaufen), onSpeak = {}, onToggleFavorite = {})
            }
        }

        composeRule.onNodeWithText(question.prompt).assertExists()
        question.options.forEach { composeRule.onNodeWithText(it).assertExists() }
    }
}
