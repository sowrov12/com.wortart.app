package com.wortart.app.domain.question

import com.wortart.app.data.local.entity.Auxiliaries
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.data.local.entity.isSeparable
import com.wortart.app.data.local.entity.kindKey
import kotlin.random.Random

enum class QuestionType { AUXILIARY, PREFIX, MEANING, QUIZ, BLANK, PARTIZIP, ANTONYM }

/** A multiple-choice question shown with a flashcard. */
data class ChoiceQuestion(
    val type: QuestionType,
    val prompt: String,
    val options: List<String>,
    val correctAnswer: String,
    /** Short explanation shown after answering, e.g. "Perfekt: ist gegangen". */
    val explanation: String,
    /** Shown instead of the German word until the answer is submitted, e.g. "_____rufen". */
    val titleBlank: String? = null,
    /** Shown instead of the German example until the answer is submitted (the example with a gap). */
    val exampleBlank: String? = null,
    /** Hides the meaning line of the card until the answer is submitted. */
    val hideMeaning: Boolean = false,
)

/** English and Bengali meaning on one line, e.g. "to shop / কিনতে যাওয়া" (first meaning of each). */
fun VocabEntity.shortMeaning(): String =
    "${translationEn.substringBefore(';').trim()} / ${translationBn.substringBefore(';').trim()}"

/** The card sets of the flashcards: which question types a group offers and how they are named. */
object PracticeSets {
    private val VERB_SETS = listOf(
        QuestionType.MEANING, QuestionType.AUXILIARY, QuestionType.BLANK, QuestionType.PARTIZIP,
    )

    /** The question types that make sense for a group (null = all groups). */
    fun forGroup(group: String?): List<QuestionType> = when (group) {
        null -> listOf(
            QuestionType.MEANING, QuestionType.AUXILIARY, QuestionType.BLANK,
            QuestionType.PARTIZIP, QuestionType.PREFIX, QuestionType.ANTONYM,
        )
        "Separable Verbs" -> VERB_SETS + QuestionType.PREFIX
        "Regular Verbs", "Irregular Verbs", "Modal Verbs", "Inseparable Verbs", "Reflexive Verbs",
        "Sein Verbs", "Dativ Verbs", "Prepositional Verbs" -> VERB_SETS
        "Adjectives" -> listOf(QuestionType.MEANING, QuestionType.BLANK, QuestionType.ANTONYM)
        else -> listOf(QuestionType.MEANING, QuestionType.BLANK)
    }

    /** Name of a set on the setup screen; null means "Mixed". */
    fun label(type: QuestionType?): String = when (type) {
        null -> "Mixed"
        QuestionType.MEANING -> "Meaning"
        QuestionType.AUXILIARY -> "Haben / Sein"
        QuestionType.BLANK -> "Fill the blank"
        QuestionType.PARTIZIP -> "Partizip II"
        QuestionType.PREFIX -> "Separable prefix"
        QuestionType.ANTONYM -> "Opposite word"
        QuestionType.QUIZ -> "Quiz"
    }
}

/**
 * Builds the question for a card.
 *
 * [build] is the quick default: haben/sein for verbs, the prefix for separable verbs, the meaning
 * for adjectives and adverbs. [buildSet] builds one specific set and [buildMixed] picks a random
 * set that fits the card. All builders return null when the card has no data for that question.
 */
object ChoiceQuestionBuilder {

    val COMMON_PREFIXES = listOf(
        "ab", "an", "auf", "aus", "ein", "mit", "nach", "vor", "zu", "weg", "zurück", "fest",
        "her", "hin", "um", "los", "zusammen", "vorbei", "fort", "heraus", "hinein", "dar",
    )

    private const val PREFIX_OPTION_COUNT = 4
    private const val MEANING_OPTION_COUNT = 4
    private const val GAP = "_____"

