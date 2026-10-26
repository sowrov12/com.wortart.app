package com.wortart.app.ui.components

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories

/** Short grammar tag shown on a flashcard, e.g. "Verb | Separable". */
fun VocabEntity.grammarTag(): String = when (category) {
    WordCategories.REGULAR_VERB -> "Verb | Regular"
    WordCategories.IRREGULAR_VERB -> "Verb | Irregular"
    WordCategories.SEPARABLE_VERB -> "Verb | Separable"
    WordCategories.ADJECTIVE -> "Adjective"
    WordCategories.ADVERB -> if (topic == "Connectors") "Connector" else "Adverb"
    else -> category
}
