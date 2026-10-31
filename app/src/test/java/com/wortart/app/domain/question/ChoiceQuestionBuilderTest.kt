package com.wortart.app.domain.question

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoiceQuestionBuilderTest {

    private fun card(
        id: String,
        word: String,
        category: String,
        auxiliary: String? = null,
        partizip2: String? = null,
        prefix: String? = null,
    ) = VocabEntity(
        id = id,
        word = word,
        category = category,
        level = "A1",
        topic = "General",
        translationEn = "en",
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = auxiliary,
        partizip2 = partizip2,
        prefix = prefix,
    )

    @Test
    fun regularVerbAsksForHabenOrSein() {
        val question = ChoiceQuestionBuilder.build(
            card("rv1", "lernen", WordCategories.REGULAR_VERB, auxiliary = "haben", partizip2 = "gelernt"),
        )!!

        assertEquals(QuestionType.AUXILIARY, question.type)
        assertEquals(listOf("haben", "sein"), question.options)
        assertEquals("haben", question.correctAnswer)
        assertEquals("Perfekt: hat gelernt", question.explanation)
    }

    @Test
    fun seinVerbExplanationUsesIst() {
        val question = ChoiceQuestionBuilder.build(
            card("iv1", "gehen", WordCategories.IRREGULAR_VERB, auxiliary = "sein", partizip2 = "gegangen"),
        )!!

        assertEquals("sein", question.correctAnswer)
        assertEquals("Perfekt: ist gegangen", question.explanation)
    }

    @Test
    fun separableVerbAsksForPrefixWithFourDistinctOptions() {
        val question = ChoiceQuestionBuilder.build(
            card("sv1", "einkaufen", WordCategories.SEPARABLE_VERB, auxiliary = "haben", prefix = "ein"),
        )!!

        assertEquals(QuestionType.PREFIX, question.type)
        assertEquals("ein", question.correctAnswer)
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.toSet().size)
        assertTrue(question.correctAnswer in question.options)
        assertEquals("ein + kaufen", question.explanation)
    }

    @Test
    fun prefixOptionsAreStableForTheSameCard() {
        val verb = card("sv2", "aufstehen", WordCategories.SEPARABLE_VERB, prefix = "auf")
        assertEquals(ChoiceQuestionBuilder.build(verb), ChoiceQuestionBuilder.build(verb))
    }

    @Test
    fun everySeedPrefixCanBeBuilt() {
        // Prefixes used in vocab_seed.json.
        listOf("ein", "auf", "an", "mit", "ab", "aus", "vor").forEach { prefix ->
            val question = ChoiceQuestionBuilder.build(
                card("sv-$prefix", prefix + "xyz", WordCategories.SEPARABLE_VERB, prefix = prefix),
            )
            assertNotNull(question)
            assertTrue(prefix in question!!.options)
            assertEquals(4, question.options.toSet().size)
        }
    }

    private fun word(id: String, word: String, category: String, en: String, bn: String) = card(id, word, category)
        .copy(translationEn = en, translationBn = bn)

    private val adjectivePool = listOf(
        word("a1", "schnell", WordCategories.ADJECTIVE, "fast; quick", "দ্রুত"),
        word("a2", "langsam", WordCategories.ADJECTIVE, "slow", "ধীর"),
        word("a3", "groß", WordCategories.ADJECTIVE, "big; tall", "বড়; লম্বা"),
        word("a4", "schön", WordCategories.ADJECTIVE, "beautiful", "সুন্দর"),
        word("a5", "klein", WordCategories.ADJECTIVE, "small", "ছোট"),
        word("d1", "oft", WordCategories.ADVERB, "often", "প্রায়ই"),
    )

    @Test
    fun adjectiveAsksForTheMeaningAmongSimilarWords() {
        val question = ChoiceQuestionBuilder.build(adjectivePool[0], adjectivePool)!!

        assertEquals(QuestionType.MEANING, question.type)
        assertEquals("What is the correct meaning?", question.prompt)
        assertEquals("fast / দ্রুত", question.correctAnswer)
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.toSet().size)
        assertTrue("fast / দ্রুত" in question.options)
        assertEquals("schnell = fast; quick / দ্রুত", question.explanation)
    }

    @Test
    fun wrongMeaningsComeFromWordsOfTheSameCategoryOnly() {
        val question = ChoiceQuestionBuilder.build(adjectivePool[0], adjectivePool)!!

        assertFalse("often / প্রায়ই" in question.options)
    }

    @Test
    fun meaningOptionsAreStableForTheSameCard() {
        val adjective = adjectivePool[2]

        assertEquals(
            ChoiceQuestionBuilder.build(adjective, adjectivePool),
            ChoiceQuestionBuilder.build(adjective, adjectivePool),
        )
    }

    @Test
    fun adverbsGetMeaningQuestionsToo() {
        val adverbs = listOf(
            word("d1", "oft", WordCategories.ADVERB, "often", "প্রায়ই"),
            word("d2", "immer", WordCategories.ADVERB, "always", "সবসময়"),
            word("d3", "hier", WordCategories.ADVERB, "here", "এখানে"),
        )

        val question = ChoiceQuestionBuilder.build(adverbs[0], adverbs)!!

        assertEquals(QuestionType.MEANING, question.type)
        assertEquals("often / প্রায়ই", question.correctAnswer)
        assertEquals(3, question.options.size)
    }

    @Test
    fun withoutOtherWordsThereIsNoMeaningQuestion() {
        assertNull(ChoiceQuestionBuilder.build(adjectivePool[0]))
        assertNull(ChoiceQuestionBuilder.build(adjectivePool[0], listOf(adjectivePool[0])))
    }

    @Test
    fun theShortMeaningUsesTheFirstMeaningOfEachLanguage() {
        assertEquals("to go / যাওয়া", card("v1", "gehen", WordCategories.IRREGULAR_VERB)
            .copy(translationEn = "to go; to walk", translationBn = "যাওয়া; হাঁটা").shortMeaning())
    }

    @Test
    fun incompleteVerbDataGivesNoQuestion() {
        assertNull(ChoiceQuestionBuilder.build(card("v1", "x", WordCategories.REGULAR_VERB)))
        assertNull(ChoiceQuestionBuilder.build(card("v2", "x", WordCategories.IRREGULAR_VERB, auxiliary = "kaufen")))
        assertNull(ChoiceQuestionBuilder.build(card("v3", "x", WordCategories.SEPARABLE_VERB)))
    }
}