    /**
     * @param pool other words that can supply wrong answers for meaning questions
     *   (words of the same category are used). Not needed for verbs.
     */
    fun build(card: VocabEntity, pool: List<VocabEntity> = emptyList()): ChoiceQuestion? =
        when (card.category) {
            WordCategories.SEPARABLE_VERB -> prefixQuestion(card, requireGap = false)
            WordCategories.REGULAR_VERB, WordCategories.IRREGULAR_VERB, WordCategories.MODAL_VERB ->
                if (card.isSeparable()) {
                    prefixQuestion(card, requireGap = false) ?: auxiliaryQuestion(card)
                } else {
                    auxiliaryQuestion(card)
                }
            WordCategories.ADJECTIVE, WordCategories.ADVERB -> meaningQuestion(card, pool, byKind = false)
            WordCategories.PREPOSITION, WordCategories.CONJUNCTION, WordCategories.QUESTION_WORD ->
                meaningQuestion(card, pool, byKind = false)
            else -> null
        }

    /** Builds the question of one set, or null if this card does not have it. */
    fun buildSet(
        card: VocabEntity,
        type: QuestionType,
        pool: List<VocabEntity> = emptyList(),
    ): ChoiceQuestion? = when (type) {
        QuestionType.MEANING -> meaningQuestion(card, pool, byKind = true)
        QuestionType.AUXILIARY ->
            if (card.category in WordCategories.ALL_VERBS) auxiliaryQuestion(card) else null
        QuestionType.BLANK -> blankQuestion(card)
        QuestionType.PARTIZIP ->
            if (card.category in WordCategories.ALL_VERBS) partizipQuestion(card) else null
        QuestionType.PREFIX -> if (card.isSeparable()) prefixQuestion(card, requireGap = true) else null
        QuestionType.ANTONYM -> antonymQuestion(card)
        QuestionType.QUIZ -> null
    }

    /** Question types that can exist for this card. */
    fun applicableTypes(card: VocabEntity): List<QuestionType> = when {
        card.category in WordCategories.ALL_VERBS -> {
            val base = listOf(
                QuestionType.MEANING, QuestionType.AUXILIARY, QuestionType.BLANK, QuestionType.PARTIZIP,
            )
            if (card.isSeparable()) base + QuestionType.PREFIX else base
        }
        card.category == WordCategories.ADJECTIVE ->
            listOf(QuestionType.MEANING, QuestionType.BLANK, QuestionType.ANTONYM)
        else -> listOf(QuestionType.MEANING, QuestionType.BLANK)
    }

    /** A random set that works for this card. */
    fun buildMixed(card: VocabEntity, pool: List<VocabEntity>, random: Random): ChoiceQuestion? {
        for (type in applicableTypes(card).shuffled(random)) {
            val question = buildSet(card, type, pool)
            if (question != null) return question
        }
        return null
    }

    private fun splitOptions(raw: String?): List<String> =
        raw?.split('|')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct().orEmpty()

    private fun auxiliaryQuestion(card: VocabEntity): ChoiceQuestion? {
        val auxiliary = card.auxiliary
        if (auxiliary != Auxiliaries.HABEN && auxiliary != Auxiliaries.SEIN) return null

        return ChoiceQuestion(
            type = QuestionType.AUXILIARY,
            prompt = "Which auxiliary verb is correct?",
            options = listOf(Auxiliaries.HABEN, Auxiliaries.SEIN),
            correctAnswer = auxiliary,
            explanation = perfektExplanation(card, auxiliary),
        )
    }

    private fun prefixQuestion(card: VocabEntity, requireGap: Boolean): ChoiceQuestion? {
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

        val titleGap = card.prefixBlankTitle?.takeIf { it.isNotBlank() }
            ?: if (stem.isNotEmpty() && stem != card.word) GAP + stem else null
        val exampleGap = card.prefixBlankDe?.takeIf { it.isNotBlank() }
            ?: gapLastToken(card.exampleDe, prefix)
        if (requireGap && exampleGap == null) return null

        return ChoiceQuestion(
            type = QuestionType.PREFIX,
            prompt = "Select the separable prefix:",
            options = options,
            correctAnswer = prefix,
            explanation = explanation,
            titleBlank = titleGap,
            exampleBlank = exampleGap,
        )
    }

