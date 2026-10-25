package com.wortart.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local learning progress. The table holds exactly one row ([SINGLETON_ID]).
 * German Score is not stored; it is computed from these values.
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val streakCount: Int = 0,
    val cardsPlayed: Int = 0,
    /** Fraction of correct answers, 0f..1f. */
    val successRate: Float = 0f,
    val practiceTimeSeconds: Long = 0L,
    val perfectPronunciations: Int = 0,
    /** Last practice day as epoch day (LocalDate.toEpochDay()); null if never practiced. */
    val lastPracticeDate: Long? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
