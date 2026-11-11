package com.wortart.app.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityHistoryTest {

    private val today = 20_000L // a Friday

    @Test
    fun theHistoryIsReadAndWrittenAsText() {
        val history = mapOf(19_999L to 5, 20_000L to 3)

        assertEquals("19999:5,20000:3", ActivityHistory.encode(history))
        assertEquals(history, ActivityHistory.decode("19999:5,20000:3"))
    }

    @Test
    fun brokenPartsAreSkipped() {
        val history = ActivityHistory.decode("abc,19999:5,20000,20001:x,20002:0,:3,20003:2:1,20004:4")

        assertEquals(mapOf(19_999L to 5, 20_004L to 4), history)
    }

    @Test
    fun emptyTextGivesAnEmptyHistory() {
        assertTrue(ActivityHistory.decode(null).isEmpty())
        assertTrue(ActivityHistory.decode("").isEmpty())
        assertTrue(ActivityHistory.decode("   ").isEmpty())
    }

    @Test
    fun answersAreAddedToTheDay() {
        var history = emptyMap<Long, Int>()
        history = ActivityHistory.add(history, today)
        history = ActivityHistory.add(history, today)
        history = ActivityHistory.add(history, today - 1, count = 4)

        assertEquals(mapOf(today to 2, today - 1 to 4), history)
    }

    @Test
    fun oldDaysAreForgotten() {
        val history = mapOf(today - 100 to 9, today - 59 to 2)

        val updated = ActivityHistory.add(history, today)

        assertEquals(mapOf(today - 59 to 2, today to 1), updated)
    }

    @Test
    fun theLastSevenDaysEndTodayAndFillGapsWithZero() {
        val week = ActivityHistory.lastDays(mapOf(today to 7, today - 2 to 3, today - 10 to 99), today)

        assertEquals(7, week.size)
        assertEquals(today - 6, week.first().epochDay)
        assertEquals(today, week.last().epochDay)
        assertEquals(listOf(0, 0, 0, 0, 3, 0, 7), week.map { it.answers })
    }

    @Test
    fun aShorterOrLongerRangeWorksToo() {
        assertEquals(3, ActivityHistory.lastDays(emptyMap(), today, days = 3).size)
        assertEquals(14, ActivityHistory.lastDays(emptyMap(), today, days = 14).size)
    }

    @Test
    fun daysAreNamedInEnglish() {
        assertEquals("F", ActivityHistory.dayInitial(today))
        assertEquals("Fri", ActivityHistory.dayName(today))
        assertEquals("Sat", ActivityHistory.dayName(today + 1))
        assertEquals("M", ActivityHistory.dayInitial(today + 3))
    }

    @Test
    fun theNoOpLogAlwaysShowsAnEmptyWeek() {
        NoActivityLog.recordAnswer(today)

        assertEquals(List(7) { 0 }, NoActivityLog.lastDays(today).map { it.answers })
    }
}
