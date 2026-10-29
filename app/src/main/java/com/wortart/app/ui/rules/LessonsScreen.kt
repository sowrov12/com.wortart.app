package com.wortart.app.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.domain.rules.RuleBlock
import com.wortart.app.domain.rules.RuleExample
import com.wortart.app.domain.rules.lessons.Lesson
import com.wortart.app.domain.rules.lessons.LessonBlock
import com.wortart.app.domain.rules.lessons.lessonEmoji
import com.wortart.app.domain.rules.lessons.parseHexColor
import com.wortart.app.domain.rules.lessons.toChoiceQuestion
import com.wortart.app.ui.components.ChoiceQuestionView
import com.wortart.app.ui.components.LevelBadge
import com.wortart.app.ui.components.LevelFilterRow
import com.wortart.app.ui.theme.WortArtColors

/**
 * The "Lessons" part of the Rules tab: grammar lessons in English and Bengali with a level
 * filter. Each lesson opens to explanations, tables, examples, tips, common mistakes and a
 * one-question quiz. Stateless apart from the open lesson.
 */
@Composable
fun LessonsContent(
    state: LessonsUiState,
    onSelectLevel: (String?) -> Unit,
    onAnswerQuiz: (key: String, option: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.status) {
        LessonsStatus.LOADING -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Loading lessons…", color = WortArtColors.TextSecondary, fontSize = 15.sp)
        }

        LessonsStatus.ERROR -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.error ?: "Could not load the lessons.",
                color = WortArtColors.TextSecondary,
                fontSize = 15.sp,
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WortArtColors.Gold,
                    contentColor = WortArtColors.OnGold,
                ),
            ) {
                Text(text = "Try again")
            }
        }

        LessonsStatus.READY -> LessonsList(
            state = state,
            onSelectLevel = onSelectLevel,
            onAnswerQuiz = onAnswerQuiz,
            modifier = modifier,
        )
    }
}

@Composable
private fun LessonsList(
    state: LessonsUiState,
    onSelectLevel: (String?) -> Unit,
    onAnswerQuiz: (String, String) -> Unit,
    modifier: Modifier,
) {
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    // der / die / das colors from the lessons file.
    val genderColors = remember(state.file) {
        state.file?.genderColors.orEmpty()
            .mapNotNull { (article, hex) -> parseHexColor(hex)?.let { article to Color(it) } }
            .toMap()
    }
    val lessons = state.lessons

    CompositionLocalProvider(LocalGenderColors provides genderColors) {
        Column(modifier = modifier.fillMaxSize()) {
            LevelFilterRow(
                selected = state.level,
                onSelect = onSelectLevel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (lessons.isEmpty()) {
                Text(
                    text = "No lessons for this level yet.",
                    color = WortArtColors.TextSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            expanded = expandedId == lesson.id,
                            answers = state.answers,
                            onToggle = { expandedId = if (expandedId == lesson.id) null else lesson.id },
                            onAnswerQuiz = onAnswerQuiz,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    expanded: Boolean,
    answers: Map<String, String>,
    onToggle: () -> Unit,
    onAnswerQuiz: (String, String) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "lessonChevron",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(
                width = 1.dp,
                color = WortArtColors.Gold.copy(alpha = if (expanded) 0.5f else 0.25f),
                shape = shape,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Collapse" else "Expand",
                    onClick = onToggle,
                )
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = lessonEmoji(lesson.icon), fontSize = 22.sp)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title.de,
                    color = WortArtColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${lesson.title.en} · ${lesson.title.bn}",
                    color = WortArtColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.size(8.dp))
            LevelBadge(lesson.level)
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = WortArtColors.Gold,
                modifier = Modifier.rotate(chevronRotation),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = lesson.summary.en, color = WortArtColors.TextSecondary, fontSize = 15.sp)
                    Text(text = lesson.summary.bn, color = WortArtColors.GoldLight, fontSize = 15.sp)
                }

                lesson.blocks.forEach { block -> LessonBlockView(block) }

                lesson.quiz.forEachIndexed { index, quiz ->
                    val key = "${lesson.id}:$index"
                    val question = remember(key) { quiz.toChoiceQuestion() }
                    if (question != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Quick check",
                                color = WortArtColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            ChoiceQuestionView(
                                question = question,
                                selected = answers[key],
                                onSelect = { option -> onAnswerQuiz(key, option) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonBlockView(block: LessonBlock) {
    when (block) {
        is LessonBlock.Text -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.title?.let { title ->
                Text(
                    text = title,
                    color = WortArtColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(text = block.en, color = WortArtColors.TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
            Text(text = block.bn, color = WortArtColors.GoldLight, fontSize = 15.sp, lineHeight = 22.sp)
        }

        is LessonBlock.Table -> RuleTableView(
            RuleBlock.Table(
                title = block.caption?.let { "${it.en} · ${it.bn}" },
                headers = block.headers,
                rows = block.rows,
            ),
        )

        is LessonBlock.Examples -> ExampleListView(
            RuleBlock.ExampleList(
                title = null,
                examples = block.items.map { RuleExample(it.de, it.en, it.bn, it.mark) },
            ),
        )

        is LessonBlock.Tip -> Callout(label = "Tip", accent = WortArtColors.Gold) {
            Text(text = block.en, color = WortArtColors.TextPrimary, fontSize = 14.sp)
            Text(text = block.bn, color = WortArtColors.GoldLight, fontSize = 14.sp)
        }

        is LessonBlock.Mistake -> Callout(label = "Common mistake", accent = WortArtColors.Wrong) {
            Text(
                text = "✗  ${block.wrong}",
                color = WortArtColors.Wrong,
                fontSize = 15.sp,
                textDecoration = TextDecoration.LineThrough,
            )
            Text(text = "✓  ${block.right}", color = WortArtColors.Correct, fontSize = 15.sp)
            Text(text = block.en, color = WortArtColors.TextSecondary, fontSize = 14.sp)
            Text(text = block.bn, color = WortArtColors.GoldLight, fontSize = 14.sp)
        }
    }
}

/** A tinted box with a small label, used for tips and common mistakes. */
@Composable
private fun Callout(label: String, accent: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.4f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        content()
    }
}
