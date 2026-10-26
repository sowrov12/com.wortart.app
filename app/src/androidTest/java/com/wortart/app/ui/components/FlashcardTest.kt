package com.wortart.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gehen = VocabEntity(
        id = "iv001",
        word = "gehen",
        category = WordCategories.IRREGULAR_VERB,
        level = CefrLevels.A1,
        topic = "Movement",
        translationEn = "to go; to walk",
        translationBn = "যাওয়া; হাঁটা",
        exampleDe = "Ich gehe jeden Morgen zur Arbeit.",
        exampleEn = "I go to work every morning.",
        exampleBn = "আমি প্রতিদিন সকালে কাজে যাই।",
        auxiliary = "sein",
        praeteritum = "ging",
        partizip2 = "gegangen",
        stammform = "er geht",
    )

    @Test
    fun showsAllCardInformation() {
        composeRule.setContent {
            WortArtTheme {
                FlashcardView(card = gehen, onSpeak = {}, onToggleFavorite = {})
            }
        }

        composeRule.onNodeWithText("gehen").assertIsDisplayed()
        composeRule.onNodeWithText("A1").assertIsDisplayed()
        composeRule.onNodeWithText("Verb | Irregular").assertIsDisplayed()
        composeRule.onNodeWithText("to go; to walk").assertExists()
        composeRule.onNodeWithText("যাওয়া; হাঁটা").assertExists()
        composeRule.onNodeWithText("Ich gehe jeden Morgen zur Arbeit.").assertExists()
        composeRule.onNodeWithText("I go to work every morning.").assertExists()
        composeRule.onNodeWithText("আমি প্রতিদিন সকালে কাজে যাই।").assertExists()
    }

    @Test
    fun audioButtonInvokesCallback() {
        var speakCount = 0
        composeRule.setContent {
            WortArtTheme {
                FlashcardView(card = gehen, onSpeak = { speakCount++ }, onToggleFavorite = {})
            }
        }

        composeRule.onNodeWithContentDescription("Play pronunciation of gehen").performClick()

        composeRule.runOnIdle { assertEquals(1, speakCount) }
    }

    @Test
    fun favoriteButtonTogglesAndReflectsState() {
        val card = mutableStateOf(gehen)
        composeRule.setContent {
            WortArtTheme {
                FlashcardView(
                    card = card.value,
                    onSpeak = {},
                    onToggleFavorite = {
                        card.value = card.value.copy(isFavorite = !card.value.isFavorite)
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        composeRule.onNodeWithContentDescription("Remove from favorites").assertExists()

        composeRule.onNodeWithContentDescription("Remove from favorites").performClick()
        composeRule.onNodeWithContentDescription("Add to favorites").assertExists()
    }

    @Test
    fun choiceContentSlotIsRendered() {
        composeRule.setContent {
            WortArtTheme {
                FlashcardView(
                    card = gehen,
                    onSpeak = {},
                    onToggleFavorite = {},
                    choiceContent = { Text("slot-content") },
                )
            }
        }

        composeRule.onNodeWithText("slot-content").assertExists()
    }

    @Test
    fun grammarTagsForAllCategories() {
        assertEquals("Verb | Regular", gehen.copy(category = WordCategories.REGULAR_VERB).grammarTag())
        assertEquals("Verb | Irregular", gehen.grammarTag())
        assertEquals("Verb | Separable", gehen.copy(category = WordCategories.SEPARABLE_VERB).grammarTag())
        assertEquals("Adjective", gehen.copy(category = WordCategories.ADJECTIVE).grammarTag())
        assertEquals("Adverb", gehen.copy(category = WordCategories.ADVERB, topic = "Time").grammarTag())
        assertEquals("Connector", gehen.copy(category = WordCategories.ADVERB, topic = "Connectors").grammarTag())
    }
}
