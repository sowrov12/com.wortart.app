package com.wortart.app.domain.stats

/** A milestone shown on the Statistics screen. */
data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val earned: Boolean,
)

/**
 * Milestones that are worked out from the statistics alone. Every one is based on a running
 * total, so once earned it stays earned.
 */
object Achievements {

    const val TEN_CARDS = 10
    const val HUNDRED_CARDS = 100
    const val FIFTY_CORRECT = 50
    const val ONE_HOUR_SECONDS = 3600L

    fun from(summary: StatsSummary): List<Achievement> = listOf(
        Achievement("first_card", "🌱", "First Card", "Answer your first flashcard", summary.cardsPlayed >= 1),
        Achievement("ten_cards", "📚", "10 Cards", "Answer $TEN_CARDS flashcards", summary.cardsPlayed >= TEN_CARDS),
        Achievement("hundred_cards", "🏆", "100 Cards", "Answer $HUNDRED_CARDS flashcards", summary.cardsPlayed >= HUNDRED_CARDS),
        Achievement("fifty_correct", "🎯", "50 Correct", "Give $FIFTY_CORRECT correct answers", summary.correctAnswers >= FIFTY_CORRECT),
        Achievement("one_hour", "⏱️", "1 Hour", "Practice for one hour", summary.practiceTimeSeconds >= ONE_HOUR_SECONDS),
        Achievement("streak_3", "🔥", "3-Day Streak", "Practice 3 days in a row", summary.streakDays >= 3),
        Achievement("streak_7", "🔥", "7-Day Streak", "Practice 7 days in a row", summary.streakDays >= 7),
        Achievement("hundred_correct", "✅", "100 Correct", "Give 100 correct answers", summary.correctAnswers >= 100),
        Achievement("five_hundred_cards", "🃏", "500 Cards", "Answer 500 flashcards", summary.cardsPlayed >= 500),
        Achievement("thousand_score", "⭐", "1000 Score", "Reach a German Score of 1000", summary.germanScore >= 1000),
        Achievement("streak_15", "🔥", "15-Day Streak", "Practice 15 days in a row", summary.streakDays >= 15),
        Achievement("streak_30", "🔥", "30-Day Streak", "Practice 30 days in a row", summary.streakDays >= 30),
    )
}
