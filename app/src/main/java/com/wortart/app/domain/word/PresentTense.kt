package com.wortart.app.domain.word

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories

/**
 * Builds the present tense (Präsens) of a verb.
 *
 * Regular verbs follow the endings -e, -st, -t, -en, -t, -en (with -est / -et after d or t,
 * and the special forms of verbs on -eln and -ern). A verb whose vowel changes needs its
 * er-form in `stammform` (for example "er fährt"); the du-form is derived from it.
 * Separable verbs get the prefix at the end: "kaufe … ein".
 */
object PresentTense {

    val PRONOUNS = listOf("ich", "du", "er/sie/es", "wir", "ihr", "sie/Sie")

    /**
     * The six forms in the order of [PRONOUNS], or null when they cannot be built reliably
     * (not a verb, an irregular verb without its er-form, or an unusual infinitive).
     */
    fun forms(card: VocabEntity): List<String>? {
        if (card.category !in WordCategories.VERBS) return null
        // Reflexive and prepositional verbs ("sich waschen", "warten auf") need more than the simple pattern.
        if (card.word.contains(' ')) return null
        // An irregular verb may change its vowel. Without the er-form the du-form would be a guess.
        if (card.category == WordCategories.IRREGULAR_VERB && card.stammform.isNullOrBlank()) return null

        val prefix = card.prefix?.takeIf { it.isNotBlank() }
        val base = if (prefix != null && card.word.startsWith(prefix)) card.word.removePrefix(prefix) else card.word

        val erVerb: String? = card.stammform?.takeIf { it.isNotBlank() }?.let { text ->
            val tokens = text.trim().split(Regex("\\s+"))
            if (tokens.size < 2 || tokens[0] != "er") return null
            tokens[1]
        }

        val ich: String
        var du: String
        var er: String
        val ihr: String
        when {
            base.endsWith("eln") -> {
                val stem = base.dropLast(1)
                ich = base.dropLast(3) + "le"
                du = stem + "st"
                er = stem + "t"
                ihr = stem + "t"
            }
            base.endsWith("ern") -> {
                val stem = base.dropLast(1)
                ich = stem + "e"
                du = stem + "st"
                er = stem + "t"
                ihr = stem + "t"
            }
            base.endsWith("en") && base.length > 3 -> {
                val stem = base.dropLast(2)
                val needsE = stem.last() == 'd' || stem.last() == 't'
                ich = stem + "e"
                er = stem + if (needsE) "et" else "t"
                ihr = er
                du = when {
                    stem.last() in "sßzx" -> stem + "t"
                    needsE -> stem + "est"
                    else -> stem + "st"
                }
            }
            else -> return null
        }

        if (erVerb != null) {
            when {
                erVerb.endsWith("et") && erVerb.length > 3 -> {
                    du = erVerb.dropLast(2) + "est"
                    er = erVerb
                }
                erVerb.endsWith("t") && erVerb.length > 2 -> {
                    val core = erVerb.dropLast(1)
                    du = core + if (core.last() in "sßzx") "t" else "st"
                    er = erVerb
                }
                else -> return null
            }
        }

        val tail = if (prefix != null) " … $prefix" else ""
        return listOf(ich, du, er, base, ihr, base).map { it + tail }
    }
}
