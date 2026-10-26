package com.wortart.app.ui.explore

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val kochen = VocabEntity(
        id = "rv3",
        word = "kochen",
        category = "regular_verb",
        level = "A1",
        topic = "Food",
        translationEn = "to cook",
        translationBn = "রান্না করা",
        exampleDe = "Ich koche.",
        exampleEn = "I cook.",
        exampleBn = "আমি রান্না করি।",
        auxiliary = "haben",
    )

    private val topicsState = ExploreUiState(
        destination = ExploreDestination.Topics,
        topics = listOf(
            TopicSummary("Food", 2),
            TopicSummary("Career", 1),
            TopicSummary("Health", 3),
            TopicSummary("Travel", 0),
        ),
    )

    private fun show(
        state: ExploreUiState,
        onSelectLevel: (String?) -> Unit = {},
        onOpenTopic: (String?) -> Unit = {},
        onCloseTopic: () -> Unit = {},
        onToggleFavorite: (VocabEntity) -> Unit = {},
    ) {
        composeRule.setContent {
            WortArtTheme {
                ExploreScreen(
                    state = state,
                    onSelectLevel = onSelectLevel,
                    onOpenTopic = onOpenTopic,
                    onCloseTopic = onCloseTopic,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }

    @Test
    fun gridShowsTopicsWithWordCounts() {
        show(topicsState)

        composeRule.onNodeWithText("Explore").assertExists()
        composeRule.onNodeWithText("All words").assertExists()
        composeRule.onNodeWithText("6 words").assertExists()
        composeRule.onNodeWithText("Food").assertExists()
        composeRule.onNodeWithText("2 words").assertExists()
        composeRule.onNodeWithText("Career").assertExists()
        composeRule.onNodeWithText("1 word").assertExists()
        composeRule.onNodeWithText("Health").assertExists()
        composeRule.onNodeWithText("3 words").assertExists()
    }

    @Test
    fun levelChipsAreShownAndReportTheTappedLevel() {
        val selected = mutableListOf<String?>()
        show(topicsState, onSelectLevel = { selected += it })

        listOf("All", "A1", "A2", "B1", "B2").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }

        composeRule.onNodeWithText("B1").performClick()
        composeRule.onNodeWithText("All").performClick()

        composeRule.runOnIdle { assertEquals(listOf<String?>("B1", null), selected) }
    }

    @Test
    fun tappingATopicOrAllWordsOpensIt() {
        val opened = mutableListOf<String?>()
        show(topicsState, onOpenTopic = { opened += it })

        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("All words").performClick()

        composeRule.runOnIdle { assertEquals(listOf<String?>("Food", null), opened) }
    }

    @Test
    fun topicWithoutWordsCannotBeOpened() {
        val opened = mutableListOf<String?>()
        show(topicsState, onOpenTopic = { opened += it })

        composeRule.onNodeWithText("Travel").performClick()

        composeRule.runOnIdle { assertEquals(emptyList<String?>(), opened) }
    }

    @Test
    fun wordListShowsWordsAndSupportsBackAndFavorite() {
        var backCount = 0
        var favorited: VocabEntity? = null
        show(
            state = ExploreUiState(
                destination = ExploreDestination.Words("Food"),
                topics = topicsState.topics,
                words = listOf(kochen),
            ),
            onCloseTopic = { backCount++ },
            onToggleFavorite = { favorited = it },
        )

        composeRule.onNodeWithText("Food").assertExists()
        composeRule.onNodeWithText("1 word").assertExists()
        composeRule.onNodeWithText("kochen").assertExists()
        composeRule.onNodeWithText("Verb | Regular").assertExists()
        composeRule.onNodeWithText("to cook").assertExists()
        composeRule.onNodeWithText("রান্না করা").assertExists()
        // Level chip "A1" and the badge on the row both show "A1".
        assertEquals(2, composeRule.onAllNodesWithText("A1").fetchSemanticsNodes().size)

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        composeRule.onNodeWithText("‹ Topics").performClick()

        composeRule.runOnIdle {
            assertEquals(kochen, favorited)
            assertEquals(1, backCount)
        }
    }

    @Test
    fun emptyWordListShowsAMessage() {
        show(ExploreUiState(destination = ExploreDestination.Words("Food"), topics = topicsState.topics))

        composeRule.onNodeWithText("No words found.").assertExists()
    }
}
