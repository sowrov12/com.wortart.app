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
    )
}
