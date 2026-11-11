package com.wortart.app.journey

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.MainActivity
import com.wortart.app.WortArtApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One walk through the real app, the way a learner uses it: welcome, practice a flashcard,
 * look at the statistics, search a word, open it, make it a favorite and find it in the profile.
 *
 * It runs against the real database and screens, so it needs an emulator or phone. It resets
 * the local data first, so it can be repeated.
 */
@RunWith(AndroidJUnit4::class)
class UserJourneyTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<WortArtApp>()

        // A fresh start: no progress, no favorites, vocabulary loaded, welcome screen not seen yet.
        runBlocking { app.localDataResetter.reset() }
        app.getSharedPreferences("wortart_onboarding", Context.MODE_PRIVATE).edit().clear().commit()

        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 20_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun aLearnerPracticesSearchesAndSavesAFavorite() {
        // 1. Welcome screen
        waitForText("Get Started")
        composeRule.onNodeWithText("Get Started").performClick()

        // 2. Home
        waitForText("Quick Access")

        // 3. One flashcard: mark an answer, submit it, see the result, go back
        composeRule.onNodeWithText("Flashcards").performScrollTo().performClick()
        waitForText("Submit")
        composeRule.onAllNodes(isSelectable())[0].performClick()
        composeRule.onNodeWithText("Submit").assertIsEnabled().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Finish").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Back").performClick()

        // 4. Statistics count the answered card
        waitForText("Quick Access")
        composeRule.onNodeWithText("Stats").performClick()
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("statsCardsPlayed").assertTextEquals("1") }.isSuccess
        }

        // 5. Explore: search a word and open it
        composeRule.onNodeWithText("Explore").performClick()
        waitForText("Search words…")
        composeRule.onNodeWithTag("searchField").performTextInput("gehen")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("gehen") and hasClickAction() and !hasSetTextAction())
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("gehen") and hasClickAction() and !hasSetTextAction()).performClick()

        // 6. Word details: make it a favorite
        waitForText("Word Details")
        composeRule.onNodeWithText("Add to Favorites").performClick()
        waitForText("Remove from Favorites")
        composeRule.onNodeWithContentDescription("Back").performClick()

        // 7. Profile: the favorites row counts it (Explore keeps its search text, so wait for its categories)
        waitForText("Categories")
        composeRule.onNodeWithText("Profile").performClick()
        waitForText("Favorites (1)")
    }
}
