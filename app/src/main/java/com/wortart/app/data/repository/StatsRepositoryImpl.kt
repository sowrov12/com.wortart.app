package com.wortart.app.data.repository

import com.wortart.app.data.local.dao.UserStatsDao
import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.domain.stats.StatsCalculator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

class StatsRepositoryImpl(
    private val dao: UserStatsDao,
    private val todayEpochDay: () -> Long = { LocalDate.now().toEpochDay() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StatsRepository {

    // Every update is read-modify-write; the lock makes concurrent updates safe.
    private val updateLock = Mutex()

    override fun observeStats(): Flow<UserStats> =
        dao.observe().map { it ?: UserStats() }.distinctUntilChanged().flowOn(ioDispatcher)

    override suspend fun recordAnswer(isCorrect: Boolean) =
        update { StatsCalculator.afterAnswer(it, isCorrect, todayEpochDay()) }

    override suspend fun addPracticeTime(seconds: Long) =
        update { StatsCalculator.afterPracticeTime(it, seconds) }

    override suspend fun recordPerfectPronunciation() =
        update { StatsCalculator.afterPerfectPronunciation(it) }

    private suspend fun update(transform: (UserStats) -> UserStats) {
        withContext(ioDispatcher) {
            updateLock.withLock {
                val current = dao.get() ?: UserStats().also { dao.insertIfAbsent(it) }
                dao.upsert(transform(current))
            }
        }
    }
}
