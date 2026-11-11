package com.wortart.app.ui.explore

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private class FakeRepository(words: List<VocabEntity>) : VocabRepository {
        val all = MutableStateFlow(words)
        var lastFavoriteCall: Pair<String, Boolean>? = null

        override fun observeWords(filter: VocabFilter) = all.map { list ->
            list.filter {
                (filter.level == null || it.level == filter.level) &&
                    (filter.category == null || it.category == filter.category) &&
                    (filter.topic == null || it.topic == filter.topic) &&
                    (!filter.favoritesOnly || it.isFavorite) &&
                    matchesQuery(it, filter.query.trim())
            }
        }

        private fun matchesQuery(card: VocabEntity, query: String): Boolean =
            query.isEmpty() || listOf(card.word, card.translationEn, card.translationBn)
                .any { it.contains(query, ignoreCase = true) }

        override fun observeFavorites() = all.map { list -> list.filter { it.isFavorite } }

        override fun observeTopics() = all.map { list -> list.map { it.topic }.distinct().sorted() }

        override suspend fun getWord(id: String) = all.value.firstOrNull { it.id == id }

        override suspend fun wordCount() = all.value.size

        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            lastFavoriteCall = id to isFavorite
            all.value = all.value.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }

        override suspend fun getDistractors(card: VocabEntity, count: Int) = emptyList<VocabEntity>()
    }

    private fun word(id: String, topic: String, level: String, en: String, bn: String) = VocabEntity(
        id = id,
        word = id,
        category = "regular_verb",
        level = level,
        topic = topic,
        translationEn = en,
        translationBn = bn,
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = "haben",
    )

    private val gehen = word("gehen", "Movement", "A1", "to go", "যাওয়া")
    private val kochen = word("kochen", "Food", "A1", "to cook", "রান্না করা")
    private val essen = word("essen", "Food", "A1", "to eat", "খাওয়া")
    private val arbeiten = word("arbeiten", "Career", "B1", "to work", "কাজ করা")
    private val gesund = word("gesund", "Health", "A2", "healthy", "সুস্থ")
    private val krank = word("krank", "Health", "A2", "sick", "অসুস্থ")

    private val repository = FakeRepository(listOf(gehen, kochen, essen, arbeiten, gesund, krank))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Creates the ViewModel and keeps its state flow active while [block] runs. */
    private fun runWithViewModel(block: suspend TestScope.(ExploreViewModel) -> Unit) = runTest {
        val viewModel = ExploreViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        this.block(viewModel)
    }

    /** Lets the search debounce delay pass. */
    private fun TestScope.letSearchSettle() {
        advanceTimeBy(ExploreViewModel.SEARCH_DEBOUNCE_MS + 50)
        runCurrent()
    }

    private fun ExploreUiState.ids() = words.map { it.id }

    // ---- Start ----

    @Test
    fun startsWithAllWordsAndNoFilters() = runWithViewModel { viewModel ->
        val state = viewModel.uiState.value

        assertTrue(state.isLoaded)
        assertFalse(state.hasFilters)
        assertEquals(6, state.words.size)
    }

    @Test
    fun categoriesAreSortedFeaturedFirstAndCounted() = runWithViewModel { viewModel ->
        val topics = viewModel.uiState.value.topics

        assertEquals(listOf("Food", "Career", "Health", "Movement"), topics.map { it.name })
        assertEquals(listOf(2, 1, 2, 1), topics.map { it.wordCount })
    }

    // ---- Level ----

    @Test
    fun theLevelFilterKeepsMatchingWordsAndUpdatesTheCounts() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A1")

        val state = viewModel.uiState.value
        assertEquals("A1", state.level)
        assertTrue(state.hasFilters)
        assertEquals(listOf("gehen", "kochen", "essen"), state.ids())
        assertEquals(
            mapOf("Food" to 2, "Career" to 0, "Health" to 0, "Movement" to 1),
            state.topics.associate { it.name to it.wordCount },
        )
    }

    @Test
    fun selectingTheActiveLevelAgainClearsIt() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A1")
        viewModel.selectLevel("A1")
        assertNull(viewModel.uiState.value.level)

        viewModel.selectLevel("B1")
        viewModel.selectLevel(null)
        assertNull(viewModel.uiState.value.level)
    }

    // ---- Category ----

    @Test
    fun aCategoryFiltersTheWordsButNotTheCategoryCounts() = runWithViewModel { viewModel ->
        viewModel.selectTopic("Food")

        val state = viewModel.uiState.value
        assertEquals("Food", state.topic)
        assertEquals(listOf("kochen", "essen"), state.ids())
        assertEquals(listOf(2, 1, 2, 1), state.topics.map { it.wordCount })
    }

    @Test
    fun selectingTheActiveCategoryAgainClearsIt() = runWithViewModel { viewModel ->
        viewModel.selectTopic("Food")
        viewModel.selectTopic("Food")

        assertNull(viewModel.uiState.value.topic)
        assertEquals(6, viewModel.uiState.value.words.size)
    }

    @Test
    fun levelAndCategoryCombine() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A2")
        viewModel.selectTopic("Health")
        assertEquals(listOf("gesund", "krank"), viewModel.uiState.value.ids())

        viewModel.selectLevel("B1")
        assertTrue(viewModel.uiState.value.words.isEmpty())
    }

    // ---- Favorites ----

    @Test
    fun favoritesOnlyShowsJustTheFavorites() = runWithViewModel { viewModel ->
        viewModel.toggleFavorite(kochen)
        viewModel.toggleFavorite(gesund)

        viewModel.toggleFavoritesOnly()

        val state = viewModel.uiState.value
        assertTrue(state.favoritesOnly)
        assertEquals(listOf("kochen", "gesund"), state.ids())

        viewModel.toggleFavoritesOnly()
        assertEquals(6, viewModel.uiState.value.words.size)
    }

    @Test
    fun favoritesCombineWithTheOtherFilters() = runWithViewModel { viewModel ->
        viewModel.toggleFavorite(kochen)
        viewModel.toggleFavorite(gesund)

        viewModel.toggleFavoritesOnly()
        viewModel.selectTopic("Food")

        assertEquals(listOf("kochen"), viewModel.uiState.value.ids())
    }

    @Test
    fun theHeartOfAWordUpdatesTheRepositoryAndTheList() = runWithViewModel { viewModel ->
        viewModel.toggleFavorite(kochen)

        assertEquals("kochen" to true, repository.lastFavoriteCall)
        assertTrue(viewModel.uiState.value.words.first { it.id == "kochen" }.isFavorite)
    }

    // ---- Search ----

    @Test
    fun theQueryIsExposedAtOnceButAppliedAfterTheTypingPause() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("to ")

        assertEquals("to ", viewModel.query.value)
        assertEquals("", viewModel.uiState.value.query)

        letSearchSettle()
        assertEquals("to", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.hasFilters)
    }

    @Test
    fun searchFindsWordsInGermanEnglishAndBengali() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("geh")
        letSearchSettle()
        assertEquals(listOf("gehen"), viewModel.uiState.value.ids())

        viewModel.onQueryChange("healthy")
        letSearchSettle()
        assertEquals(listOf("gesund"), viewModel.uiState.value.ids())

        viewModel.onQueryChange("কাজ")
        letSearchSettle()
        assertEquals(listOf("arbeiten"), viewModel.uiState.value.ids())
    }

    @Test
    fun searchCombinesWithLevelAndCategory() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("to")
        letSearchSettle()
        assertEquals(listOf("gehen", "kochen", "essen", "arbeiten"), viewModel.uiState.value.ids())

        viewModel.selectLevel("A1")
        assertEquals(listOf("gehen", "kochen", "essen"), viewModel.uiState.value.ids())

        viewModel.selectTopic("Food")
        assertEquals(listOf("kochen", "essen"), viewModel.uiState.value.ids())
    }

    @Test
    fun searchChangesTheCategoryCounts() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("to c")
        letSearchSettle()

        val counts = viewModel.uiState.value.topics.associate { it.name to it.wordCount }
        assertEquals(1, counts["Food"])
        assertEquals(0, counts["Health"])
    }

    @Test
    fun fastTypingOnlyAppliesTheLastText() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("t")
        advanceTimeBy(50)
        viewModel.onQueryChange("to c")
        advanceTimeBy(50)

        assertEquals("", viewModel.uiState.value.query)

        letSearchSettle()
        assertEquals(listOf("kochen"), viewModel.uiState.value.ids())
    }

    @Test
    fun aBlankQueryIsNotASearch() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("   ")
        letSearchSettle()

        assertFalse(viewModel.uiState.value.hasFilters)
        assertEquals(6, viewModel.uiState.value.words.size)
    }

    @Test
    fun aQueryWithoutMatchesGivesAnEmptyListAfterLoading() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("zzz")
        letSearchSettle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertTrue(state.words.isEmpty())
        assertTrue(state.hasFilters)
    }

    // ---- Clearing ----

    @Test
    fun clearFiltersResetsEverythingAtOnce() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A1")
        viewModel.selectTopic("Food")
        viewModel.toggleFavoritesOnly()
        viewModel.onQueryChange("to")
        letSearchSettle()
        assertTrue(viewModel.uiState.value.hasFilters)

        viewModel.clearFilters()

        // No waiting: the filters and the search text are gone immediately.
        val state = viewModel.uiState.value
        assertEquals("", viewModel.query.value)
        assertEquals("", state.query)
        assertNull(state.level)
        assertNull(state.topic)
        assertFalse(state.favoritesOnly)
        assertFalse(state.hasFilters)
        assertEquals(6, state.words.size)
    }

    @Test
    fun clearingTheQueryIsAppliedAtOnce() = runWithViewModel { viewModel ->
        viewModel.onQueryChange("to")
        letSearchSettle()

        viewModel.clearQuery()

        assertEquals("", viewModel.uiState.value.query)
        assertEquals(6, viewModel.uiState.value.words.size)
    }

    @Test
    fun showFavoritesClearsEveryOtherFilterAndListsOnlyTheFavorites() = runWithViewModel { viewModel ->
        viewModel.toggleFavorite(kochen)
        viewModel.selectLevel("B1")
        viewModel.selectTopic("Career")
        viewModel.onQueryChange("zzz")
        letSearchSettle()

        viewModel.showFavorites()

        val state = viewModel.uiState.value
        assertTrue(state.favoritesOnly)
        assertNull(state.level)
        assertNull(state.topic)
        assertEquals("", viewModel.query.value)
        assertEquals("", state.query)
        assertEquals(listOf("kochen"), state.ids())
    }
}
