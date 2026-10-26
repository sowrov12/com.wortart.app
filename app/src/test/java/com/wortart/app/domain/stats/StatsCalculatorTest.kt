package com.wortart.app.domain.stats

import com.wortart.app.data.local.entity.UserStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCalculatorTest {

    private val day = 20_000L

    private fun answer(stats: UserStats, isCorrect: Boolean, today: Long = day) =
        StatsCalculator.afterAnswer(stats, isCorrect, today)

    // ---- Answers and success rate ----

    @Test
    fun firstCorrectAnswerStartsTheStreak() {
        val stats = answer(UserStats(), isCorrect = true)

        assertEquals(1, stats.cardsPlayed)
        assertEquals(1f, stats.successRate, 0.0001f)
        assertEquals(1, stats.streakCount)
        assertEquals(day, stats.lastPracticeDate)
    }

    @Test
    fun firstWrongAnswerStillCountsAsPracticeButHasNoSuccess() {
        val stats = answer(UserStats(), isCorrect = false)

        assertEquals(1, stats.cardsPlayed)
        assertEquals(0f, stats.successRate, 0.0001f)
        assertEquals(1, stats.streakCount)
    }

    @Test
    fun successRateIsTheShareOfCorrectAnswers() {
        var stats = UserStats()
        listOf(true, true, false, true).forEach { stats = answer(stats, it) }

        assertEquals(4, stats.cardsPlayed)
        assertEquals(0.75f, stats.successRate, 0.0001f)
        assertEquals(3, StatsCalculator.correctAnswers(stats))
    }

    @Test
    fun correctAnswerCountStaysExactOverManyAnswers() {
        var stats = UserStats()
        var expectedCorrect = 0
        for (i in 1..300) {
            val isCorrect = i % 3 == 0
            if (isCorrect) expectedCorrect++
            stats = answer(stats, isCorrect)
            assertEquals("after $i answers", expectedCorrect, StatsCalculator.correctAnswers(stats))
        }
    }

    // ---- Streak ----

    @Test
    fun answeringTwiceOnTheSameDayKeepsTheStreak() {
        var stats = answer(UserStats(), true)
        stats = answer(stats, true)

        assertEquals(1, stats.streakCount)
    }

    @Test
    fun consecutiveDaysExtendTheStreak() {
        var stats = answer(UserStats(), true, today = day)
        stats = answer(stats, true, today = day + 1)
        stats = answer(stats, false, today = day + 2)

        assertEquals(3, stats.streakCount)
        assertEquals(day + 2, stats.lastPracticeDate)
    }

    @Test
    fun missingADayRestartsTheStreakAtOne() {
        var stats = answer(UserStats(), true, today = day)
        stats = answer(stats, true, today = day + 1)
        stats = answer(stats, true, today = day + 4)

        assertEquals(1, stats.streakCount)
    }

    @Test
    fun deviceClockSetBackRestartsTheStreak() {
        val stats = answer(UserStats(streakCount = 5, lastPracticeDate = day + 5), true, today = day)

        assertEquals(1, stats.streakCount)
        assertEquals(day, stats.lastPracticeDate)
    }

    @Test
    fun displayedStreakIsZeroOnceADayWasSkipped() {
        val stats = UserStats(streakCount = 5, lastPracticeDate = day)

        assertEquals(5, StatsCalculator.currentStreak(stats, today = day))
        assertEquals(5, StatsCalculator.currentStreak(stats, today = day + 1))
        assertEquals(0, StatsCalculator.currentStreak(stats, today = day + 2))
        assertEquals(0, StatsCalculator.currentStreak(UserStats(), today = day))
    }

    // ---- German Score ----

    @Test
    fun germanScoreAddsCorrectAnswersPronunciationsAndStreak() {
        val stats = UserStats(
            cardsPlayed = 10,
            successRate = 0.8f,
            perfectPronunciations = 3,
            streakCount = 4,
            lastPracticeDate = day,
        )

        // 8 correct * 10 + 3 pronunciations * 5 + 4 streak days * 20
        assertEquals(175, StatsCalculator.germanScore(stats, today = day))
    }

    @Test
    fun brokenStreakNoLongerAddsScore() {
        val stats = UserStats(
            cardsPlayed = 10,
            successRate = 0.8f,
            perfectPronunciations = 3,
            streakCount = 4,
            lastPracticeDate = day,
        )

        assertEquals(95, StatsCalculator.germanScore(stats, today = day + 5))
    }

    // ---- Summary ----

    @Test
    fun summaryContainsTheDisplayValues() {
        val stats = UserStats(
            cardsPlayed = 10,
            successRate = 0.8f,
            practiceTimeSeconds = 3900,
            perfectPronunciations = 3,
            streakCount = 4,
            lastPracticeDate = day,
        )

        val summary = StatsCalculator.summary(stats, today = day)

        assertEquals(175, summary.germanScore)
        assertEquals(4, summary.streakDays)
        assertTrue(summary.practicedToday)
        assertEquals(10, summary.cardsPlayed)
        assertEquals(8, summary.correctAnswers)
        assertEquals(80, summary.successPercent)
        assertEquals("1 h 05 min", summary.practiceTimeLabel)
        assertEquals(3, summary.perfectPronunciations)
    }

    @Test
    fun practicedTodayIsFalseTheNextDay() {
        val stats = UserStats(streakCount = 2, lastPracticeDate = day)

        assertFalse(StatsCalculator.summary(stats, today = day + 1).practicedToday)
    }

    @Test
    fun emptyStatsGiveAZeroSummary() {
        assertEquals(StatsSummary(), StatsCalculator.summary(UserStats(), today = day))
    }

    // ---- Practice time and pronunciations ----

    @Test
    fun practiceTimeIsAdded() {
        var stats = StatsCalculator.afterPracticeTime(UserStats(), 90)
        stats = StatsCalculator.afterPracticeTime(stats, 30)

        assertEquals(120L, stats.practiceTimeSeconds)
    }

    @Test
    fun zeroOrNegativePracticeTimeIsIgnored() {
        val stats = UserStats(practiceTimeSeconds = 50)

        assertEquals(stats, StatsCalculator.afterPracticeTime(stats, 0))
        assertEquals(stats, StatsCalculator.afterPracticeTime(stats, -10))
    }

    @Test
    fun oneStretchOfPracticeIsCappedAtThirtyMinutes() {
        val stats = StatsCalculator.afterPracticeTime(UserStats(), 5 * 3600L)

        assertEquals(StatsCalculator.MAX_PRACTICE_SEGMENT_SECONDS, stats.practiceTimeSeconds)
    }

    @Test
    fun perfectPronunciationsAreCounted() {
        val stats = StatsCalculator.afterPerfectPronunciation(
            StatsCalculator.afterPerfectPronunciation(UserStats()),
        )

        assertEquals(2, stats.perfectPronunciations)
    }

    @Test
    fun practiceTimeIsFormattedForDisplay() {
        assertEquals("0 min", formatPracticeTime(0))
        assertEquals("45 sec", formatPracticeTime(45))
        assertEquals("1 min", formatPracticeTime(60))
        assertEquals("59 min", formatPracticeTime(3599))
        assertEquals("1 h 00 min", formatPracticeTime(3600))
        assertEquals("1 h 05 min", formatPracticeTime(3900))
        assertEquals("2 h 01 min", formatPracticeTime(7260))
        assertEquals("0 min", formatPracticeTime(-5))
    }
}
