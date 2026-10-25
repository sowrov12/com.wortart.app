package com.wortart.app.data.local.seed

import android.content.Context
import com.wortart.app.data.local.WortArtDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the bundled vocabulary (assets/vocab_seed.json) into Room.
 *
 * - First launch: inserts all words.
 * - Later launches: does nothing, unless [SEED_VERSION] was increased (new dataset shipped
 *   in an app update). In that case words are upserted and the user's favorites are kept.
 * - Also makes sure the single UserStats row exists.
 */
class VocabSeeder(
    private val context: Context,
    private val db: WortArtDatabase,
) {

    /** Returns true if the dataset was written to the database, false if it was already up to date. */
    suspend fun seedIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        db.userStatsDao().insertIfAbsent()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val vocabDao = db.vocabDao()
        val upToDate = prefs.getInt(KEY_SEED_VERSION, 0) >= SEED_VERSION && vocabDao.count() > 0
        if (upToDate) return@withContext false

        val json = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        vocabDao.upsertPreservingFavorites(VocabSeedParser.parse(json))
        prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).commit()
        true
    }

    companion object {
        /** Increase this whenever vocab_seed.json changes. */
        const val SEED_VERSION = 1
        const val ASSET_NAME = "vocab_seed.json"
        const val PREFS_NAME = "wortart_seed"
        private const val KEY_SEED_VERSION = "seed_version"
    }
}
