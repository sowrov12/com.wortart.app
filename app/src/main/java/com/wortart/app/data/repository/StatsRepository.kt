package com.wortart.app.data.repository

import com.wortart.app.data.local.entity.UserStats
import kotlinx.coroutines.flow.Flow

/** Reads and updates the learner's progress. */
interface StatsRepository {

    /** The current stats; emits default (all zero) stats until the first update. */
    fun observeStats(): Flow<UserStats>

    /** Counts one answered flashcard and updates success rate and streak. */
    suspend fun recordAnswer(isCorrect: Boolean)

    /** Adds [seconds] of practice time. */
    suspend fun addPracticeTime(seconds: Long)

    suspend fun recordPerfectPronunciation()
}
