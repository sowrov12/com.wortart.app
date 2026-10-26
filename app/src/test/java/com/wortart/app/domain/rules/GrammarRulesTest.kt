package com.wortart.app.domain.rules

import com.wortart.app.data.local.entity.VocabEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarRulesTest {

    private fun verb(word: String, praeteritum: String?, partizip2: String?, auxiliary: String?) =
        VocabEntity(
            id = word,
            word = word,
            category = "irregular_verb",
            level = "A1",
            topic = "General",
            translationEn = "en",
            translationBn = "bn",
            exampleDe = "de",
            exampleEn = "en",
            exampleBn = "bn",
            auxiliary = auxiliary,
            praeteritum = praeteritum,
            partizip2 = partizip2,
        )

    private fun adjective(word: String, komparativ: String?, superlativ: String?) = VocabEntity(
        id = word,
        word = word,
        category = "adjective",
        level = "A1",
        topic = "General",
        translationEn = "en",
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        komparativ = komparativ,
        superlativ = superlativ,
    )

    private val gehen = verb("gehen", "ging", "gegangen", "sein")
    private val essen = verb("essen", "aß", "gegessen", "haben")
    private val schnell = adjective("schnell", "schneller", "am schnellsten")

    private val sections = grammarRuleSections(listOf(gehen, essen), listOf(schnell))

    private fun List<RuleSection>.tables(): List<RuleBlock.Table> =
        flatMap { it.blocks }.filterIsInstance<RuleBlock.Table>()

    @Test
    fun sectionsComeInTheExpectedOrderWithUniqueIds() {
        assertEquals(
            listOf(
                RuleSectionIds.SEPARABLE,
                RuleSectionIds.INSEPARABLE,
                RuleSectionIds.CONJUGATION,
                RuleSectionIds.IRREGULAR,
                RuleSectionIds.ADJECTIVES,
            ),
            sections.map { it.id },
        )
        assertEquals(sections.size, sections.map { it.id }.toSet().size)
    }

    @Test
    fun everyTableRowHasOneCellPerHeader() {
        sections.tables().forEach { table ->
            table.rows.forEach { row ->
                assertEquals("Table ${table.title}: $row", table.headers.size, row.size)
            }
        }
    }

    @Test
    fun noTextIsBlank() {
        sections.forEach { section ->
            assertTrue(section.title.isNotBlank() && section.subtitle.isNotBlank())
            assertTrue("Section ${section.id} has no content", section.blocks.isNotEmpty())
            section.blocks.forEach { block ->
                when (block) {
                    is RuleBlock.Paragraph -> assertTrue(block.text.isNotBlank())
                    is RuleBlock.BulletList -> {
                        assertTrue(block.items.isNotEmpty())
                        block.items.forEach { assertTrue(it.isNotBlank()) }
                    }
                    is RuleBlock.Table -> {
                        assertTrue(block.rows.isNotEmpty())
                        (block.headers + block.rows.flatten()).forEach { assertTrue(it.isNotBlank()) }
                    }
                    is RuleBlock.ExampleList -> {
                        assertTrue(block.examples.isNotEmpty())
                        block.examples.forEach {
                            assertTrue(it.de.isNotBlank() && it.en.isNotBlank() && it.bn.isNotBlank())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun everyExampleHasBengaliText() {
        val bengali = Regex("[\\u0980-\\u09FF]")
        sections.flatMap { it.blocks }
            .filterIsInstance<RuleBlock.ExampleList>()
            .flatMap { it.examples }
            .forEach { assertTrue("Not Bengali: ${it.bn}", bengali.containsMatchIn(it.bn)) }
    }

    @Test
    fun separableSectionHasThePrefixAndConjugationTables() {
        val separable = sections.first { it.id == RuleSectionIds.SEPARABLE }
        val tables = separable.blocks.filterIsInstance<RuleBlock.Table>()

        val prefixes = tables.first { it.title == "Common separable prefixes" }
        assertTrue(prefixes.rows.any { it == listOf("ein", "einkaufen", "to shop") })

        val conjugation = tables.first { it.title?.startsWith("Present tense: einkaufen") == true }
        assertEquals(listOf("ich", "du", "er/sie/es", "wir", "ihr", "sie/Sie"), conjugation.rows.map { it[0] })
        assertEquals("kaufe … ein", conjugation.rows[0][1])
    }

    @Test
    fun presentTenseTableCoversAllSixPronouns() {
        val table = sections.tables().first { it.title == "Present tense (Präsens)" }
        assertEquals(6, table.rows.size)
        assertEquals(listOf("Pronoun", "machen", "fahren", "essen", "sein", "haben"), table.headers)
        assertEquals(listOf("du", "machst", "fährst", "isst", "bist", "hast"), table.rows[1])
    }

    @Test
    fun irregularVerbTableIsBuiltFromTheVocabularySortedByWord() {
        val table = sections.tables().first { it.title == "Irregular verbs in your vocabulary" }

        assertEquals(
            listOf(
                listOf("essen", "aß", "gegessen", "haben"),
                listOf("gehen", "ging", "gegangen", "sein"),
            ),
            table.rows,
        )
    }

    @Test
    fun missingFormsAreShownAsADash() {
        val result = grammarRuleSections(listOf(verb("sein", null, null, null)), emptyList())
        val table = result.tables().first { it.title == "Irregular verbs in your vocabulary" }

        assertEquals(listOf("sein", "–", "–", "–"), table.rows.single())
    }

    @Test
    fun adjectiveTableIsBuiltFromTheVocabulary() {
        val table = sections.tables().first { it.title == "Adjectives in your vocabulary" }

        assertEquals(listOf(listOf("schnell", "schneller", "am schnellsten")), table.rows)
    }

    @Test
    fun emptyVocabularyOmitsTheDynamicTables() {
        val result = grammarRuleSections(emptyList(), emptyList())
        val titles = result.tables().map { it.title }

        assertFalse("Irregular verbs in your vocabulary" in titles)
        assertFalse("Adjectives in your vocabulary" in titles)
        assertNotNull(result.firstOrNull { it.id == RuleSectionIds.IRREGULAR })
        assertTrue(result.first { it.id == RuleSectionIds.IRREGULAR }.blocks.isNotEmpty())
    }
}
