package com.wortart.app.ui.rules

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.rules.RuleBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
class RulesViewModelTest {

    private class FakeRepository(words: List<VocabEntity>) : VocabRepository {
        val all = MutableStateFlow(words)

        override fun observeWords(filter: VocabFilter) = all.map { list ->
            list.filter { filter.category == null || it.category == filter.category }
        }

        override fun observeFavorites() = all.map { list -> list.filter { it.isFavorite } }
        override fun observeTopics() = all.map { list -> list.map { it.topic }.distinct() }
        override suspend fun getWord(id: String) = all.value.firstOrNull { it.id == id }
        override suspend fun wordCount() = all.value.size
        override suspend fun setFavorite(id: String, isFavorite: Boolean) = Unit
        override suspend fun getDistractors(card: VocabEntity, count: Int) = emptyList<VocabEntity>()
    }

    private fun entity(word: String, category: String) = VocabEntity(
        id = word,
        word = word,
        category = category,
        level = "A1",
        topic = "General",
        translationEn = "en",
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = "haben",
        praeteritum = "p-$word",
        partizip2 = "pp-$word",
        komparativ = "k-$word",
        superlativ = "s-$word",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tableRows(viewModel: RulesViewModel, title: String): List<List<String>>? =
        viewModel.sections.value
            .flatMap { it.blocks }
            .filterIsInstance<RuleBlock.Table>()
            .firstOrNull { it.title == title }
            ?.rows

    @Test
    fun staticRulesAreAvailableBeforeAnyVocabularyExists() = runTest {
        val viewModel = RulesViewModel(FakeRepository(emptyList()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sections.collect { } }

        assertEquals(5, viewModel.sections.value.size)
        assertEquals(null, tableRows(viewModel, "Irregular verbs in your vocabulary"))
    }

    @Test
    fun tablesAreFilledFromTheVocabularyByCategory() = runTest {
        val repository = FakeRepository(
            listOf(
                entity("gehen", "irregular_verb"),
                entity("machen", "regular_verb"),
                entity("schnell", "adjective"),
            ),
        )
        val viewModel = RulesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sections.collect { } }

        assertEquals(
            listOf(listOf("gehen", "p-gehen", "pp-gehen", "haben")),
            tableRows(viewModel, "Irregular verbs in your vocabulary"),
        )
        assertEquals(
            listOf(listOf("schnell", "k-schnell", "s-schnell")),
            tableRows(viewModel, "Adjectives in your vocabulary"),
        )
    }

    @Test
    fun tablesUpdateWhenTheVocabularyChanges() = runTest {
        val repository = FakeRepository(listOf(entity("gehen", "irregular_verb")))
        val viewModel = RulesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sections.collect { } }

        repository.all.value = repository.all.value + entity("kommen", "irregular_verb")

        val rows = tableRows(viewModel, "Irregular verbs in your vocabulary")!!
        assertEquals(listOf("gehen", "kommen"), rows.map { it[0] })
        assertTrue(rows.all { it.size == 4 })
    }
}
