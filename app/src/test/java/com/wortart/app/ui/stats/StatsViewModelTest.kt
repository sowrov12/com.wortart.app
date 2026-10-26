package com.wortart.app.ui.stats

import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.data.repository.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private class FakeStatsRepository(initial: UserStats = UserStats()) : StatsRepository {
        val stats = MutableStateFlow(initial)

        override fun observeStats() = stats
        override suspend fun recordAnswer(isCorrect: Boolean) = Unit
        override suspend fun addPracticeTime(seconds: Long) = Unit
        override suspend fun recordPerfectPronunciation() = Unit
    }

    private val day = 20_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithAnEmptySummary() = runTest {
        val viewModel = StatsViewModel(FakeStatsRepository()) { day }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        val summary = viewModel.uiState.value
        assertEquals(0, summary.germanScore)
        assertEquals(0, summary.streakDays)
        assertEquals("0 min", summary.practiceTimeLabel)
    }

    @Test
    fun showsTheStoredStatsAsDisplayValues() = runTest {
        val repository = FakeStatsRepository(
            UserStats(
                cardsPlayed = 20,
                successRate = 0.5f,
                practiceTimeSeconds = 600,
                perfectPronunciations = 2,
                streakCount = 3,
                lastPracticeDate = day,
            ),
        )
        val viewModel = StatsViewModel(repository) { day }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        val summary = viewModel.uiState.value
        assertEquals(20, summary.cardsPlayed)
        assertEquals(50, summary.successPercent)
        assertEquals("10 min", summary.practiceTimeLabel)
        assertEquals(3, summary.streakDays)
        assertTrue(summary.practicedToday)
        // 10 correct * 10 + 2 pronunciations * 5 + 3 streak days * 20
        assertEquals(170, summary.germanScore)
    }

    @Test
    fun updatesWhenTheStatsChange() = runTest {
        val repository = FakeStatsRepository()
        val viewModel = StatsViewModel(repository) { day }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        repository.stats.value = UserStats(cardsPlayed = 4, successRate = 0.75f, streakCount = 1, lastPracticeDate = day)

        val summary = viewModel.uiState.value
        assertEquals(4, summary.cardsPlayed)
        assertEquals(75, summary.successPercent)
        assertEquals(3 * 10 + 20, summary.germanScore)
    }

    @Test
    fun streakIsBrokenWhenTheDayHasMovedOnTooFar() = runTest {
        val repository = FakeStatsRepository(UserStats(streakCount = 6, lastPracticeDate = day))
        var today = day + 3
        val viewModel = StatsViewModel(repository) { today }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        val summary = viewModel.uiState.value
        assertEquals(0, summary.streakDays)
        assertFalse(summary.practicedToday)
    }
}
