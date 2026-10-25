package com.wortart.app.data.local.entity

/** Allowed values for [VocabEntity.category]. Adverbs also cover connectors (Konnektoren). */
object WordCategories {
    const val REGULAR_VERB = "regular_verb"
    const val IRREGULAR_VERB = "irregular_verb"
    const val SEPARABLE_VERB = "separable_verb"
    const val ADJECTIVE = "adjective"
    const val ADVERB = "adverb"

    val ALL = listOf(REGULAR_VERB, IRREGULAR_VERB, SEPARABLE_VERB, ADJECTIVE, ADVERB)
    val VERBS = listOf(REGULAR_VERB, IRREGULAR_VERB, SEPARABLE_VERB)
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
