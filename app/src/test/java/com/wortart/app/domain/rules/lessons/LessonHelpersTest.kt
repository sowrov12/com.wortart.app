package com.wortart.app.domain.rules.lessons

import com.wortart.app.domain.question.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LessonHelpersTest {

    // ---- Colors ----

    @Test
    fun hexColorsBecomeOpaqueArgbValues() {
        assertEquals(0xFF4C8DFFL, parseHexColor("#4C8DFF"))
        assertEquals(0xFFFF5A5FL, parseHexColor("#ff5a5f"))
        assertEquals(0xFF2EB67DL, parseHexColor("  #2EB67D "))
    }

    @Test
    fun invalidColorsAreRejected() {
        assertNull(parseHexColor("4C8DFF"))
        assertNull(parseHexColor("#4C8DF"))
        assertNull(parseHexColor("#4C8DFFAA"))
        assertNull(parseHexColor("blue"))
        assertNull(parseHexColor(""))
    }

    // ---- Articles ----

    @Test
    fun articlesAreFoundAtTheStartOfATableCell() {
        assertEquals("der", articleOf("der (m)"))
        assertEquals("die", articleOf("Die Frau"))
        assertEquals("das", articleOf("  das Kind"))
        assertEquals("der", articleOf("der"))
    }

    @Test
    fun otherTextHasNoArticle() {
        assertNull(articleOf("den Mann"))
        assertNull(articleOf("dieser Tisch"))
        assertNull(articleOf("ich"))
        assertNull(articleOf(""))
    }

    // ---- Quiz ----

    private val quiz = QuizItem(
        q = "Ich sehe ___ Mann.",
        options = listOf("der", "den", "dem"),
        answer = 1,
        why = Bi("Accusative masculine.", "কর্ম কারকে পুংলিঙ্গ।"),
    )

    @Test
    fun aQuizBecomesAChoiceQuestionWithBothLanguagesInTheExplanation() {
        val question = quiz.toChoiceQuestion()!!

        assertEquals(QuestionType.QUIZ, question.type)
        assertEquals("Ich sehe ___ Mann.", question.prompt)
        assertEquals(listOf("der", "den", "dem"), question.options)
        assertEquals("den", question.correctAnswer)
        assertEquals("Accusative masculine.\nকর্ম কারকে পুংলিঙ্গ।", question.explanation)
    }

    @Test
    fun aQuizWithABadAnswerIndexGivesNoQuestion() {
        assertNull(quiz.copy(answer = 3).toChoiceQuestion())
        assertNull(quiz.copy(answer = -1).toChoiceQuestion())
    }

    // ---- Emoji ----

    @Test
    fun knownIconsGetAnEmojiAndUnknownOnesABook() {
        assertEquals("📖", lessonEmoji("menu_book"))
        assertEquals("🚫", lessonEmoji("block"))
        assertEquals("📚", lessonEmoji("something_else"))
    }
}
