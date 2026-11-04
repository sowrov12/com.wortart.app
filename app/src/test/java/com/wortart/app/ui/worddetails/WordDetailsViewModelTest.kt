package com.wortart.app.ui.worddetails

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordDetailsViewModelTest {

    private class FakeVocabulary(words: List<VocabEntity>) : VocabRepository {
        val all = MutableStateFlow(words)
        val favoriteCalls = mutableListOf<Pair<String, Boolean>>()

        override fun observeWords(filter: VocabFilter): Flow<List<VocabEntity>> = all
        override fun observeFavorites(): Flow<List<VocabEntity>> = all.map { list -> list.filter { it.isFavorite } }
        override fun observeTopics(): Flow<List<String>> = MutableStateFlow(emptyList())
        override suspend fun getWord(id: String) = all.value.firstOrNull { it.id == id }
        override suspend fun wordCount() = all.value.size
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            favoriteCalls += id to isFavorite
            all.value = all.value.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
        override suspend fun getDistractors(card: VocabEntity, count: Int) = emptyList<VocabEntity>()
    }

    private val gehen = VocabEntity(
        id = "iv001",
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
        stammform = "er geht",
    )

    private val vocabulary = FakeVocabulary(listOf(gehen))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.openWord(id: String): WordDetailsViewModel {
        val viewModel = WordDetailsViewModel(id, vocabulary)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        return viewModel
    }

    @Test
    fun theWordIsLoadedWithItsDetails() = runTest {
        val viewModel = openWord("iv001")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("gehen", state.card?.word)
        assertEquals("Irregular Verb", state.content?.categoryLabel)
        assertTrue(state.content!!.hasExtraDetails)
        assertFalse(state.card!!.isFavorite)
    }

    @Test
    fun anUnknownWordIsReportedAfterLoading() = runTest {
        val viewModel = openWord("missing")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.card)
        assertNull(state.content)
    }

    @Test
    fun theFavoriteStateFollowsTheDatabase() = runTest {
        vocabulary.all.value = listOf(gehen.copy(isFavorite = true))

        val viewModel = openWord("iv001")

        assertTrue(viewModel.uiState.value.card!!.isFavorite)
    }

    @Test
    fun theHeartAddsAndRemovesTheFavorite() = runTest {
        val viewModel = openWord("iv001")

        viewModel.toggleFavorite()
        assertTrue(viewModel.uiState.value.card!!.isFavorite)

        viewModel.toggleFavorite()
        assertFalse(viewModel.uiState.value.card!!.isFavorite)

        assertEquals(listOf("iv001" to true, "iv001" to false), vocabulary.favoriteCalls)
        assertNotNull(viewModel.uiState.value.card)
    }

    @Test
    fun togglingAnUnknownWordDoesNothing() = runTest {
        val viewModel = openWord("missing")

        viewModel.toggleFavorite()

        assertTrue(vocabulary.favoriteCalls.isEmpty())
    }
}
