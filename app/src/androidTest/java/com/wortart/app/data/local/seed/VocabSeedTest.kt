package com.wortart.app.data.local.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.dao.escapeForLike
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.data.local.entity.WordCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabSeedTest {

    private lateinit var context: Context
    private lateinit var db: WortArtDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(VocabSeeder.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, WortArtDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun readSeedJson(): String =
        context.assets.open(VocabSeeder.ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun seedFileHasFiftyValidItems() {
        val items = VocabSeedParser.parse(readSeedJson())
        assertEquals(50, items.size)
        assertEquals(50, items.map { it.id }.toSet().size)
    }

    @Test
    fun seedFileCoversAllCategoriesAndLevels() {
        val items = VocabSeedParser.parse(readSeedJson())
        WordCategories.ALL.forEach { category ->
            assertTrue("No items for $category", items.any { it.category == category })
        }
        CefrLevels.ALL.forEach { level ->
            assertTrue("No items for $level", items.any { it.level == level })
        }
    }

    @Test
    fun parserRejectsInvalidData() {
        val badCategory = """[{"id":"x","word":"w","category":"noun","level":"A1","topic":"t",
            "translationEn":"e","translationBn":"b","exampleDe":"d","exampleEn":"e","exampleBn":"b"}]"""
        val missingAuxiliary = """[{"id":"x","word":"w","category":"regular_verb","level":"A1","topic":"t",
            "translationEn":"e","translationBn":"b","exampleDe":"d","exampleEn":"e","exampleBn":"b"}]"""
        listOf(badCategory, missingAuxiliary).forEach { json ->
            val failed = try {
                VocabSeedParser.parse(json)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
            assertTrue("Expected IllegalArgumentException", failed)
        }
    }

    @Test
    fun seederFillsEmptyDatabaseAndCreatesStatsRow() = runTest {
        val seeded = VocabSeeder(context, db).seedIfNeeded()

        assertTrue(seeded)
        assertEquals(50, db.vocabDao().count())
        assertNotNull(db.userStatsDao().get())
    }

    @Test
    fun seederIsIdempotent() = runTest {
        val seeder = VocabSeeder(context, db)
        assertTrue(seeder.seedIfNeeded())
        assertFalse(seeder.seedIfNeeded())
        assertEquals(50, db.vocabDao().count())
    }

    @Test
    fun reseedKeepsFavorites() = runTest {
        val seeder = VocabSeeder(context, db)
        seeder.seedIfNeeded()
        db.vocabDao().setFavorite("iv001", true)

        // Simulate an app update that ships a newer dataset.
        context.getSharedPreferences(VocabSeeder.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        assertTrue(seeder.seedIfNeeded())

        assertEquals(50, db.vocabDao().count())
        assertTrue(db.vocabDao().getById("iv001")?.isFavorite == true)
    }

    @Test
    fun seededDataIsSearchableInAllThreeLanguages() = runTest {
        VocabSeeder(context, db).seedIfNeeded()
        val dao = db.vocabDao()

        assertTrue(dao.search("gehen".escapeForLike()).first().any { it.id == "iv001" })
        assertTrue(dao.search("to eat".escapeForLike()).first().any { it.id == "iv003" })
        assertTrue(dao.search("খাওয়া".escapeForLike()).first().any { it.id == "iv003" })
    }

    @Test
    fun seededDataSupportsExploreFilters() = runTest {
        VocabSeeder(context, db).seedIfNeeded()
        val dao = db.vocabDao()

        val separableA1 = dao.observeFiltered(
            level = CefrLevels.A1,
            category = WordCategories.SEPARABLE_VERB,
        ).first()
        assertEquals(3, separableA1.size)

        assertTrue(dao.observeTopics().first().containsAll(listOf("Food", "Health", "Career")))
    }
}
