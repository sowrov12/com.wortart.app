package com.wortart.app.domain.word

import com.wortart.app.data.local.entity.Auxiliaries
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.data.local.entity.isSeparable

/** One block of the Word Details page below the word. */
sealed interface DetailSection {
    val title: String

    data class Table(
        override val title: String,
        val headers: List<String>,
        val rows: List<List<String>>,
    ) : DetailSection

    /** Label and value pairs, e.g. "Auxiliary verb" - "sein". */
    data class Facts(
        override val title: String,
        val items: List<Pair<String, String>>,
    ) : DetailSection
}

/**
 * What the Word Details page shows for a word.
 * [sections] only contains what the data supports; it is empty for words without extras.
 */
data class WordDetailsContent(
    val categoryLabel: String,
    val sections: List<DetailSection>,
) {
    val hasExtraDetails: Boolean get() = sections.isNotEmpty()
}

fun categoryLabel(card: VocabEntity): String = when (card.category) {
    WordCategories.REGULAR_VERB -> "Regular Verb"
    WordCategories.IRREGULAR_VERB -> "Irregular Verb"
    WordCategories.SEPARABLE_VERB -> "Separable Verb"
    WordCategories.MODAL_VERB -> "Modal Verb"
    WordCategories.PREPOSITION -> "Preposition"
    WordCategories.CONJUNCTION -> "Conjunction"
    WordCategories.QUESTION_WORD -> "Question Word"
    WordCategories.ADJECTIVE -> "Adjective"
    WordCategories.ADVERB -> if (card.topic == "Connectors") "Connector" else "Adverb"
    else -> card.category
}

fun buildWordDetails(card: VocabEntity): WordDetailsContent {
    val sections = when (card.category) {
        in WordCategories.ALL_VERBS -> verbSections(card)
        WordCategories.ADJECTIVE -> adjectiveSections(card)
        else -> oppositeSection(card)
    }
    return WordDetailsContent(categoryLabel(card), sections)
}

private fun verbSections(card: VocabEntity): List<DetailSection> = buildList {
    val present = PresentTense.forms(card)
    if (present != null) {
        add(
            DetailSection.Table(
                title = "Conjugation (Present)",
                headers = listOf("Pronoun", "Form"),
                rows = PresentTense.PRONOUNS.zip(present) { pronoun, form -> listOf(pronoun, form) },
            ),
        )
    }

    if (card.praeteritum != null || card.partizip2 != null) {
        add(
            DetailSection.Table(
                title = "Principal forms",
                headers = listOf("Infinitiv", "Präteritum", "Partizip II"),
                rows = listOf(listOf(card.word, card.praeteritum ?: "–", card.partizip2 ?: "–")),
            ),
        )
    }

    val auxiliary = card.auxiliary
    if (auxiliary != null) {
        val perfect = card.partizip2?.let { participle ->
            val finite = if (auxiliary == Auxiliaries.SEIN) "ist" else "hat"
            "$finite $participle"
        }
        add(
            DetailSection.Facts(
                title = "Perfect tense",
                items = listOfNotNull(
                    "Auxiliary verb" to auxiliary,
                    perfect?.let { "Perfekt" to it },
                ),
            ),
        )
    }

    val prefix = card.prefix
    if (card.isSeparable() && !prefix.isNullOrBlank()) {
        add(
            DetailSection.Facts(
                title = "Separable prefix",
                items = listOfNotNull(
                    "Prefix" to prefix,
                    present?.let { "Example" to "ich ${it.first()}" },
                ),
            ),
        )
    }
}

private fun adjectiveSections(card: VocabEntity): List<DetailSection> = buildList {
    val comparative = card.komparativ
    val superlative = card.superlativ
    if (comparative != null && superlative != null) {
        add(
            DetailSection.Table(
                title = "Comparison",
                headers = listOf("Positiv", "Komparativ", "Superlativ"),
                rows = listOf(listOf(card.word, comparative, superlative)),
            ),
        )
    }
    addAll(oppositeSection(card))
}

private fun oppositeSection(card: VocabEntity): List<DetailSection> {
    val antonym = card.antonym?.takeIf { it.isNotBlank() } ?: return emptyList()
    return listOf(DetailSection.Facts(title = "Opposite", items = listOf("Antonym" to antonym)))
}
