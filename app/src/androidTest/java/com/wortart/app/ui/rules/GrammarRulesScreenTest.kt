package com.wortart.app.ui.rules

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.rules.grammarRuleSections
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrammarRulesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gehen = VocabEntity(
        id = "iv1",
        word = "gehen",
        category = "irregular_verb",
        level = "A1",
        topic = "Movement",
        translationEn = "to go",
        translationBn = "যাওয়া",
        exampleDe = "Ich gehe.",
        exampleEn = "I go.",
        exampleBn = "আমি যাই।",
        auxiliary = "sein",
        praeteritum = "ging",
        partizip2 = "gegangen",
    )

    private fun showRules(irregular: List<VocabEntity> = emptyList()) {
        composeRule.setContent {
            WortArtTheme {
                GrammarRulesScreen(sections = grammarRuleSections(irregular, emptyList()))
            }
        }
    }

    @Test
    fun allSectionsAreListedAndCollapsedAtFirst() {
        showRules()

        composeRule.onNodeWithText("Grammar Rules").assertExists()
        composeRule.onNodeWithText("Separable verbs").assertExists()
        composeRule.onNodeWithText("Trennbare Verben").assertExists()
        composeRule.onNodeWithText("Inseparable prefixes").assertExists()
        composeRule.onNodeWithText("Present tense & Perfekt").assertExists()
        composeRule.onNodeWithText("Irregular verbs").assertExists()
        composeRule.onNodeWithText("Comparative & superlative").assertExists()

        composeRule.onNodeWithText("Common separable prefixes").assertDoesNotExist()
    }

    @Test
    fun tappingASectionShowsItsTablesAndExamples() {
        showRules()

        composeRule.onNodeWithText("Separable verbs").performClick()

        composeRule.onNodeWithText("Common separable prefixes").assertExists()
        composeRule.onNodeWithText("Present tense: einkaufen (to shop)").assertExists()
        composeRule.onNodeWithText("kaufe … ein").assertExists()
        composeRule.onNodeWithText("Ich kaufe heute im Supermarkt ein.").assertExists()
        composeRule.onNodeWithText("আমি আজ সুপারমার্কেটে কেনাকাটা করি।").assertExists()
    }

    @Test
    fun tappingTheOpenSectionCollapsesIt() {
        showRules()

        composeRule.onNodeWithText("Separable verbs").performClick()
        composeRule.onNodeWithText("Common separable prefixes").assertExists()

        composeRule.onNodeWithText("Separable verbs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Common separable prefixes").assertDoesNotExist()
    }

    @Test
    fun openingAnotherSectionClosesTheFirstOne() {
        showRules()

        composeRule.onNodeWithText("Inseparable prefixes").performClick()
        composeRule.onNodeWithText("Common inseparable prefixes").assertExists()

        // "Separable verbs" is above the open card, so it is still on screen.
        composeRule.onNodeWithText("Separable verbs").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Common inseparable prefixes").assertDoesNotExist()
        composeRule.onNodeWithText("Common separable prefixes").assertExists()
    }

    @Test
    fun irregularVerbTableShowsTheLearnersVocabulary() {
        showRules(irregular = listOf(gehen))

        composeRule.onNodeWithText("Irregular verbs").performClick()

        composeRule.onNodeWithText("Irregular verbs in your vocabulary").assertExists()
        composeRule.onNodeWithText("gehen").assertExists()
        composeRule.onNodeWithText("ging").assertExists()
        composeRule.onNodeWithText("gegangen").assertExists()
    }
}
