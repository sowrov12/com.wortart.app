package com.wortart.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.dao.UserStatsDao
import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.domain.stats.ActivityHistory
import com.wortart.app.domain.stats.ActivityLog
import com.wortart.app.domain.stats.DayActivity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsRepositoryTest {

    private lateinit var db: WortArtDatabase
    private lateinit var dao: UserStatsDao
    private lateinit var repository: StatsRepository
    private var today = 20_000L

    private class RecordingLog : ActivityLog {
        val history = mutableMapOf<Long, Int>()
        override fun recordAnswer(day: Long) {
            history[day] = (history[day] ?: 0) + 1
        }
        override fun lastDays(today: Long, days: Int): List<DayActivity> =
            ActivityHistory.lastDays(history, today, days)
        override fun clear() = history.clear()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WortArtDatabase::class.java).build()
        dao = db.userStatsDao()
        repository = StatsRepositoryImpl(dao, todayEpochDay = { today })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun emitsDefaultStatsBeforeAnyUpdate() = runTest {
        assertEquals(UserStats(), repository.observeStats().first())
    }

    @Test
    fun answersArePersisted() = runTest {
        repository.recordAnswer(true)
        repository.recordAnswer(false)

        val stats = dao.get()!!
        assertEquals(2, stats.cardsPlayed)
        assertEquals(0.5f, stats.successRate, 0.0001f)
        assertEquals(today, stats.lastPracticeDate)
        assertEquals(stats, repository.observeStats().first())
    }

    @Test
    fun streakFollowsTheCalendar() = runTest {
        repository.recordAnswer(true)
        today += 1
        repository.recordAnswer(true)
        assertEquals(2, dao.get()!!.streakCount)

        today += 5
        repository.recordAnswer(true)
        assertEquals(1, dao.get()!!.streakCount)
    }

    @Test
    fun practiceTimeAndPronunciationsAreAdded() = runTest {
        repository.addPracticeTime(90)
        repository.addPracticeTime(30)
        repository.recordPerfectPronunciation()

        val stats = dao.get()!!
        assertEquals(120L, stats.practiceTimeSeconds)
        assertEquals(1, stats.perfectPronunciations)
    }

    @Test
    fun existingProgressIsKeptWhenUpdating() = runTest {
        dao.insertIfAbsent(UserStats(cardsPlayed = 10, successRate = 0.5f, practiceTimeSeconds = 100))

        repository.recordAnswer(true)

        val stats = dao.get()!!
        assertEquals(11, stats.cardsPlayed)
        assertEquals(100L, stats.practiceTimeSeconds)
    }

    @Test
    fun parallelUpdatesAreNotLost() = runTest {
        coroutineScope {
            repeat(50) {
                launch { repository.recordAnswer(true) }
                launch { repository.addPracticeTime(2) }
            }
        }

        val stats = dao.get()!!
        assertEquals(50, stats.cardsPlayed)
        assertEquals(100L, stats.practiceTimeSeconds)
        assertEquals(1f, stats.successRate, 0.0001f)
    }

    @Test
    fun everyAnswerIsAddedToTheDailyActivityLog() = runTest {
        val log = RecordingLog()
        val repositoryWithLog = StatsRepositoryImpl(dao, log, todayEpochDay = { today })

        repositoryWithLog.recordAnswer(true)
        repositoryWithLog.recordAnswer(false)
        today += 1
        repositoryWithLog.recordAnswer(true)

        assertEquals(mapOf(20_000L to 2, 20_001L to 1), log.history)
    }

    @Test
    fun practiceTimeDoesNotCountAsAnAnswerInTheLog() = runTest {
        val log = RecordingLog()
        val repositoryWithLog = StatsRepositoryImpl(dao, log, todayEpochDay = { today })

        repositoryWithLog.addPracticeTime(60)

        assertEquals(emptyMap<Long, Int>(), log.history)
    }
}
