package com.wortart.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.dao.UserStatsDao
import com.wortart.app.data.local.dao.VocabDao
import com.wortart.app.data.local.dao.escapeForLike
import com.wortart.app.data.local.entity.Auxiliaries
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WortArtDatabaseTest {

    private lateinit var db: WortArtDatabase
    private lateinit var vocabDao: VocabDao
    private lateinit var statsDao: UserStatsDao

    private val gehen = VocabEntity(
        id = "v001",
        word = "gehen",
        category = WordCategories.IRREGULAR_VERB,
        level = CefrLevels.A1,
        topic = "Movement",
        translationEn = "to go",
        translationBn = "যাওয়া",
        exampleDe = "Ich gehe nach Hause.",
        exampleEn = "I am going home.",
        exampleBn = "আমি বাড়ি যাচ্ছি।",
        auxiliary = Auxiliaries.SEIN,
        praeteritum = "ging",
        partizip2 = "gegangen",
        stammform = "er geht",
    )

    private val einkaufen = VocabEntity(
        id = "v002",
        word = "einkaufen",
        category = WordCategories.SEPARABLE_VERB,
        level = CefrLevels.A1,
        topic = "Food",
        translationEn = "to shop",
        translationBn = "কেনাকাটা করা",
        exampleDe = "Ich kaufe im Supermarkt ein.",
        exampleEn = "I shop at the supermarket.",
        exampleBn = "আমি সুপারমার্কেটে কেনাকাটা করি।",
        auxiliary = Auxiliaries.HABEN,
        prefix = "ein",
    )

    private val schnell = VocabEntity(
        id = "a001",
        word = "schnell",
        category = WordCategories.ADJECTIVE,
        level = CefrLevels.A1,
        topic = "General",
        translationEn = "fast",
        translationBn = "দ্রুত",
        exampleDe = "Das Auto ist schnell.",
        exampleEn = "The car is fast.",
        exampleBn = "গাড়িটি দ্রুত।",
        komparativ = "schneller",
        superlativ = "am schnellsten",
        antonym = "langsam",
    )

    private val langsam = schnell.copy(
        id = "a002",
        word = "langsam",
        translationEn = "slow",
        translationBn = "ধীর",
        komparativ = "langsamer",
        superlativ = "am langsamsten",
        antonym = "schnell",
    )

    private val arbeiten = VocabEntity(
        id = "v003",
        word = "arbeiten",
        category = WordCategories.REGULAR_VERB,
        level = CefrLevels.B1,
        topic = "Career",
        translationEn = "to work",
        translationBn = "কাজ করা",
        exampleDe = "Er arbeitet in einem Büro.",
        exampleEn = "He works in an office.",
        exampleBn = "সে একটি অফিসে কাজ করে।",
        auxiliary = Auxiliaries.HABEN,
    )

    private val allWords = listOf(gehen, einkaufen, schnell, langsam, arbeiten)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WortArtDatabase::class.java).build()
        vocabDao = db.vocabDao()
        statsDao = db.userStatsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- VocabDao ----

    @Test
    fun insertAndReadById() = runTest {
        vocabDao.insertAll(allWords)
        assertEquals(allWords.size, vocabDao.count())
        assertEquals(gehen, vocabDao.getById("v001"))
        assertNull(vocabDao.getById("missing"))
    }

    @Test
    fun insertAllIgnoresExistingIds() = runTest {
        vocabDao.insertAll(listOf(gehen))
        vocabDao.insertAll(listOf(gehen.copy(translationEn = "changed")))
        assertEquals("to go", vocabDao.getById("v001")?.translationEn)
    }

    @Test
    fun observeAllIsSortedByWord() = runTest {
        vocabDao.insertAll(allWords)
        val words = vocabDao.observeAll().first().map { it.word }
        assertEquals(listOf("arbeiten", "einkaufen", "gehen", "langsam", "schnell"), words)
    }

    @Test
    fun filterByLevelCategoryAndTopic() = runTest {
        vocabDao.insertAll(allWords)

        val a1 = vocabDao.observeFiltered(level = CefrLevels.A1).first()
        assertEquals(4, a1.size)

        val adjectives = vocabDao.observeFiltered(category = WordCategories.ADJECTIVE).first()
        assertEquals(listOf("langsam", "schnell"), adjectives.map { it.word })

        val career = vocabDao.observeFiltered(topic = "Career").first()
        assertEquals(listOf("arbeiten"), career.map { it.word })

        val combined = vocabDao.observeFiltered(
            level = CefrLevels.A1,
            category = WordCategories.SEPARABLE_VERB,
            topic = "Food",
        ).first()
        assertEquals(listOf("einkaufen"), combined.map { it.word })

        assertEquals(allWords.size, vocabDao.observeFiltered().first().size)
    }

    @Test
    fun searchGermanEnglishAndBengali() = runTest {
        vocabDao.insertAll(allWords)

        val german = vocabDao.search("geh".escapeForLike()).first()
        assertEquals(listOf("gehen"), german.map { it.word })

        val english = vocabDao.search("slow".escapeForLike()).first()
        assertEquals(listOf("langsam"), english.map { it.word })

        val bengali = vocabDao.search("কাজ".escapeForLike()).first()
        assertEquals(listOf("arbeiten"), bengali.map { it.word })

        val noMatch = vocabDao.search("xyz".escapeForLike()).first()
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun searchCombinesWithFilters() = runTest {
        vocabDao.insertAll(allWords)
        val result = vocabDao.search("to ".escapeForLike(), level = CefrLevels.B1).first()
        assertEquals(listOf("arbeiten"), result.map { it.word })
    }

    @Test
    fun searchTreatsPercentAndUnderscoreLiterally() = runTest {
        vocabDao.insertAll(allWords)
        assertTrue(vocabDao.search("%".escapeForLike()).first().isEmpty())
        assertTrue(vocabDao.search("_".escapeForLike()).first().isEmpty())
    }

    @Test
    fun favoriteToggleAndObserveFavorites() = runTest {
        vocabDao.insertAll(allWords)
        assertTrue(vocabDao.observeFavorites().first().isEmpty())

        vocabDao.setFavorite("v001", true)
        assertEquals(listOf("gehen"), vocabDao.observeFavorites().first().map { it.word })

        vocabDao.setFavorite("v001", false)
        assertTrue(vocabDao.observeFavorites().first().isEmpty())
    }

    @Test
    fun upsertPreservingFavoritesKeepsFavoriteFlag() = runTest {
        vocabDao.insertAll(allWords)
        vocabDao.setFavorite("v001", true)

        vocabDao.upsertPreservingFavorites(
            listOf(
                gehen.copy(translationEn = "to walk"),
                gehen.copy(id = "v099", word = "laufen", isFavorite = true),
            ),
        )

        val updated = vocabDao.getById("v001")
        assertEquals("to walk", updated?.translationEn)
        assertTrue(updated?.isFavorite == true)

        // A new row coming from the dataset does not become a favorite by itself.
        val added = vocabDao.getById("v099")
        assertNotNull(added)
        assertFalse(added!!.isFavorite)
    }

    @Test
    fun observeTopicsReturnsDistinctSortedTopics() = runTest {
        vocabDao.insertAll(allWords)
        assertEquals(
            listOf("Career", "Food", "General", "Movement"),
            vocabDao.observeTopics().first(),
        )
    }

    @Test
    fun randomDistractorsMatchCategoryAndLevelAndExcludeSelf() = runTest {
        vocabDao.insertAll(allWords)
        val result = vocabDao.getRandomDistractors(
            category = WordCategories.ADJECTIVE,
            level = CefrLevels.A1,
            excludeId = "a001",
            limit = 3,
        )
        assertEquals(listOf("a002"), result.map { it.id })
    }

    // ---- UserStatsDao ----

    @Test
    fun statsAreNullBeforeInsertThenDefault() = runTest {
        assertNull(statsDao.get())
        statsDao.insertIfAbsent()
        assertEquals(UserStats(), statsDao.get())
    }

    @Test
    fun insertIfAbsentDoesNotOverwriteProgress() = runTest {
        statsDao.insertIfAbsent()
        statsDao.update(UserStats(streakCount = 5, cardsPlayed = 20))

        statsDao.insertIfAbsent()

        val stats = statsDao.get()
        assertEquals(5, stats?.streakCount)
        assertEquals(20, stats?.cardsPlayed)
    }

    @Test
    fun updateAndObserveStats() = runTest {
        statsDao.insertIfAbsent()
        val updated = UserStats(
            streakCount = 3,
            cardsPlayed = 40,
            successRate = 0.75f,
            practiceTimeSeconds = 1_800L,
            perfectPronunciations = 7,
            lastPracticeDate = 20_000L,
        )
        statsDao.update(updated)
        assertEquals(updated, statsDao.observe().first())
    }

    @Test
    fun upsertCreatesRowWhenMissing() = runTest {
        statsDao.upsert(UserStats(cardsPlayed = 1))
        assertEquals(1, statsDao.get()?.cardsPlayed)
    }
}
