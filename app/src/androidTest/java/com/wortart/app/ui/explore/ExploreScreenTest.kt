package com.wortart.app.ui.explore

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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

    private fun word(id: String, word: String, topic: String, en: String, bn: String, isFavorite: Boolean = false) =
        VocabEntity(
            id = id,
            word = word,
            category = "regular_verb",
            level = "A1",
            topic = topic,
            translationEn = en,
            translationBn = bn,
            exampleDe = "de",
            exampleEn = "en",
            exampleBn = "bn",
            auxiliary = "haben",
            isFavorite = isFavorite,
        )

    private val kochen = word("rv3", "kochen", "Food", "to cook", "রান্না করা")
    private val arbeiten = word("rv5", "arbeiten", "Career", "to work", "কাজ করা")

    private val topics = listOf(
        TopicSummary("Food", 2),
        TopicSummary("Career", 1),
        TopicSummary("Travel", 0),
    )

    private fun state(
        words: List<VocabEntity> = listOf(kochen, arbeiten),
        level: String? = null,
        topic: String? = null,
        favoritesOnly: Boolean = false,
        query: String = "",
        isLoaded: Boolean = true,
    ) = ExploreUiState(
        level = level,
        topic = topic,
        favoritesOnly = favoritesOnly,
        query = query,
        topics = topics,
        words = words,
        isLoaded = isLoaded,
    )

    private fun show(
        state: ExploreUiState,
        query: String = state.query,
        onQueryChange: (String) -> Unit = {},
        onSelectLevel: (String?) -> Unit = {},
        onSelectTopic: (String?) -> Unit = {},
        onToggleFavoritesOnly: () -> Unit = {},
        onClearFilters: () -> Unit = {},
        onOpenWord: (String) -> Unit = {},
        onToggleFavorite: (VocabEntity) -> Unit = {},
    ) {
        composeRule.setContent {
            WortArtTheme {
                ExploreScreen(
                    state = state,
                    query = query,
                    onQueryChange = onQueryChange,
                    onSelectLevel = onSelectLevel,
                    onSelectTopic = onSelectTopic,
                    onToggleFavoritesOnly = onToggleFavoritesOnly,
                    onClearFilters = onClearFilters,
                    onOpenWord = onOpenWord,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }

    // ---- Layout ----

    @Test
    fun showsTheSearchLevelChipsCategoriesAndWords() {
        show(state())

        composeRule.onNodeWithText("Explore").assertExists()
        composeRule.onNodeWithText("Search words…").assertExists()
        listOf("All", "A2", "B1", "B2").forEach { composeRule.onNodeWithText(it).assertExists() }

        composeRule.onNodeWithText("Categories").assertExists()
        composeRule.onNodeWithText("Food").assertExists()
        composeRule.onNodeWithText("(2)").assertExists()
        composeRule.onNodeWithText("Career").assertExists()
        composeRule.onNodeWithText("(1)").assertExists()

        composeRule.onNodeWithText("Words (2)").assertExists()
        composeRule.onNodeWithText("kochen").assertExists()
        composeRule.onNodeWithText("to cook | রান্না করা").assertExists()
        composeRule.onNodeWithText("arbeiten").assertExists()
    }

    @Test
    fun withFiltersTheListIsCalledResults() {
        show(state(level = "A1", words = listOf(kochen)))

        composeRule.onNodeWithText("Results (1)").assertExists()
        composeRule.onNodeWithText("Words (1)").assertDoesNotExist()
    }

    // ---- Search ----

    @Test
    fun typingInTheSearchFieldIsReported() {
        val typed = mutableListOf<String>()
        show(state(), onQueryChange = { typed += it })

        composeRule.onNodeWithTag("searchField").performTextInput("kochen")

        composeRule.runOnIdle { assertEquals(listOf("kochen"), typed) }
    }

    @Test
    fun theClearButtonOnlyAppearsWithTextAndEmptiesTheField() {
        val typed = mutableListOf<String>()
        show(state(query = "ko"), query = "ko", onQueryChange = { typed += it })

        composeRule.onNodeWithContentDescription("Clear search").performClick()

        composeRule.runOnIdle { assertEquals(listOf(""), typed) }
    }

    @Test
    fun withoutTextThereIsNoClearButton() {
        show(state(), query = "")

        composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    // ---- Filters ----

    @Test
    fun levelChipsReportTheTappedLevel() {
        val picked = mutableListOf<String?>()
        show(state(), onSelectLevel = { picked += it })

        composeRule.onNodeWithText("B1").performClick()
        composeRule.onNodeWithText("All").performClick()

        composeRule.runOnIdle { assertEquals(listOf<String?>("B1", null), picked) }
    }

    @Test
    fun categoryCardsReportTheTappedCategory() {
        val picked = mutableListOf<String?>()
        show(state(), onSelectTopic = { picked += it })

        composeRule.onNodeWithText("Food").performClick()

        composeRule.runOnIdle { assertEquals(listOf<String?>("Food"), picked) }
    }

    @Test
    fun aCategoryWithoutWordsCannotBePicked() {
        val picked = mutableListOf<String?>()
        show(state(), onSelectTopic = { picked += it })

        composeRule.onNodeWithText("Travel").performClick()

        composeRule.runOnIdle { assertEquals(emptyList<String?>(), picked) }
    }

    @Test
    fun theSelectedCategoryCanBePickedAgainToClearIt() {
        val picked = mutableListOf<String?>()
        show(state(topic = "Travel"), onSelectTopic = { picked += it })

        composeRule.onNodeWithText("Travel").performClick()

        composeRule.runOnIdle { assertEquals(listOf<String?>("Travel"), picked) }
    }

    @Test
    fun theHeartButtonTogglesFavoritesOnly() {
        var toggles = 0
        show(state(), onToggleFavoritesOnly = { toggles++ })

        composeRule.onNodeWithContentDescription("Show favorites only").performClick()

        composeRule.runOnIdle { assertEquals(1, toggles) }
    }

    @Test
    fun clearFiltersAppearsWithFiltersAndResetsThem() {
        var clears = 0
        show(state(level = "A1"), onClearFilters = { clears++ })

        composeRule.onNodeWithText("Clear filters").performClick()

        composeRule.runOnIdle { assertEquals(1, clears) }
    }

    @Test
    fun withoutFiltersThereIsNothingToClear() {
        show(state())

        composeRule.onNodeWithText("Clear filters").assertDoesNotExist()
    }

    // ---- Empty states ----

    @Test
    fun noMatchesShowAMessageAndOfferToClearTheFilters() {
        var clears = 0
        show(state(words = emptyList(), query = "zzz"), query = "zzz", onClearFilters = { clears++ })

        composeRule.onNodeWithText("No words found").performScrollTo().assertExists()
        composeRule.onNodeWithText("Try another search or remove a filter.").assertExists()
        composeRule.onNodeWithText("Clear filters").performClick()

        composeRule.runOnIdle { assertEquals(1, clears) }
    }

    @Test
    fun anEmptyVocabularyExplainsWhy() {
        show(state(words = emptyList()))

        composeRule.onNodeWithText("The vocabulary is still loading.").performScrollTo().assertExists()
    }

    @Test
    fun nothingIsShownAsEmptyBeforeTheFirstResult() {
        show(state(words = emptyList(), isLoaded = false))

        composeRule.onNodeWithText("No words found").assertDoesNotExist()
    }

    // ---- Words ----

    @Test
    fun tappingAWordOpensItsDetails() {
        val opened = mutableListOf<String>()
        show(state(), onOpenWord = { opened += it })

        composeRule.onNodeWithText("kochen").performClick()

        composeRule.runOnIdle { assertEquals(listOf("rv3"), opened) }
    }

    @Test
    fun theHeartOfAWordTogglesItsFavorite() {
        var toggled: VocabEntity? = null
        show(state(words = listOf(kochen)), onToggleFavorite = { toggled = it })

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()

        composeRule.runOnIdle { assertEquals(kochen, toggled) }
    }
}
