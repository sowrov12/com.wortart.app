package com.wortart.app.domain.rules.lessons

import com.wortart.app.domain.question.ChoiceQuestion
import com.wortart.app.domain.question.QuestionType

/** The emoji shown for the Material icon name of a lesson. Unknown names get a book. */
fun lessonEmoji(icon: String): String = when (icon) {
    "menu_book" -> "📖"
    "category" -> "🗂️"
    "filter_none" -> "🔢"
    "people" -> "👥"
    "schedule" -> "⏰"
    "bolt" -> "⚡"
    "reorder" -> "🔀"
    "call_split" -> "✂️"
    "link" -> "🔗"
    "handshake" -> "🤝"
    "tune" -> "🎛️"
    "history" -> "🕘"
    "construction" -> "🛠️"
    "access_time" -> "🕒"
    "block" -> "🚫"
    "trending_up" -> "📈"
    else -> "📚"
}

private val HEX_COLOR = Regex("^#([0-9A-Fa-f]{6})$")

/** "#4C8DFF" as an opaque ARGB value (0xFF4C8DFF), or null if the text is not a #RRGGBB color. */
fun parseHexColor(hex: String): Long? {
    val match = HEX_COLOR.matchEntire(hex.trim()) ?: return null
    return 0xFF000000L or match.groupValues[1].toLong(16)
}

/** "der", "die" or "das" when [text] starts with that article, otherwise null. */
fun articleOf(text: String): String? {
    val firstWord = text.trimStart().substringBefore(' ').lowercase()
    return firstWord.takeIf { it == "der" || it == "die" || it == "das" }
}

/** The quiz as a [ChoiceQuestion] with a two-language explanation, or null if its answer index is invalid. */
fun QuizItem.toChoiceQuestion(): ChoiceQuestion? {
    val correct = options.getOrNull(answer) ?: return null
    return ChoiceQuestion(
        type = QuestionType.QUIZ,
        prompt = q,
        options = options,
        correctAnswer = correct,
        explanation = "${why.en}\n${why.bn}",
    )
}
