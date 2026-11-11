package com.wortart.app.data.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.domain.stats.ActivityLog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrefsActivityLogTest {

    private val today = 20_000L
    private lateinit var context: Context
    private lateinit var log: ActivityLog

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PrefsActivityLog(context).clear()
        log = PrefsActivityLog(context)
    }

    @After
    fun tearDown() {
        PrefsActivityLog(context).clear()
    }

    @Test
    fun anEmptyLogShowsAWeekOfZeros() {
        assertEquals(List(7) { 0 }, log.lastDays(today).map { it.answers })
    }

    @Test
    fun answersAreCountedPerDay() {
        log.recordAnswer(today)
        log.recordAnswer(today)
        log.recordAnswer(today - 1)

        assertEquals(listOf(0, 0, 0, 0, 0, 1, 2), log.lastDays(today).map { it.answers })
    }

    @Test
    fun theCountsSurviveAnotherInstanceLikeARestart() {
        log.recordAnswer(today)

        val afterRestart = PrefsActivityLog(context)

        assertEquals(1, afterRestart.lastDays(today).last().answers)
    }

    @Test
    fun clearForgetsEverything() {
        log.recordAnswer(today)

        log.clear()

        assertEquals(List(7) { 0 }, log.lastDays(today).map { it.answers })
    }
}
