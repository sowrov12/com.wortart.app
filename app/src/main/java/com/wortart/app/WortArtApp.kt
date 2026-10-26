package com.wortart.app

import android.app.Application
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.seed.VocabSeeder
import com.wortart.app.data.repository.StatsRepository
import com.wortart.app.data.repository.StatsRepositoryImpl
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.data.repository.VocabRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registered in AndroidManifest.xml via android:name=".WortArtApp".
 *
 * Seeding runs in the background. Room Flows re-emit when the rows are inserted,
 * so screens that observe the repository update automatically on first launch.
 */
class WortArtApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: WortArtDatabase by lazy { WortArtDatabase.getInstance(this) }

    val vocabRepository: VocabRepository by lazy { VocabRepositoryImpl(database.vocabDao()) }

    val statsRepository: StatsRepository by lazy { StatsRepositoryImpl(database.userStatsDao()) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            VocabSeeder(this@WortArtApp, database).seedIfNeeded()
        }
    }
}
