package com.wortart.app.ui.components

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.data.local.entity.WordGroups
import com.wortart.app.data.local.entity.groupLabels

/** Short grammar tag shown on a flashcard, e.g. "Verb | Separable". */
fun VocabEntity.grammarTag(): String = when (category) {
    WordCategories.REGULAR_VERB -> verbTag("Regular")
    WordCategories.IRREGULAR_VERB -> verbTag("Irregular")
    WordCategories.SEPARABLE_VERB -> "Verb | Separable"
    WordCategories.MODAL_VERB -> "Verb | Modal"
    WordCategories.ADJECTIVE -> "Adjective"
    WordCategories.ADVERB -> if (topic == "Connectors") "Connector" else "Adverb"
    WordCategories.PREPOSITION -> "Preposition"
    WordCategories.CONJUNCTION -> "Conjunction"
    WordCategories.QUESTION_WORD -> "Question word"
    else -> category
}

/** "Verb | Regular", plus Separable / Inseparable and Reflexive when the word has those tags. */
private fun VocabEntity.verbTag(kind: String): String {
    val labels = groupLabels()
    val parts = mutableListOf(kind)
    if (WordGroups.SEPARABLE in labels) parts += "Separable"
    if (WordGroups.INSEPARABLE in labels) parts += "Inseparable"
    if (WordGroups.REFLEXIVE in labels) parts += "Reflexive"
    return "Verb | " + parts.joinToString(" | ")
}
