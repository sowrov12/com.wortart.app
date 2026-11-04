package com.wortart.app.domain.word

import com.wortart.app.data.local.entity.VocabEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDetailsContentTest {

    private fun entity(
        word: String,
        category: String,
        topic: String = "General",
        auxiliary: String? = null,
        praeteritum: String? = null,
        partizip2: String? = null,
        prefix: String? = null,
        stammform: String? = null,
        komparativ: String? = null,
        superlativ: String? = null,
        antonym: String? = null,
    ) = VocabEntity(
        id = word,
        word = word,
        category = category,
        level = "A1",
        topic = topic,
        translationEn = "en",
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = auxiliary,
        praeteritum = praeteritum,
        partizip2 = partizip2,
        prefix = prefix,
        stammform = stammform,
        komparativ = komparativ,
        superlativ = superlativ,
        antonym = antonym,
    )

    private fun DetailSection.table() = this as DetailSection.Table
    private fun DetailSection.facts() = this as DetailSection.Facts

    private val gehen = entity(
        "gehen", "irregular_verb", auxiliary = "sein", praeteritum = "ging",
        partizip2 = "gegangen", stammform = "er geht",
    )

    @Test
    fun categoryLabelsNameTheWordType() {
        assertEquals("Regular Verb", categoryLabel(entity("machen", "regular_verb")))
        assertEquals("Irregular Verb", categoryLabel(gehen))
        assertEquals("Separable Verb", categoryLabel(entity("einkaufen", "separable_verb")))
        assertEquals("Adjective", categoryLabel(entity("schnell", "adjective")))
        assertEquals("Adverb", categoryLabel(entity("oft", "adverb", topic = "Time")))
        assertEquals("Connector", categoryLabel(entity("weil", "adverb", topic = "Connectors")))
    }

    @Test
    fun anIrregularVerbShowsConjugationPrincipalFormsAndThePerfectTense() {
        val content = buildWordDetails(gehen)

        assertEquals("Irregular Verb", content.categoryLabel)
        assertEquals(
            listOf("Conjugation (Present)", "Principal forms", "Perfect tense"),
            content.sections.map { it.title },
        )

        val conjugation = content.sections[0].table()
        assertEquals(listOf("Pronoun", "Form"), conjugation.headers)
        assertEquals(listOf("ich", "gehe"), conjugation.rows.first())
        assertEquals(listOf("du", "gehst"), conjugation.rows[1])

        assertEquals(
            listOf(listOf("gehen", "ging", "gegangen")),
            content.sections[1].table().rows,
        )
        assertEquals(
            listOf("Auxiliary verb" to "sein", "Perfekt" to "ist gegangen"),
            content.sections[2].facts().items,
        )
    }

    @Test
    fun aHabenVerbUsesHatInThePerfect() {
        val content = buildWordDetails(
            entity("essen", "irregular_verb", auxiliary = "haben", partizip2 = "gegessen", stammform = "er isst"),
        )

        val perfect = content.sections.first { it.title == "Perfect tense" }.facts()
        assertEquals("hat gegessen", perfect.items.last().second)
    }

    @Test
    fun aSeparableVerbExplainsItsPrefix() {
        val content = buildWordDetails(
            entity("einkaufen", "separable_verb", auxiliary = "haben", prefix = "ein", partizip2 = "eingekauft"),
        )

        val prefix = content.sections.first { it.title == "Separable prefix" }.facts()
        assertEquals(listOf("Prefix" to "ein", "Example" to "ich kaufe … ein"), prefix.items)
        assertEquals("kaufe … ein", content.sections.first().table().rows.first()[1])
    }

    @Test
    fun anAdjectiveShowsTheComparisonAndItsOpposite() {
        val content = buildWordDetails(
            entity("schnell", "adjective", komparativ = "schneller", superlativ = "am schnellsten", antonym = "langsam"),
        )

        assertEquals(listOf("Comparison", "Opposite"), content.sections.map { it.title })
        assertEquals(
            listOf(listOf("schnell", "schneller", "am schnellsten")),
            content.sections[0].table().rows,
        )
        assertEquals(listOf("Antonym" to "langsam"), content.sections[1].facts().items)
    }

    @Test
    fun anAdverbShowsOnlyItsOppositeWhenItHasOne() {
        val withOpposite = buildWordDetails(entity("oft", "adverb", antonym = "selten"))
        assertEquals(listOf("Opposite"), withOpposite.sections.map { it.title })
        assertTrue(withOpposite.hasExtraDetails)

        val without = buildWordDetails(entity("weil", "adverb", topic = "Connectors"))
        assertTrue(without.sections.isEmpty())
        assertFalse(without.hasExtraDetails)
    }

    @Test
    fun missingDataLeavesOutTheMatchingSection() {
        val content = buildWordDetails(entity("laufen", "irregular_verb"))

        assertTrue(content.sections.isEmpty())
        assertFalse(content.hasExtraDetails)
    }

    @Test
    fun blankOppositesAreIgnored() {
        assertTrue(buildWordDetails(entity("oft", "adverb", antonym = "  ")).sections.isEmpty())
    }
}
