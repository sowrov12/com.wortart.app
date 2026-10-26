package com.wortart.app.domain.question

import com.wortart.app.data.local.entity.Auxiliaries
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import kotlin.random.Random

enum class QuestionType { AUXILIARY, PREFIX }

/** A multiple-choice question shown under a flashcard. */
data class ChoiceQuestion(
    val type: QuestionType,
    val prompt: String,
    val options: List<String>,
    val correctAnswer: String,
    /** Short explanation shown after answering, e.g. "Perfekt: ist gegangen". */
    val explanation: String,
)

/**
 * Builds the choice question for a card:
 * - regular / irregular verbs: haben or sein
 * - separable verbs: pick the separable prefix
 * - other word types: no question yet (returns null)
 */
object ChoiceQuestionBuilder {

    val COMMON_PREFIXES = listOf(
        "ab", "an", "auf", "aus", "ein", "mit", "nach", "vor", "zu", "weg", "zurück", "fest",
    )

    private const val PREFIX_OPTION_COUNT = 4

    fun build(card: VocabEntity): ChoiceQuestion? = when (card.category) {
        WordCategories.REGULAR_VERB, WordCategories.IRREGULAR_VERB -> auxiliaryQuestion(card)
        WordCategories.SEPARABLE_VERB -> prefixQuestion(card)
        else -> null
    }

    private fun auxiliaryQuestion(card: VocabEntity): ChoiceQuestion? {
        val auxiliary = card.auxiliary
        if (auxiliary != Auxiliaries.HABEN && auxiliary != Auxiliaries.SEIN) return null

        return ChoiceQuestion(
            type = QuestionType.AUXILIARY,
            prompt = "Which auxiliary verb does “${card.word}” use in the Perfekt?",
            options = listOf(Auxiliaries.HABEN, Auxiliaries.SEIN),
            correctAnswer = auxiliary,
            explanation = perfektExplanation(card, auxiliary),
        )
    }

    private fun prefixQuestion(card: VocabEntity): ChoiceQuestion? {
        val prefix = card.prefix?.takeIf { it.isNotBlank() } ?: return null

        // Seeded by the card id so the option order stays the same on every recomposition.
        val random = Random(card.id.hashCode())
        val distractors = COMMON_PREFIXES
            .filter { it != prefix }
            .shuffled(random)
            .take(PREFIX_OPTION_COUNT - 1)
        val options = (distractors + prefix).shuffled(random)

        val stem = card.word.removePrefix(prefix)
        val explanation = if (stem.isNotEmpty() && stem != card.word) {
            "$prefix + $stem"
        } else {
            "The separable prefix of “${card.word}” is “$prefix”."
        }

        return ChoiceQuestion(
            type = QuestionType.PREFIX,
            prompt = "What is the separable prefix of “${card.word}”?",
            options = options,
            correctAnswer = prefix,
            explanation = explanation,
        )
    }

    private fun perfektExplanation(card: VocabEntity, auxiliary: String): String {
        val participle = card.partizip2
        val finite = if (auxiliary == Auxiliaries.SEIN) "ist" else "hat"
        return if (participle != null) {
            "Perfekt: $finite $participle"
        } else {
            "“${card.word}” uses $auxiliary in the Perfekt."
        }
    }
}
