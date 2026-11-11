package com.wortart.app.ui.practice

import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.StatsRepository
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.question.QuestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSetupTest {

    private class FakeVocabulary(words: List<VocabEntity>) : VocabRepository {
        val all = MutableStateFlow(words)
        override fun observeWords(filter: VocabFilter): Flow<List<VocabEntity>> = all
        override fun observeFavorites(): Flow<List<VocabEntity>> = all
        override fun observeTopics(): Flow<List<String>> = MutableStateFlow(emptyList())
        override suspend fun getWord(id: String) = all.value.firstOrNull { it.id == id }
        override suspend fun wordCount() = all.value.size
        override suspend fun setFavorite(id: String, isFavorite: Boolean) = Unit
        override suspend fun getDistractors(card: VocabEntity, count: Int) = emptyList<VocabEntity>()
    }

    private class FakeStats : StatsRepository {
        override fun observeStats(): Flow<UserStats> = MutableStateFlow(UserStats())
        override suspend fun recordAnswer(isCorrect: Boolean) = Unit
        override suspend fun addPracticeTime(seconds: Long) = Unit
        override suspend fun recordPerfectPronunciation() = Unit
    }

    private fun entity(id: String, level: String, tags: String) = VocabEntity(
        id = id, word = id, category = "regular_verb", level = level, topic = "Regular Verbs",
        translationEn = "en-$id", translationBn = "bn-$id",
        exampleDe = "de $id", exampleEn = "en $id", exampleBn = "bn $id",
        auxiliary = "haben", prefix = if ("Separable" in tags) "an" else null, tags = tags,
        prefixBlankDe = "de _____", prefixBlankTitle = "_____$id",
    )

    private val words = listOf(
        entity("a", "A1", ",Regular Verbs,Separable Verbs,"),
        entity("b", "A2", ",Regular Verbs,Separable Verbs,"),
        entity("c", "A1", ",Regular Verbs,"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setupListsGroupsAndCountsMatchingWords() {
        val vm = PracticeViewModel(FakeVocabulary(words), FakeStats(), random = Random(1))

        vm.openSetup()
        vm.selectGroup("Separable Verbs")
        vm.toggleLevel("A1")

        val state = vm.uiState.value
        assertEquals(PracticeStatus.SETUP, state.status)
        assertTrue("Separable Verbs" in state.presentGroups)
        assertEquals(1, state.availableCount)
    }

    @Test
    fun sessionOnlyContainsWordsOfTheChosenGroupAndSet() {
        val vm = PracticeViewModel(
            FakeVocabulary(words), FakeStats(), sessionSize = 5, random = Random(1),
            initialConfig = PracticeConfig(group = "Separable Verbs", type = QuestionType.PREFIX),
        )

        vm.startNew()

        val state = vm.uiState.value
        assertEquals(PracticeStatus.ACTIVE, state.status)
        assertEquals(setOf("a", "b"), state.cards.map { it.card.id }.toSet())
        assertTrue(state.cards.all { it.question.type == QuestionType.PREFIX })
    }
}