    /** Replaces the last standalone occurrence of [token] in [text] with a gap, or null. */
    private fun gapLastToken(text: String, token: String): String? {
        val pattern = Regex("(?<!\\p{L})" + Regex.escape(token) + "(?!\\p{L})", RegexOption.IGNORE_CASE)
        val match = pattern.findAll(text).lastOrNull() ?: return null
        return text.substring(0, match.range.first) + GAP + text.substring(match.range.last + 1)
    }

    private fun meaningQuestion(
        card: VocabEntity,
        pool: List<VocabEntity>,
        byKind: Boolean,
    ): ChoiceQuestion? {
        val correct = card.shortMeaning()

        val random = Random(card.id.hashCode())
        val distractors = pool
            .filter { other ->
                other.id != card.id &&
                    if (byKind) other.kindKey() == card.kindKey() else other.category == card.category
            }
            .map { it.shortMeaning() }
            .filter { it != correct }
            .distinct()
            .shuffled(random)
            .take(MEANING_OPTION_COUNT - 1)
        if (distractors.isEmpty()) return null

        return ChoiceQuestion(
            type = QuestionType.MEANING,
            prompt = "What is the correct meaning?",
            options = (distractors + correct).shuffled(random),
            correctAnswer = correct,
            explanation = "${card.word} = ${card.translationEn} / ${card.translationBn}",
            hideMeaning = true,
        )
    }

    private fun blankQuestion(card: VocabEntity): ChoiceQuestion? {
        val gapText = card.blankDe?.takeIf { it.isNotBlank() } ?: return null
        val answer = card.blankAnswer?.takeIf { it.isNotBlank() } ?: return null
        val wrong = splitOptions(card.blankWrong).filter { it != answer }
        if (wrong.size < 2) return null

        val random = Random(card.id.hashCode() + 7)
        return ChoiceQuestion(
            type = QuestionType.BLANK,
            prompt = "Fill in the blank:",
            options = (wrong.take(2) + answer).shuffled(random),
            correctAnswer = answer,
            explanation = card.exampleDe,
            // For verbs the infinitive stays visible; for other words it would give the answer away.
            titleBlank = if (card.category in WordCategories.ALL_VERBS) null else GAP,
            exampleBlank = gapText,
        )
    }

    private fun partizipQuestion(card: VocabEntity): ChoiceQuestion? {
        val answer = card.partizip2?.takeIf { it.isNotBlank() } ?: return null
        val wrong = splitOptions(card.pz2Wrong).filter { it != answer }
        if (wrong.size < 2) return null

        val random = Random(card.id.hashCode() + 11)
        return ChoiceQuestion(
            type = QuestionType.PARTIZIP,
            prompt = "Select the Partizip II:",
            options = (wrong.take(2) + answer).shuffled(random),
            correctAnswer = answer,
            explanation = perfektExplanation(card, card.auxiliary ?: Auxiliaries.HABEN),
        )
    }

    private fun antonymQuestion(card: VocabEntity): ChoiceQuestion? {
        if (card.category != WordCategories.ADJECTIVE) return null
        val antonym = card.antonym?.takeIf { it.isNotBlank() } ?: return null
        val meaning = card.antonymMeaning?.takeIf { it.isNotBlank() } ?: return null
        val wrong = card.antonymWrong?.takeIf { it.isNotBlank() } ?: return null

        val correct = "$antonym — $meaning"
        val random = Random(card.id.hashCode() + 3)
        return ChoiceQuestion(
            type = QuestionType.ANTONYM,
            prompt = "Select the opposite word:",
            options = listOf(correct, wrong).shuffled(random),
            correctAnswer = correct,
            explanation = "${card.word} ↔ $antonym",
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
