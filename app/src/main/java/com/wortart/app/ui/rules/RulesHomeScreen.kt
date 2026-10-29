package com.wortart.app.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.domain.rules.RuleSection
import com.wortart.app.ui.components.PillChip
import com.wortart.app.ui.theme.WortArtColors

/** Connects the Rules tab to its [RulesViewModel]. */
@Composable
fun GrammarRulesRoute(viewModel: RulesViewModel, modifier: Modifier = Modifier) {
    val reference by viewModel.sections.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()

    RulesHomeScreen(
        referenceSections = reference,
        lessonsState = lessons,
        onSelectLevel = viewModel::selectLevel,
        onAnswerQuiz = viewModel::answerQuiz,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

/**
 * The Rules tab. "Lessons" shows the bilingual grammar lessons with quizzes;
 * "Reference" shows the quick reference tables.
 */
@Composable
fun RulesHomeScreen(
    referenceSections: List<RuleSection>,
    lessonsState: LessonsUiState,
    onSelectLevel: (String?) -> Unit,
    onAnswerQuiz: (key: String, option: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLessons by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text(
                text = "Grammar Rules",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Lessons in English and Bengali, plus quick reference tables",
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip(label = "Lessons", selected = showLessons, onClick = { showLessons = true })
                PillChip(label = "Reference", selected = !showLessons, onClick = { showLessons = false })
            }
        }

        if (showLessons) {
            LessonsContent(
                state = lessonsState,
                onSelectLevel = onSelectLevel,
                onAnswerQuiz = onAnswerQuiz,
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        } else {
            GrammarRulesScreen(
                sections = referenceSections,
                showHeader = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
