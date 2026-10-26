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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
                    (filter.topic == null || it.topic == filter.topic)
            }
        }

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

    private fun word(id: String, topic: String, level: String) = VocabEntity(
        id = id,
        word = id,
        category = "regular_verb",
        level = level,
        topic = topic,
        translationEn = "en",
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = "haben",
    )

    private val gehen = word("gehen", "Movement", "A1")
    private val kochen = word("kochen", "Food", "A1")
    private val essen = word("essen", "Food", "A1")
    private val arbeiten = word("arbeiten", "Career", "B1")
    private val gesund = word("gesund", "Health", "A2")
    private val krank = word("krank", "Health", "A2")

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

    @Test
    fun topicsAreSortedFeaturedFirstAndCounted() = runWithViewModel { viewModel ->
        val topics = viewModel.uiState.value.topics

        assertEquals(listOf("Food", "Career", "Health", "Movement"), topics.map { it.name })
        assertEquals(listOf(2, 1, 2, 1), topics.map { it.wordCount })
        assertEquals(6, viewModel.uiState.value.totalWords)
        assertEquals(ExploreDestination.Topics, viewModel.uiState.value.destination)
    }

    @Test
    fun levelFilterChangesTheTopicCounts() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A1")

        val state = viewModel.uiState.value
        assertEquals("A1", state.level)
        assertEquals(
            mapOf("Food" to 2, "Career" to 0, "Health" to 0, "Movement" to 1),
            state.topics.associate { it.name to it.wordCount },
        )
    }

    @Test
    fun selectingTheActiveLevelAgainClearsIt() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A1")
        viewModel.selectLevel("A1")
        assertEquals(null, viewModel.uiState.value.level)

        viewModel.selectLevel("B1")
        viewModel.selectLevel(null)
        assertEquals(null, viewModel.uiState.value.level)
    }

    @Test
    fun openingATopicShowsItsWords() = runWithViewModel { viewModel ->
        viewModel.openTopic("Food")

        val state = viewModel.uiState.value
        assertEquals(ExploreDestination.Words("Food"), state.destination)
        assertEquals(listOf("kochen", "essen"), state.words.map { it.id })
    }

    @Test
    fun topicAndLevelFiltersCombine() = runWithViewModel { viewModel ->
        viewModel.selectLevel("A2")
        viewModel.openTopic("Health")
        assertEquals(listOf("gesund", "krank"), viewModel.uiState.value.words.map { it.id })

        viewModel.selectLevel("B1")
        assertTrue(viewModel.uiState.value.words.isEmpty())
    }

    @Test
    fun openingAllWordsShowsEveryWord() = runWithViewModel { viewModel ->
        viewModel.openTopic(null)

        val state = viewModel.uiState.value
        assertEquals(ExploreDestination.Words(null), state.destination)
        assertEquals(6, state.words.size)
    }

    @Test
    fun closingATopicReturnsToTheGrid() = runWithViewModel { viewModel ->
        viewModel.openTopic("Food")
        viewModel.closeTopic()

        val state = viewModel.uiState.value
        assertEquals(ExploreDestination.Topics, state.destination)
        assertTrue(state.words.isEmpty())
    }

    @Test
    fun toggleFavoriteUpdatesTheRepositoryAndTheList() = runWithViewModel { viewModel ->
        viewModel.openTopic("Food")
        viewModel.toggleFavorite(kochen)

        assertEquals("kochen" to true, repository.lastFavoriteCall)
        val updated = viewModel.uiState.value.words.first { it.id == "kochen" }
        assertTrue(updated.isFavorite)
    }
}
