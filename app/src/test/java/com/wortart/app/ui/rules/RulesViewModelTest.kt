package com.wortart.app.ui.rules

import com.wortart.app.data.lessons.LessonsRepository
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.rules.RuleBlock
import com.wortart.app.domain.rules.lessons.Bi
import com.wortart.app.domain.rules.lessons.Lesson
import com.wortart.app.domain.rules.lessons.LessonBlock
import com.wortart.app.domain.rules.lessons.LessonsFile
import com.wortart.app.domain.rules.lessons.QuizItem
import com.wortart.app.domain.rules.lessons.Trio
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RulesViewModelTest {

    private class FakeLessons(var result: Result<LessonsFile> = Result.success(lessonsFile())) : LessonsRepository {
        var loads = 0

        override suspend fun load(): Result<LessonsFile> {
            loads++
            return result
        }
    }

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
        val viewModel = RulesViewModel(FakeRepository(emptyList()), FakeLessons())
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
        val viewModel = RulesViewModel(repository, FakeLessons())
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
        val viewModel = RulesViewModel(repository, FakeLessons())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sections.collect { } }

        repository.all.value = repository.all.value + entity("kommen", "irregular_verb")

        val rows = tableRows(viewModel, "Irregular verbs in your vocabulary")!!
        assertEquals(listOf("gehen", "kommen"), rows.map { it[0] })
        assertTrue(rows.all { it.size == 4 })
    }

    // ---- Lessons ----

    private fun lessonsViewModel(lessons: FakeLessons = FakeLessons()): RulesViewModel =
        RulesViewModel(FakeRepository(emptyList()), lessons)

    @Test
    fun lessonsAreLoadedFromTheRepository() = runTest {
        val viewModel = lessonsViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.lessons.collect { } }

        val state = viewModel.lessons.value
        assertEquals(LessonsStatus.READY, state.status)
        assertEquals(listOf("one", "two", "three"), state.lessons.map { it.id })
        assertNull(state.error)
    }

    @Test
    fun aFailedLoadShowsAnErrorAndCanBeRetried() = runTest {
        val fake = FakeLessons(Result.failure(IllegalArgumentException("Bad file")))
        val viewModel = lessonsViewModel(fake)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.lessons.collect { } }

        assertEquals(LessonsStatus.ERROR, viewModel.lessons.value.status)
        assertEquals("Bad file", viewModel.lessons.value.error)

        fake.result = Result.success(lessonsFile())
        viewModel.load()

        assertEquals(LessonsStatus.READY, viewModel.lessons.value.status)
        assertEquals(2, fake.loads)
    }

    @Test
    fun theLevelFilterKeepsOnlyMatchingLessons() = runTest {
        val viewModel = lessonsViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.lessons.collect { } }

        viewModel.selectLevel("A2")
        assertEquals(listOf("three"), viewModel.lessons.value.lessons.map { it.id })

        viewModel.selectLevel("A1")
        assertEquals(listOf("one", "two"), viewModel.lessons.value.lessons.map { it.id })

        viewModel.selectLevel("B2")
        assertTrue(viewModel.lessons.value.lessons.isEmpty())
    }

    @Test
    fun selectingTheActiveLevelAgainShowsAllLessons() = runTest {
        val viewModel = lessonsViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.lessons.collect { } }

        viewModel.selectLevel("A1")
        viewModel.selectLevel("A1")

        assertNull(viewModel.lessons.value.level)
        assertEquals(3, viewModel.lessons.value.lessons.size)
    }

    @Test
    fun quizAnswersAreKeptAndCanOnlyBeGivenOnce() = runTest {
        val viewModel = lessonsViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.lessons.collect { } }

        viewModel.answerQuiz("one:0", "den")
        viewModel.answerQuiz("one:0", "der")
        viewModel.answerQuiz("two:0", "die")

        assertEquals(mapOf("one:0" to "den", "two:0" to "die"), viewModel.lessons.value.answers)
    }

    @Test
    fun levelAndAnswersSurviveAReload() = runTest {
        val viewModel = lessonsViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.lessons.collect { } }
        viewModel.selectLevel("A1")
        viewModel.answerQuiz("one:0", "den")

        viewModel.load()

        assertEquals("A1", viewModel.lessons.value.level)
        assertEquals(mapOf("one:0" to "den"), viewModel.lessons.value.answers)
        assertFalse(viewModel.lessons.value.lessons.isEmpty())
        assertNotNull(viewModel.lessons.value.file)
    }
}

private fun lessonsFile() = LessonsFile(
    version = 1,
    genderColors = mapOf("der" to "#4C8DFF", "die" to "#FF5A5F", "das" to "#2EB67D"),
    sections = listOf(
        lesson("one", "A1"),
        lesson("two", "A1"),
        lesson("three", "A2"),
    ),
)

private fun lesson(id: String, level: String) = Lesson(
    id = id,
    level = level,
    icon = "menu_book",
    title = Trio("Titel $id", "Title $id", "শিরোনাম $id"),
    summary = Bi("Summary $id", "সারসংক্ষেপ $id"),
    blocks = listOf(LessonBlock.Text(en = "Text $id", bn = "লেখা $id")),
    quiz = listOf(QuizItem("Ich sehe ___ Mann.", listOf("den", "der"), 0, Bi("Accusative.", "কর্ম কারক।"))),
)
