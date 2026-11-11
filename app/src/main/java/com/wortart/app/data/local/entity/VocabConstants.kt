package com.wortart.app.data.local.entity

/** Allowed values for [VocabEntity.category]. Adverbs also cover connectors (Konnektoren). */
object WordCategories {
    const val REGULAR_VERB = "regular_verb"
    const val IRREGULAR_VERB = "irregular_verb"
    const val SEPARABLE_VERB = "separable_verb"
    const val MODAL_VERB = "modal_verb"
    const val ADJECTIVE = "adjective"
    const val ADVERB = "adverb"
    const val PREPOSITION = "preposition"
    const val CONJUNCTION = "conjunction"
    const val QUESTION_WORD = "question_word"

    val ALL = listOf(
        REGULAR_VERB, IRREGULAR_VERB, SEPARABLE_VERB, MODAL_VERB,
        ADJECTIVE, ADVERB, PREPOSITION, CONJUNCTION, QUESTION_WORD,
    )
    val VERBS = listOf(REGULAR_VERB, IRREGULAR_VERB, SEPARABLE_VERB)

    /** Every category that is a verb, including modal verbs. */
    val ALL_VERBS = listOf(REGULAR_VERB, IRREGULAR_VERB, SEPARABLE_VERB, MODAL_VERB)
}

/**
 * Group labels used for the Explore categories and the flashcard category filter.
 * A word has one primary group ([VocabEntity.topic]) and can belong to more groups through
 * [VocabEntity.tags], e.g. a regular verb that is also separable and reflexive.
 */
object WordGroups {
    const val REGULAR = "Regular Verbs"
    const val IRREGULAR = "Irregular Verbs"
    const val MODAL = "Modal Verbs"
    const val SEPARABLE = "Separable Verbs"
    const val INSEPARABLE = "Inseparable Verbs"
    const val REFLEXIVE = "Reflexive Verbs"
    const val SEIN = "Sein Verbs"
    const val DATIV_VERBS = "Dativ Verbs"
    const val PREP_VERBS = "Prepositional Verbs"
    const val ADJECTIVES = "Adjectives"
    const val ADVERBS = "Adverbs"
    const val PREPOSITIONS = "Prepositions"
    const val CONJUNCTIONS = "Conjunctions"
    const val QUESTION_WORDS = "Question Words"

    val ORDER = listOf(
        REGULAR, IRREGULAR, MODAL, SEPARABLE, INSEPARABLE, REFLEXIVE, SEIN, DATIV_VERBS, PREP_VERBS,
        ADJECTIVES, ADVERBS,
        PREPOSITIONS, "Akkusativ Prepositions", "Dativ Prepositions", "Genitiv Prepositions",
        "Wechselpräpositionen", "Temporal Prepositions",
        CONJUNCTIONS, "Coordinating Conjunctions", "Subordinating Conjunctions", "Two-part Conjunctions",
        QUESTION_WORDS,
    )
}

/** Allowed values for [VocabEntity.level]. */
object CefrLevels {
    const val A1 = "A1"
    const val A2 = "A2"
    const val B1 = "B1"
    const val B2 = "B2"

    val ALL = listOf(A1, A2, B1, B2)
}

/** Allowed values for [VocabEntity.auxiliary]. */
object Auxiliaries {
    const val HABEN = "haben"
    const val SEIN = "sein"
}
