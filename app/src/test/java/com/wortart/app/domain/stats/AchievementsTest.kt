package com.wortart.app.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    private fun earned(summary: StatsSummary): Set<String> =
        Achievements.from(summary).filter { it.earned }.map { it.id }.toSet()

    @Test
    fun aNewLearnerHasEarnedNothing() {
        assertTrue(earned(StatsSummary()).isEmpty())
        assertEquals(5, Achievements.from(StatsSummary()).size)
    }

    @Test
    fun theFirstCardIsTheFirstMilestone() {
        assertEquals(setOf("first_card"), earned(StatsSummary(cardsPlayed = 1)))
    }

    @Test
    fun cardMilestonesBuildOnEachOther() {
        assertEquals(setOf("first_card", "ten_cards"), earned(StatsSummary(cardsPlayed = 10)))
        assertEquals(
            setOf("first_card", "ten_cards", "hundred_cards"),
            earned(StatsSummary(cardsPlayed = 100)),
        )
    }

    @Test
    fun correctAnswersAndPracticeTimeCountToo() {
        val summary = StatsSummary(
            cardsPlayed = 60,
            correctAnswers = 50,
            practiceTimeSeconds = 3600,
        )

        assertEquals(setOf("first_card", "ten_cards", "fifty_correct", "one_hour"), earned(summary))
    }

    @Test
    fun justBelowTheLimitsIsNotEnough() {
        val summary = StatsSummary(cardsPlayed = 9, correctAnswers = 49, practiceTimeSeconds = 3599)

        assertEquals(setOf("first_card"), earned(summary))
    }

    @Test
    fun everyAchievementIsDescribed() {
        Achievements.from(StatsSummary()).forEach {
            assertTrue(it.title.isNotBlank() && it.description.isNotBlank() && it.emoji.isNotBlank())
        }
        assertFalse(Achievements.from(StatsSummary()).map { it.id }.toSet().size < 5)
    }
}
