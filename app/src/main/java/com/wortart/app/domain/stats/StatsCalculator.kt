package com.wortart.app.domain.stats

import com.wortart.app.data.local.entity.UserStats
import kotlin.math.roundToInt

/** Everything the Statistics screen shows, ready to display. */
data class StatsSummary(
    val germanScore: Int = 0,
    /** Current streak in days; 0 once a day was missed. */
    val streakDays: Int = 0,
    val practicedToday: Boolean = false,
    val cardsPlayed: Int = 0,
    val correctAnswers: Int = 0,
    val successPercent: Int = 0,
    val practiceTimeSeconds: Long = 0L,
    val perfectPronunciations: Int = 0,
) {
    val practiceTimeLabel: String get() = formatPracticeTime(practiceTimeSeconds)
}

/** "45 sec", "12 min", "1 h 05 min". */
fun formatPracticeTime(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    return when {
        seconds == 0L -> "0 min"
        seconds < 60L -> "$seconds sec"
        seconds < 3600L -> "${seconds / 60} min"
        else -> {
            val minutes = (seconds % 3600L) / 60L
            val paddedMinutes = if (minutes < 10) "0$minutes" else "$minutes"
            "${seconds / 3600L} h $paddedMinutes min"
        }
    }
}

/**
 * Pure rules for the learning statistics. Days are epoch days (`LocalDate.toEpochDay()`).
 *
 * - A practice day is a day on which at least one flashcard was answered.
 * - The streak grows by one for each consecutive practice day and restarts at 1 after a gap.
 * - German Score = correct answers × 10 + perfect pronunciations × 5 + current streak days × 20.
 */
object StatsCalculator {

    const val POINTS_PER_CORRECT_ANSWER = 10
    const val POINTS_PER_PERFECT_PRONUNCIATION = 5
    const val POINTS_PER_STREAK_DAY = 20

    /** One resumed stretch counts for at most this long, so a forgotten open screen cannot inflate the time. */
    const val MAX_PRACTICE_SEGMENT_SECONDS = 30L * 60L

    /** The number of correct answers, derived from the stored success rate. */
    fun correctAnswers(stats: UserStats): Int =
        (stats.successRate * stats.cardsPlayed).roundToInt().coerceIn(0, stats.cardsPlayed)

    /** The streak to show on [today]: it is broken (0) once a full day was skipped. */
    fun currentStreak(stats: UserStats, today: Long): Int {
        val last = stats.lastPracticeDate ?: return 0
        return if (today - last <= 1L) stats.streakCount else 0
    }

    fun germanScore(stats: UserStats, today: Long): Int =
        correctAnswers(stats) * POINTS_PER_CORRECT_ANSWER +
            stats.perfectPronunciations * POINTS_PER_PERFECT_PRONUNCIATION +
            currentStreak(stats, today) * POINTS_PER_STREAK_DAY

    fun summary(stats: UserStats, today: Long): StatsSummary {
        val correct = correctAnswers(stats)
        val percent = if (stats.cardsPlayed == 0) 0 else (correct * 100f / stats.cardsPlayed).roundToInt()
        return StatsSummary(
            germanScore = germanScore(stats, today),
            streakDays = currentStreak(stats, today),
            practicedToday = stats.lastPracticeDate == today,
            cardsPlayed = stats.cardsPlayed,
            correctAnswers = correct,
            successPercent = percent,
            practiceTimeSeconds = stats.practiceTimeSeconds,
            perfectPronunciations = stats.perfectPronunciations,
        )
    }

    /** Stats after one answered flashcard on [today]. */
    fun afterAnswer(stats: UserStats, isCorrect: Boolean, today: Long): UserStats {
        val played = stats.cardsPlayed + 1
        val correct = correctAnswers(stats) + if (isCorrect) 1 else 0

        val last = stats.lastPracticeDate
        val streak = when {
            last == null -> 1
            last == today -> maxOf(stats.streakCount, 1)
            today - last == 1L -> stats.streakCount + 1
            else -> 1
        }

        return stats.copy(
            cardsPlayed = played,
            successRate = correct.toFloat() / played,
            streakCount = streak,
            lastPracticeDate = today,
        )
    }

    /** Stats after [seconds] of practice. Non-positive values are ignored. */
    fun afterPracticeTime(stats: UserStats, seconds: Long): UserStats {
        if (seconds <= 0L) return stats
        val counted = seconds.coerceAtMost(MAX_PRACTICE_SEGMENT_SECONDS)
        return stats.copy(practiceTimeSeconds = stats.practiceTimeSeconds + counted)
    }

    fun afterPerfectPronunciation(stats: UserStats): UserStats =
        stats.copy(perfectPronunciations = stats.perfectPronunciations + 1)
}
