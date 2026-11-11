package com.wortart.app.domain.question

import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChoiceQuestionSetsTest {

    private fun entity(
        id: String,
        word: String,
        category: String,
        tags: String? = null,
        en: String = "en-$id",
        bn: String = "bn-$id",
    ) = VocabEntity(
        id = id,
        word = word,
        category = category,
        level = "A1",
        topic = "General",
        translationEn = en,
        translationBn = bn,
        exampleDe = "Ich lerne Deutsch.",
        exampleEn = "I learn German.",
        exampleBn = "bn",
        tags = tags,
    )

    private val verb = entity("v1", "lernen", WordCategories.REGULAR_VERB).copy(
        auxiliary = "haben",
        partizip2 = "gelernt",
        pz2Wrong = "gelernen|lernt",
        blankDe = "Ich _____ Deutsch.",
        blankAnswer = "lerne",
        blankWrong = "lernst|lernt",
    )

    @Test
    fun blankQuestionForAVerbKeepsTheInfinitiveAndHasThreeOptions() {
        val q = ChoiceQuestionBuilder.buildSet(verb, QuestionType.BLANK)!!

        assertEquals(QuestionType.BLANK, q.type)
        assertEquals("Ich _____ Deutsch.", q.exampleBlank)
        assertNull(q.titleBlank)
        assertEquals(3, q.options.size)
        assertTrue("lerne" in q.options)
        assertEquals("lerne", q.correctAnswer)
    }

    @Test
    fun blankQuestionForOtherWordsHidesTheTitle() {
        val adverb = entity("a1", "heute", WordCategories.ADVERB).copy(
            blankDe = "_____ ist Samstag.",
            blankAnswer = "Heute",
            blankWrong = "Gestern|Manchmal",
        )

        val q = ChoiceQuestionBuilder.buildSet(adverb, QuestionType.BLANK)!!

        assertEquals("_____", q.titleBlank)
    }

    @Test
    fun partizipQuestionHasThreeOptions() {
        val q = ChoiceQuestionBuilder.buildSet(verb, QuestionType.PARTIZIP)!!

        assertEquals(3, q.options.size)
        assertEquals("gelernt", q.correctAnswer)
        assertEquals("Perfekt: hat gelernt", q.explanation)
    }

    @Test
    fun prefixQuestionUsesGapsInTitleAndExample() {
        val sep = entity("s1", "anrufen", WordCategories.REGULAR_VERB, tags = ",Regular Verbs,Separable Verbs,")
            .copy(
                auxiliary = "haben",
                prefix = "an",
                prefixBlankDe = "Ich rufe meine Mutter _____.",
                prefixBlankTitle = "_____rufen",
            )

        val q = ChoiceQuestionBuilder.buildSet(sep, QuestionType.PREFIX)!!

        assertEquals("_____rufen", q.titleBlank)
        assertEquals("Ich rufe meine Mutter _____.", q.exampleBlank)
        assertEquals("an", q.correctAnswer)
        assertEquals(4, q.options.size)
    }

    @Test
    fun antonymQuestionHasTwoOptionsWithMeanings() {
        val adj = entity("j1", "gut", WordCategories.ADJECTIVE).copy(
            antonym = "schlecht",
            antonymMeaning = "bad / খারাপ",
            antonymWrong = "klein — small / ছোট",
        )

        val q = ChoiceQuestionBuilder.buildSet(adj, QuestionType.ANTONYM)!!

        assertEquals(2, q.options.size)
        assertEquals("schlecht — bad / খারাপ", q.correctAnswer)
    }

    @Test
    fun missingDataGivesNoQuestionForThatSet() {
        val bare = entity("v2", "x", WordCategories.REGULAR_VERB).copy(auxiliary = "haben")

        assertNull(ChoiceQuestionBuilder.buildSet(bare, QuestionType.BLANK))
        assertNull(ChoiceQuestionBuilder.buildSet(bare, QuestionType.PARTIZIP))
        assertNull(ChoiceQuestionBuilder.buildSet(bare, QuestionType.PREFIX))
        assertNull(ChoiceQuestionBuilder.buildSet(bare, QuestionType.ANTONYM))
    }

    @Test
    fun mixedPicksASetThatFitsTheCard() {
        val q = ChoiceQuestionBuilder.buildMixed(verb, listOf(verb), Random(3))

        assertNotNull(q)
        assertTrue(q!!.type in ChoiceQuestionBuilder.applicableTypes(verb))
    }

    @Test
    fun meaningHidesTheMeaningLineAndUsesWordsOfTheSameKind() {
        val pool = listOf(
            verb,
            entity("v3", "gehen", WordCategories.IRREGULAR_VERB, en = "to go", bn = "যাওয়া"),
            entity("v4", "essen", WordCategories.IRREGULAR_VERB, en = "to eat", bn = "খাওয়া"),
            entity("j2", "klein", WordCategories.ADJECTIVE, en = "small", bn = "ছোট"),
        )

        val q = ChoiceQuestionBuilder.buildSet(verb, QuestionType.MEANING, pool)!!

        assertTrue(q.hideMeaning)
        assertFalse("small / ছোট" in q.options)
        assertTrue("to go / যাওয়া" in q.options)
    }
}
