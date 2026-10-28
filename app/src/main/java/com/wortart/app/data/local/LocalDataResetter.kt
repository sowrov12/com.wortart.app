package com.wortart.app.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wipes everything stored on this device (favorites, statistics) and restores the bundled
 * vocabulary. Used when an account is deleted.
 *
 * @param reseed loads the bundled words and creates the empty stats row again.
 */
class LocalDataResetter(
    private val db: WortArtDatabase,
    private val reseed: suspend () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun reset() {
        withContext(ioDispatcher) {
            db.clearAllTables()
            reseed()
        }
    }
}
