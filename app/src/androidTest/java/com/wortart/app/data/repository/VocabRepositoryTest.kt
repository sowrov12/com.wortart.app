package com.wortart.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabRepositoryTest {

    private lateinit var db: WortArtDatabase
    private lateinit var repository: VocabRepository

    private fun word(
        id: String,
        word: String,
        category: String,
        level: String,
        topic: String,
        en: String,
        bn: String,
    ) = VocabEntity(
        id = id,
        word = word,
        category = category,
        level = level,
        topic = topic,
        translationEn = en,
        translationBn = bn,
        exampleDe = "Beispiel",
        exampleEn = "Example",
        exampleBn = "উদাহরণ",
    )

    private val gehen = word("v1", "gehen", WordCategories.IRREGULAR_VERB, CefrLevels.A1, "Movement", "to go", "যাওয়া")
    private val kochen = word("v2", "kochen", WordCategories.REGULAR_VERB, CefrLevels.A1, "Food", "to cook", "রান্না করা")
    private val arbeiten = word("v3", "arbeiten", WordCategories.REGULAR_VERB, CefrLevels.B1, "Career", "to work", "কাজ করা")
    private val schnell = word("a1", "schnell", WordCategories.ADJECTIVE, CefrLevels.A1, "General", "fast", "দ্রুত")
    private val langsam = word("a2", "langsam", WordCategories.ADJECTIVE, CefrLevels.A1, "General", "slow", "ধীর")
    private val klein = word("a3", "klein", WordCategories.ADJECTIVE, CefrLevels.A2, "General", "small", "ছোট")

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WortArtDatabase::class.java).build()
        repository = VocabRepositoryImpl(db.vocabDao())
        db.vocabDao().insertAll(listOf(gehen, kochen, arbeiten, schnell, langsam, klein))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun defaultFilterReturnsAllWordsSortedByGermanWord() = runTest {
        val words = repository.observeWords().first().map { it.word }
        assertEquals(listOf("arbeiten", "gehen", "klein", "kochen", "langsam", "schnell"), words)
    }

    @Test
    fun filtersByLevelCategoryAndTopic() = runTest {
        val a1Adjectives = repository.observeWords(
            VocabFilter(level = CefrLevels.A1, category = WordCategories.ADJECTIVE),
        ).first()
        assertEquals(listOf("langsam", "schnell"), a1Adjectives.map { it.word })

        val career = repository.observeWords(VocabFilter(topic = "Career")).first()
        assertEquals(listOf("arbeiten"), career.map { it.word })
    }

    @Test
    fun queryMatchesGermanEnglishAndBengali() = runTest {
        assertEquals(listOf("gehen"), repository.observeWords(VocabFilter(query = "geh")).first().map { it.word })
        assertEquals(listOf("langsam"), repository.observeWords(VocabFilter(query = "slow")).first().map { it.word })
        assertEquals(listOf("arbeiten"), repository.observeWords(VocabFilter(query = "কাজ")).first().map { it.word })
    }

    @Test
    fun queryIsTrimmedAndBlankQueryMeansNoSearch() = runTest {
        val trimmed = repository.observeWords(VocabFilter(query = "  কাজ  ")).first()
        assertEquals(listOf("arbeiten"), trimmed.map { it.word })

        val blank = repository.observeWords(VocabFilter(query = "   ")).first()
        assertEquals(6, blank.size)
    }

    @Test
    fun queryCombinesWithFilters() = runTest {
        val result = repository.observeWords(
            VocabFilter(query = "to ", level = CefrLevels.B1),
        ).first()
        assertEquals(listOf("arbeiten"), result.map { it.word })
    }

    @Test
    fun specialCharactersInQueryAreMatchedLiterally() = runTest {
        assertTrue(repository.observeWords(VocabFilter(query = "%")).first().isEmpty())
        assertTrue(repository.observeWords(VocabFilter(query = "_")).first().isEmpty())
    }

    @Test
    fun favoritesCanBeToggled() = runTest {
        assertTrue(repository.observeFavorites().first().isEmpty())

        repository.setFavorite("v1", true)
        assertEquals(listOf("gehen"), repository.observeFavorites().first().map { it.word })
        assertTrue(repository.getWord("v1")?.isFavorite == true)

        repository.setFavorite("v1", false)
        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    @Test
    fun getWordAndCount() = runTest {
        assertEquals("kochen", repository.getWord("v2")?.word)
        assertEquals(null, repository.getWord("missing"))
        assertEquals(6, repository.wordCount())
    }

    @Test
    fun topicsAreDistinctAndSorted() = runTest {
        assertEquals(listOf("Career", "Food", "General", "Movement"), repository.observeTopics().first())
    }

    @Test
    fun distractorsMatchCategoryAndLevelAndExcludeTheCard() = runTest {
        val distractors = repository.getDistractors(schnell, count = 3)
        assertEquals(listOf("a2"), distractors.map { it.id })
        assertFalse(distractors.any { it.id == schnell.id })
    }
}
