package com.wortart.app.ui.rules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.domain.rules.lessons.Bi
import com.wortart.app.domain.rules.lessons.Lesson
import com.wortart.app.domain.rules.lessons.LessonBlock
import com.wortart.app.domain.rules.lessons.LessonExample
import com.wortart.app.domain.rules.lessons.LessonsFile
import com.wortart.app.domain.rules.lessons.QuizItem
import com.wortart.app.domain.rules.lessons.Trio
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val articles = Lesson(
        id = "articles",
        level = "A1",
        icon = "menu_book",
        title = Trio("Artikel", "Articles", "আর্টিকেল"),
        summary = Bi("Every noun has a gender.", "প্রতিটি বিশেষ্যের লিঙ্গ আছে।"),
        blocks = listOf(
            LessonBlock.Text(en = "Use der, die or das.", bn = "der, die বা das ব্যবহার করুন।", title = "Basics"),
            LessonBlock.Table(
                headers = listOf("Gender", "Article"),
                rows = listOf(listOf("masculine", "der Mann"), listOf("feminine", "die Frau")),
                caption = Bi("The three genders", "তিনটি লিঙ্গ"),
            ),
            LessonBlock.Examples(
                listOf(LessonExample("Ich sehe den Mann.", "I see the man.", "আমি লোকটিকে দেখি।", mark = "den Mann")),
            ),
            LessonBlock.Tip(en = "Learn the article with the noun.", bn = "বিশেষ্যের সাথে আর্টিকেল শিখুন।"),
            LessonBlock.Mistake(
                wrong = "Ich sehe der Mann.",
                right = "Ich sehe den Mann.",
                en = "Accusative needs den.",
                bn = "কর্ম কারকে den লাগে।",
            ),
        ),
        quiz = listOf(
            QuizItem("Ich sehe ___ Mann.", listOf("der", "den"), 1, Bi("Accusative.", "কর্ম কারক।")),
        ),
    )

    private val present = Lesson(
        id = "praesens",
        level = "A2",
        icon = "schedule",
        title = Trio("Präsens", "Present tense", "বর্তমান কাল"),
        summary = Bi("Now.", "এখন।"),
        blocks = listOf(LessonBlock.Text(en = "Add endings.", bn = "প্রত্যয় যোগ করুন।")),
    )

    private val file = LessonsFile(
        version = 1,
        genderColors = mapOf("der" to "#4C8DFF", "die" to "#FF5A5F", "das" to "#2EB67D"),
        sections = listOf(articles, present),
    )

    private fun readyState(level: String? = null, answers: Map<String, String> = emptyMap()) = LessonsUiState(
        status = LessonsStatus.READY,
        file = file,
        level = level,
        answers = answers,
    )

    private fun show(
        state: LessonsUiState,
        onSelectLevel: (String?) -> Unit = {},
        onAnswerQuiz: (String, String) -> Unit = { _, _ -> },
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            WortArtTheme {
                LessonsContent(
                    state = state,
                    onSelectLevel = onSelectLevel,
                    onAnswerQuiz = onAnswerQuiz,
                    onRetry = onRetry,
                )
            }
        }
    }

    @Test
    fun lessonsAreListedCollapsedWithTheirTitlesInThreeLanguages() {
        show(readyState())

        composeRule.onNodeWithText("Artikel").assertExists()
        composeRule.onNodeWithText("Articles · আর্টিকেল").assertExists()
        composeRule.onNodeWithText("Präsens").assertExists()
        composeRule.onNodeWithText("Present tense · বর্তমান কাল").assertExists()
        composeRule.onNodeWithText("Every noun has a gender.").assertDoesNotExist()
    }

    @Test
    fun openingALessonShowsEveryKindOfContent() {
        show(readyState())

        composeRule.onNodeWithText("Artikel").performClick()

        composeRule.onNodeWithText("Every noun has a gender.").assertExists()
        composeRule.onNodeWithText("প্রতিটি বিশেষ্যের লিঙ্গ আছে।").assertExists()

        composeRule.onNodeWithText("Basics").assertExists()
        composeRule.onNodeWithText("Use der, die or das.").assertExists()
        composeRule.onNodeWithText("der, die বা das ব্যবহার করুন।").assertExists()

        composeRule.onNodeWithText("The three genders · তিনটি লিঙ্গ").assertExists()
        composeRule.onNodeWithText("der Mann").assertExists()
        composeRule.onNodeWithText("die Frau").assertExists()

        composeRule.onNodeWithText("Ich sehe den Mann.").assertExists()
        composeRule.onNodeWithText("I see the man.").assertExists()
        composeRule.onNodeWithText("আমি লোকটিকে দেখি।").assertExists()

        composeRule.onNodeWithText("Tip").assertExists()
        composeRule.onNodeWithText("Learn the article with the noun.").assertExists()

        composeRule.onNodeWithText("Common mistake").assertExists()
        composeRule.onNodeWithText("✗  Ich sehe der Mann.").assertExists()
        composeRule.onNodeWithText("✓  Ich sehe den Mann.").assertExists()
        composeRule.onNodeWithText("Accusative needs den.").assertExists()

        composeRule.onNodeWithText("Quick check").assertExists()
        composeRule.onNodeWithText("Ich sehe ___ Mann.").assertExists()
    }

    @Test
    fun onlyOneLessonIsOpenAtATime() {
        show(readyState())

        composeRule.onNodeWithText("Artikel").performClick()
        composeRule.onNodeWithText("Every noun has a gender.").assertExists()

        composeRule.onNodeWithText("Präsens").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Every noun has a gender.").assertDoesNotExist()
        composeRule.onNodeWithText("Now.").assertExists()
    }

    @Test
    fun theQuizGivesFeedbackWithBothLanguagesAndReportsTheAnswer() {
        val given = mutableListOf<Pair<String, String>>()
        composeRule.setContent {
            WortArtTheme {
                var answers by remember { mutableStateOf(emptyMap<String, String>()) }
                LessonsContent(
                    state = readyState(answers = answers),
                    onSelectLevel = {},
                    onAnswerQuiz = { key, option ->
                        given += key to option
                        answers = answers + (key to option)
                    },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Artikel").performClick()
        composeRule.onNodeWithText("den").performClick()

        composeRule.onNodeWithText("✓ Correct!").assertExists()
        composeRule.onNodeWithText("Accusative.\nকর্ম কারক।").assertExists()
        composeRule.runOnIdle { assertEquals(listOf("articles:0" to "den"), given) }
    }

    @Test
    fun aWrongQuizAnswerShowsTheCorrectOne() {
        show(readyState(answers = mapOf("articles:0" to "der")))

        composeRule.onNodeWithText("Artikel").performClick()

        composeRule.onNodeWithText("✗ Not quite — the answer is den").assertExists()
    }

    @Test
    fun levelChipsFilterTheLessons() {
        val selected = mutableListOf<String?>()
        show(readyState(), onSelectLevel = { selected += it })

        composeRule.onNodeWithText("A2").performClick()
        composeRule.onNodeWithText("All").performClick()

        composeRule.runOnIdle { assertEquals(listOf<String?>("A2", null), selected) }
    }

    @Test
    fun theSelectedLevelHidesOtherLessons() {
        show(readyState(level = "A2"))

        composeRule.onNodeWithText("Präsens").assertExists()
        composeRule.onNodeWithText("Artikel").assertDoesNotExist()
    }

    @Test
    fun aLevelWithoutLessonsSaysSo() {
        show(readyState(level = "B2"))

        composeRule.onNodeWithText("No lessons for this level yet.").assertExists()
    }

    @Test
    fun theLevelBadgeIsShownOnEveryLessonCard() {
        show(readyState())

        // One "A1" chip in the filter row and one badge on the Articles card.
        composeRule.onAllNodesWithText("A1").assertCountEquals(2)
    }

    @Test
    fun loadingShowsAMessage() {
        show(LessonsUiState(status = LessonsStatus.LOADING))

        composeRule.onNodeWithText("Loading lessons…").assertExists()
    }

    @Test
    fun aLoadErrorOffersToTryAgain() {
        var retries = 0
        show(LessonsUiState(status = LessonsStatus.ERROR, error = "Bad file"), onRetry = { retries++ })

        composeRule.onNodeWithText("Bad file").assertExists()
        composeRule.onNodeWithText("Try again").performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }
}
