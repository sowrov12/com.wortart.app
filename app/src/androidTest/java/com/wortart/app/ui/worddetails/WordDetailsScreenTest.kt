package com.wortart.app.ui.worddetails

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gehen = VocabEntity(
        id = "iv001",
        word = "gehen",
        category = "irregular_verb",
        level = "A1",
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

    private val weil = VocabEntity(
        id = "adv004",
        word = "weil",
        category = "adverb",
        level = "A2",
        topic = "Connectors",
        translationEn = "because",
        translationBn = "কারণ",
        exampleDe = "Ich bleibe zu Hause, weil ich krank bin.",
        exampleEn = "I am staying at home because I am sick.",
        exampleBn = "আমি বাড়িতে থাকছি, কারণ আমি অসুস্থ।",
    )

    private fun show(
        card: VocabEntity?,
        isLoading: Boolean = false,
        isSpeaking: Boolean = false,
        onBack: () -> Unit = {},
        onToggleFavorite: () -> Unit = {},
        onSpeak: (String, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            WortArtTheme {
                WordDetailsScreen(
                    state = WordDetailsUiState(isLoading = isLoading, card = card),
                    isSpeaking = isSpeaking,
                    onBack = onBack,
                    onToggleFavorite = onToggleFavorite,
                    onSpeak = onSpeak,
                )
            }
        }
    }

    @Test
    fun showsTheWordTheMeaningAndTheCategory() {
        show(gehen)

        composeRule.onNodeWithText("Word Details").assertExists()
        composeRule.onNodeWithText("gehen").assertExists()
        composeRule.onNodeWithText("to go / যাওয়া").assertExists()
        composeRule.onNodeWithText("Irregular Verb").assertExists()
        composeRule.onNodeWithText("A1").assertExists()
    }

    @Test
    fun aVerbShowsConjugationPrincipalFormsAndThePerfectTense() {
        show(gehen)

        composeRule.onNodeWithText("Conjugation (Present)").assertExists()
        composeRule.onNodeWithText("gehst").performScrollTo().assertExists()
        composeRule.onNodeWithText("Principal forms").performScrollTo().assertExists()
        composeRule.onNodeWithText("ging").assertExists()
        composeRule.onNodeWithText("gegangen").assertExists()
        composeRule.onNodeWithText("Perfect tense").performScrollTo().assertExists()
        composeRule.onNodeWithText("ist gegangen").assertExists()
    }

    @Test
    fun theExampleSentenceHasAllThreeLanguages() {
        show(gehen)

        composeRule.onNodeWithText("Example Sentence").performScrollTo().assertExists()
        composeRule.onNodeWithText("Ich gehe jeden Morgen zur Arbeit.").assertExists()
        composeRule.onNodeWithText("I go to work every morning.").assertExists()
        composeRule.onNodeWithText("আমি প্রতিদিন সকালে কাজে যাই।").assertExists()
    }

    @Test
    fun aWordWithoutExtraDataSaysSo() {
        show(weil)

        composeRule.onNodeWithText("Connector").assertExists()
        composeRule.onNodeWithText("More details for this word are not available yet.")
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithText("Conjugation (Present)").assertDoesNotExist()
    }

    @Test
    fun theTopHeartAndTheBottomButtonToggleTheFavorite() {
        var toggles = 0
        show(gehen, onToggleFavorite = { toggles++ })

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        composeRule.onNodeWithText("Add to Favorites").performClick()

        composeRule.runOnIdle { assertEquals(2, toggles) }
    }

    @Test
    fun aFavoriteWordOffersToRemoveIt() {
        show(gehen.copy(isFavorite = true))

        composeRule.onNodeWithContentDescription("Remove from favorites").assertExists()
        composeRule.onNodeWithText("Remove from Favorites").assertExists()
    }

    @Test
    fun bothAudioButtonsPlayTheWord() {
        val spoken = mutableListOf<Pair<String, String>>()
        show(gehen, onSpeak = { id, word -> spoken += id to word })

        composeRule.onNodeWithContentDescription("Play pronunciation of gehen").performClick()
        composeRule.onNodeWithText("Play Audio").performClick()

        composeRule.runOnIdle { assertEquals(listOf("iv001" to "gehen", "iv001" to "gehen"), spoken) }
    }

    @Test
    fun backGoesBack() {
        var backs = 0
        show(gehen, onBack = { backs++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun anUnknownWordOffersToGoBack() {
        var backs = 0
        show(card = null, onBack = { backs++ })

        composeRule.onNodeWithText("This word could not be found.").assertExists()
        composeRule.onNodeWithText("Go back").performClick()

        composeRule.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun whileLoadingNothingIsShownYet() {
        show(card = null, isLoading = true)

        composeRule.onNodeWithText("This word could not be found.").assertDoesNotExist()
        composeRule.onNodeWithText("Play Audio").assertDoesNotExist()
    }
}
