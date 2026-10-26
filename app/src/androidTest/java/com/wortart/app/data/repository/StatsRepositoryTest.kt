package com.wortart.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.dao.UserStatsDao
import com.wortart.app.data.local.entity.UserStats
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
}
