package com.wortart.app.data.favorites

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.dao.escapeForLike
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesTest {

    private lateinit var context: Context
    private lateinit var db: WortArtDatabase

    private fun word(id: String, word: String, level: String, topic: String, en: String) = VocabEntity(
        id = id,
        word = word,
        category = "regular_verb",
        level = level,
        topic = topic,
        translationEn = en,
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = "haben",
    )

    private val kochen = word("v1", "kochen", "A1", "Food", "to cook")
    private val essen = word("v2", "essen", "A1", "Food", "to eat")
    private val arbeiten = word("v3", "arbeiten", "B1", "Career", "to work")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WortArtDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- The favorites-only filter ----

    @Test
    fun favoritesOnlyListsJustTheFavorites() = runTest {
        val dao = db.vocabDao()
        dao.insertAll(listOf(kochen, essen, arbeiten))
        dao.setFavorite("v1", true)
        dao.setFavorite("v3", true)

        assertEquals(listOf("arbeiten", "kochen"), dao.observeFiltered(favoritesOnly = true).first().map { it.word })
        assertEquals(3, dao.observeFiltered().first().size)
    }

    @Test
    fun favoritesOnlyCombinesWithLevelAndTopic() = runTest {
        val dao = db.vocabDao()
        dao.insertAll(listOf(kochen, essen, arbeiten))
        dao.setFavorite("v1", true)
        dao.setFavorite("v3", true)

        assertEquals(
            listOf("kochen"),
            dao.observeFiltered(level = "A1", favoritesOnly = true).first().map { it.word },
        )
        assertEquals(
            listOf("arbeiten"),
            dao.observeFiltered(topic = "Career", favoritesOnly = true).first().map { it.word },
        )
        assertTrue(dao.observeFiltered(level = "B2", favoritesOnly = true).first().isEmpty())
    }

    @Test
    fun searchCanBeLimitedToFavorites() = runTest {
        val dao = db.vocabDao()
        dao.insertAll(listOf(kochen, essen, arbeiten))
        dao.setFavorite("v2", true)

        val all = dao.search("to ".escapeForLike()).first()
        val favorites = dao.search("to ".escapeForLike(), favoritesOnly = true).first()

        assertEquals(3, all.size)
        assertEquals(listOf("essen"), favorites.map { it.word })
    }

    @Test
    fun theRepositoryPassesTheFavoritesFilterOn() = runTest {
        val repository = VocabRepositoryImpl(db.vocabDao())
        db.vocabDao().insertAll(listOf(kochen, essen, arbeiten))
        repository.setFavorite("v1", true)

        assertEquals(listOf("kochen"), repository.observeWords(VocabFilter(favoritesOnly = true)).first().map { it.word })
        assertEquals(
            listOf("kochen"),
            repository.observeWords(VocabFilter(query = "cook", favoritesOnly = true)).first().map { it.word },
        )
        assertTrue(repository.observeWords(VocabFilter(query = "eat", favoritesOnly = true)).first().isEmpty())
    }

    @Test
    fun removingAFavoriteTakesItOutOfTheList() = runTest {
        val dao = db.vocabDao()
        dao.insertAll(listOf(kochen))
        dao.setFavorite("v1", true)
        dao.setFavorite("v1", false)

        assertTrue(dao.observeFiltered(favoritesOnly = true).first().isEmpty())
    }

    // ---- Persistence ----

    @Test
    fun favoritesSurviveClosingAndReopeningTheDatabase() {
        val name = "favorites-persistence-test.db"
        context.deleteDatabase(name)

        var fileDb = Room.databaseBuilder(context, WortArtDatabase::class.java, name).build()
        runBlocking {
            fileDb.vocabDao().insertAll(listOf(kochen, essen))
            fileDb.vocabDao().setFavorite("v1", true)
        }
        fileDb.close()

        // Like restarting the app: a new database object on the same file.
        fileDb = Room.databaseBuilder(context, WortArtDatabase::class.java, name).build()
        val kochenAfter = runBlocking { fileDb.vocabDao().getById("v1") }
        val essenAfter = runBlocking { fileDb.vocabDao().getById("v2") }
        fileDb.close()
        context.deleteDatabase(name)

        assertTrue(kochenAfter!!.isFavorite)
        assertFalse(essenAfter!!.isFavorite)
    }
}
