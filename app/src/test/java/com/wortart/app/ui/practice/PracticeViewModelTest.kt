package com.wortart.app.ui.practice

import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.StatsRepository
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private class FakeVocabulary(words: List<VocabEntity>) : VocabRepository {
        val all = MutableStateFlow(words)
        val favoriteCalls = mutableListOf<Pair<String, Boolean>>()

        override fun observeWords(filter: VocabFilter): Flow<List<VocabEntity>> = all
        override fun observeFavorites(): Flow<List<VocabEntity>> = all
        override fun observeTopics(): Flow<List<String>> = MutableStateFlow(emptyList())
        override suspend fun getWord(id: String) = all.value.firstOrNull { it.id == id }
        override suspend fun wordCount() = all.value.size
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            favoriteCalls += id to isFavorite
        }
        override suspend fun getDistractors(card: VocabEntity, count: Int) = emptyList<VocabEntity>()
    }

    private class FakeStats : StatsRepository {
        val answers = mutableListOf<Boolean>()

        override fun observeStats(): Flow<UserStats> = MutableStateFlow(UserStats())
        override suspend fun recordAnswer(isCorrect: Boolean) {
            answers += isCorrect
        }
        override suspend fun addPracticeTime(seconds: Long) = Unit
        override suspend fun recordPerfectPronunciation() = Unit
    }

    private fun entity(
        id: String,
        category: String,
        en: String = "en-$id",
        bn: String = "bn-$id",
        auxiliary: String? = null,
        prefix: String? = null,
    ) = VocabEntity(
        id = id,
        word = id,
        category = category,
        level = "A1",
        topic = "General",
        translationEn = en,
        translationBn = bn,
        exampleDe = "de $id",
        exampleEn = "en $id",
        exampleBn = "bn $id",
        auxiliary = auxiliary,
        prefix = prefix,
    )

    private val words = listOf(
        entity("gehen", "irregular_verb", auxiliary = "sein"),
        entity("lernen", "regular_verb", auxiliary = "haben"),
        entity("einkaufen", "separable_verb", auxiliary = "haben", prefix = "ein"),
        entity("schnell", "adjective", en = "fast", bn = "দ্রুত"),
        entity("langsam", "adjective", en = "slow", bn = "ধীর"),
        entity("klein", "adjective", en = "small", bn = "ছোট"),
    )

    private val vocabulary = FakeVocabulary(words)
    private val stats = FakeStats()
    private lateinit var viewModel: PracticeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = PracticeViewModel(vocabulary, stats, sessionSize = 3, random = Random(7))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    private fun answerCurrentCard(correct: Boolean) {
        val question = state.current!!.question
        val choice = if (correct) question.correctAnswer else question.options.first { it != question.correctAnswer }
        viewModel.select(choice)
        viewModel.submit()
    }

    // ---- Starting ----

    @Test
    fun startsASessionOfTheRequestedSizeWithAQuestionOnEveryCard() {
        viewModel.startOrResume()

        assertEquals(PracticeStatus.ACTIVE, state.status)
        assertEquals(3, state.total)
        assertEquals(0, state.index)
        assertEquals(1f / 3f, state.progress, 0.0001f)
        state.cards.forEach { assertTrue(it.question.options.contains(it.question.correctAnswer)) }
    }

    @Test
    fun anEmptyVocabularyShowsTheEmptyState() {
        vocabulary.all.value = emptyList()

        viewModel.startOrResume()

        assertEquals(PracticeStatus.EMPTY, state.status)
    }

    @Test
    fun fewerWordsThanTheSessionSizeStillMakeASession() {
        vocabulary.all.value = words.take(2)
        viewModel.startOrResume()

        assertEquals(2, state.total)
    }

    // ---- Answering ----

    @Test
    fun aCorrectAnswerIsCountedAndRecorded() {
        viewModel.startOrResume()

        answerCurrentCard(correct = true)

        assertTrue(state.submitted)
        assertTrue(state.isCorrect)
        assertEquals(1, state.correctCount)
        assertEquals(listOf(true), stats.answers)
    }

    @Test
    fun aWrongAnswerIsRecordedButNotCounted() {
        viewModel.startOrResume()

        answerCurrentCard(correct = false)

        assertTrue(state.submitted)
        assertFalse(state.isCorrect)
        assertEquals(0, state.correctCount)
        assertEquals(listOf(false), stats.answers)
    }

    @Test
    fun submittingNeedsAMarkedOption() {
        viewModel.startOrResume()

        viewModel.submit()

        assertFalse(state.submitted)
        assertTrue(stats.answers.isEmpty())
    }

    @Test
    fun markingAnOptionDoesNotSubmitIt() {
        viewModel.startOrResume()
        val option = state.current!!.question.options.first()

        viewModel.select(option)

        assertEquals(option, state.selected)
        assertFalse(state.submitted)
        assertTrue(stats.answers.isEmpty())
    }

    @Test
    fun theMarkedOptionCanBeChangedBeforeSubmitting() {
        viewModel.startOrResume()
        val (first, second) = state.current!!.question.options

        viewModel.select(first)
        viewModel.select(second)

        assertEquals(second, state.selected)
    }

    @Test
    fun theAnswerIsLockedAfterSubmitting() {
        viewModel.startOrResume()
        answerCurrentCard(correct = true)
        val locked = state.selected

        viewModel.select(state.current!!.question.options.first { it != locked })
        viewModel.submit()

        assertEquals(locked, state.selected)
        assertEquals(listOf(true), stats.answers)
        assertEquals(1, state.correctCount)
    }

    // ---- Moving on ----

    @Test
    fun nextOnlyWorksAfterSubmitting() {
        viewModel.startOrResume()

        viewModel.next()

        assertEquals(0, state.index)
    }

    @Test
    fun nextShowsTheFollowingCardWithoutAMarkedOption() {
        viewModel.startOrResume()
        answerCurrentCard(correct = true)

        viewModel.next()

        assertEquals(1, state.index)
        assertNull(state.selected)
        assertFalse(state.submitted)
        assertEquals(2f / 3f, state.progress, 0.0001f)
    }

    @Test
    fun theSessionFinishesAfterTheLastCard() {
        viewModel.startOrResume()

        answerCurrentCard(correct = true)
        viewModel.next()
        answerCurrentCard(correct = false)
        viewModel.next()
        assertTrue(state.isLastCard)
        answerCurrentCard(correct = true)
        viewModel.next()

        assertEquals(PracticeStatus.FINISHED, state.status)
        assertEquals(2, state.correctCount)
        assertEquals(3, state.total)
        assertEquals(listOf(true, false, true), stats.answers)
    }

    // ---- Resuming and restarting ----

    @Test
    fun openingTheSessionAgainKeepsTheRunningOne() {
        viewModel.startOrResume()
        answerCurrentCard(correct = true)
        viewModel.next()
        val cards = state.cards

        viewModel.startOrResume()

        assertEquals(1, state.index)
        assertEquals(1, state.correctCount)
        assertEquals(cards, state.cards)
    }

    @Test
    fun startingNewReplacesTheRunningSession() {
        viewModel.startOrResume()
        answerCurrentCard(correct = true)

        viewModel.startNew()

        assertEquals(PracticeStatus.ACTIVE, state.status)
        assertEquals(0, state.index)
        assertEquals(0, state.correctCount)
        assertFalse(state.submitted)
    }

    @Test
    fun aFinishedSessionIsReplacedWhenOpenedAgain() {
        viewModel.startOrResume()
        repeat(3) {
            answerCurrentCard(correct = true)
            viewModel.next()
        }
        assertEquals(PracticeStatus.FINISHED, state.status)

        viewModel.startOrResume()

        assertEquals(PracticeStatus.ACTIVE, state.status)
        assertEquals(0, state.index)
    }

    // ---- Favorites ----

    @Test
    fun theHeartTogglesTheFavoriteOfTheCurrentCard() {
        viewModel.startOrResume()
        val id = state.current!!.card.id

        viewModel.toggleFavorite()

        assertTrue(state.current!!.card.isFavorite)
        assertEquals(listOf(id to true), vocabulary.favoriteCalls)

        viewModel.toggleFavorite()

        assertFalse(state.current!!.card.isFavorite)
        assertEquals(listOf(id to true, id to false), vocabulary.favoriteCalls)
        assertNotNull(state.current)
    }
}
