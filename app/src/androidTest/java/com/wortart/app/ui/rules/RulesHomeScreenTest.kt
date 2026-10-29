package com.wortart.app.ui.rules

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.domain.rules.grammarRuleSections
import com.wortart.app.domain.rules.lessons.Bi
import com.wortart.app.domain.rules.lessons.Lesson
import com.wortart.app.domain.rules.lessons.LessonBlock
import com.wortart.app.domain.rules.lessons.LessonsFile
import com.wortart.app.domain.rules.lessons.Trio
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RulesHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lessonsState = LessonsUiState(
        status = LessonsStatus.READY,
        file = LessonsFile(
            version = 1,
            genderColors = emptyMap(),
            sections = listOf(
                Lesson(
                    id = "articles",
                    level = "A1",
                    icon = "menu_book",
                    title = Trio("Artikel", "Articles", "আর্টিকেল"),
                    summary = Bi("Gender.", "লিঙ্গ।"),
                    blocks = listOf(LessonBlock.Text("t", "ট")),
                ),
            ),
        ),
    )

    @Test
    fun lessonsAreShownFirstAndTheSwitchOpensTheReferenceTables() {
        composeRule.setContent {
            WortArtTheme {
                RulesHomeScreen(
                    referenceSections = grammarRuleSections(emptyList(), emptyList()),
                    lessonsState = lessonsState,
                    onSelectLevel = {},
                    onAnswerQuiz = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Grammar Rules").assertExists()
        composeRule.onNodeWithText("Lessons").assertExists()
        composeRule.onNodeWithText("Reference").assertExists()
        composeRule.onNodeWithText("Artikel").assertExists()
        composeRule.onNodeWithText("Separable verbs").assertDoesNotExist()

        composeRule.onNodeWithText("Reference").performClick()

        composeRule.onNodeWithText("Separable verbs").assertExists()
        composeRule.onNodeWithText("Artikel").assertDoesNotExist()
        // The title is shown once, not once per part.
        composeRule.onNodeWithText("Grammar Rules").assertExists()

        composeRule.onNodeWithText("Lessons").performClick()

        composeRule.onNodeWithText("Artikel").assertExists()
        composeRule.onNodeWithText("Separable verbs").assertDoesNotExist()
    }
}
