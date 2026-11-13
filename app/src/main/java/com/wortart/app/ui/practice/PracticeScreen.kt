package com.wortart.app.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.ui.components.EmptyState
import com.wortart.app.ui.components.FavoriteButton
import com.wortart.app.ui.components.FlashcardView
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.components.GoldProgressBar
import com.wortart.app.ui.components.LoadingState
import com.wortart.app.ui.components.OutlineButton
import com.wortart.app.ui.components.WortArtTopBar
import com.wortart.app.ui.components.noBn
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** Connects [PracticeScreen] to its [PracticeViewModel] and handles the Back button. */
@Composable
fun PracticeRoute(
    viewModel: PracticeViewModel,
    speakingCardId: String?,
    onSpeak: (wordId: String, word: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Called once per submitted answer: true for a correct answer (plays the sound). */
    onAnswerResult: (Boolean) -> Unit = {},
    readAloud: Boolean = true,
    reminders: Boolean = true,
    showBengali: Boolean = true,
    onReadAloud: (Boolean) -> Unit = {},
    onReminders: (Boolean) -> Unit = {},
    onShowBengali: (Boolean) -> Unit = {},
    onOpenCategories: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onClose)

    LaunchedEffect(state.index, state.submitted) {
        if (state.status == PracticeStatus.ACTIVE && state.submitted) onAnswerResult(state.isCorrect)
    }

    // Read each card aloud when it appears. A card whose word is hidden is read after the answer.
    val currentCard = state.current
    LaunchedEffect(currentCard?.card?.id, state.submitted, readAloud) {
        val session = currentCard ?: return@LaunchedEffect
        if (!readAloud || state.status != PracticeStatus.ACTIVE) return@LaunchedEffect
        val wordHidden = session.question.titleBlank != null
        if (wordHidden == state.submitted) onSpeak(session.card.id, session.card.word)
    }

    PracticeScreen(
        state = state,
        speakingCardId = speakingCardId,
        onBack = onClose,
        onSpeak = onSpeak,
        onToggleFavorite = viewModel::toggleFavorite,
        // Tapping an option answers at once: there is no Submit button.
        onSelect = { option ->
            viewModel.select(option)
            viewModel.submit()
        },
        onSubmit = viewModel::submit,
        onNext = viewModel::next,
        onRestart = viewModel::startNew,
        modifier = modifier,
        onOpenSetup = viewModel::startNew,
        menu = PracticeMenuConfig(
            levels = state.config.levels,
            readAloud = readAloud,
            reminders = reminders,
            showBengali = showBengali,
            onToggleLevel = viewModel::toggleLevelLive,
            onClearLevels = viewModel::clearLevelsLive,
            onReadAloud = onReadAloud,
            onReminders = onReminders,
            onShowBengali = onShowBengali,
            onSkip = viewModel::skip,
            onOpenCategories = onOpenCategories,
            onSelectSet = viewModel::chooseSetLive,
        ),
    )
}

/** The flashcard session. Stateless. */
@Composable
fun PracticeScreen(
    state: PracticeUiState,
    speakingCardId: String?,
    onBack: () -> Unit,
    onSpeak: (wordId: String, word: String) -> Unit,
    onToggleFavorite: () -> Unit,
    onSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSetup: () -> Unit = {},
    menu: PracticeMenuConfig = PracticeMenuConfig(),
) {
    var showFilter by remember { mutableStateOf(false) }
    val openFilter = { showFilter = true }

    Column(modifier = modifier.fillMaxSize()) {
        if (showFilter) {
            PracticeFilterSheet(
                group = state.config.group,
                type = state.config.type,
                levels = state.config.levels,
                onSelectSet = menu.onSelectSet,
                onToggleLevel = menu.onToggleLevel,
                onDismiss = { showFilter = false },
            )
        }
        when (state.status) {
            PracticeStatus.LOADING, PracticeStatus.SETUP -> {
                WortArtTopBar(title = "Flashcards", onBack = onBack, trailing = { PracticeMenu(menu) })
                LoadingState(modifier = Modifier.weight(1f))
            }

            PracticeStatus.EMPTY -> {
                WortArtTopBar(title = "Flashcards", onBack = onBack, trailing = { PracticeMenu(menu) })
                CategoryRow(
                    state = state,
                    onOpenCategories = menu.onOpenCategories,
                    onOpenFilter = openFilter,
                )
                EmptyState(
                    title = "No flashcards here",
                    message = "No word matches this category and level. Change the category or the level.",
                    actionLabel = "Try again",
                    onAction = onRestart,
                    modifier = Modifier.weight(1f),
                )
            }

            PracticeStatus.ACTIVE -> ActiveSession(
                state = state,
                speakingCardId = speakingCardId,
                onBack = onBack,
                onSpeak = onSpeak,
                onToggleFavorite = onToggleFavorite,
                onSelect = onSelect,
                onSubmit = onSubmit,
                onNext = onNext,
                menu = menu,
                onOpenFilter = openFilter,
            )

            PracticeStatus.FINISHED -> SessionSummary(
                state = state,
                onBack = onBack,
                onRestart = onRestart,
                onOpenSetup = onOpenSetup,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    state: PracticeUiState,
    onOpenCategories: () -> Unit,
    onOpenFilter: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(24.dp)
    val activeFilters = state.config.levels.size + if (state.config.type != null) 1 else 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(shape)
                .border(1.dp, WortArtColors.Gold.copy(alpha = 0.6f), shape)
                .clickable(role = Role.Button, onClick = onOpenCategories)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "🗂", fontSize = 16.sp)
            Text(
                text = state.config.group ?: "All Category",
                color = WortArtColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Choose category",
                tint = WortArtColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Row(
            modifier = Modifier
                .clip(shape)
                .border(
                    1.dp,
                    if (activeFilters > 0) WortArtColors.Gold else WortArtColors.Gold.copy(alpha = 0.6f),
                    shape,
                )
                .clickable(role = Role.Button, onClick = onOpenFilter)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "🎚", fontSize = 16.sp)
            Text(
                text = if (activeFilters > 0) "Filter · $activeFilters" else "Filter",
                color = if (activeFilters > 0) WortArtColors.Gold else WortArtColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        trailing()
    }
}

@Composable
private fun ColumnScope.ActiveSession(
    state: PracticeUiState,
    speakingCardId: String?,
    onBack: () -> Unit,
    onSpeak: (wordId: String, word: String) -> Unit,
    onToggleFavorite: () -> Unit,
    onSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    menu: PracticeMenuConfig,
    onOpenFilter: () -> Unit,
) {
    val session = state.current ?: return
    val card = session.card
    val question = session.question

    WortArtTopBar(title = "Flashcards", onBack = onBack, trailing = { PracticeMenu(menu) })
    CategoryRow(
        state = state,
        onOpenCategories = menu.onOpenCategories,
        onOpenFilter = onOpenFilter,
        trailing = { FavoriteButton(isFavorite = card.isFavorite, onClick = onToggleFavorite) },
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!state.endless) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.index + 1} / ${state.total}",
                    color = WortArtColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            GoldProgressBar(fraction = state.progress)
        }

        FlashcardView(
            card = card,
            onSpeak = { onSpeak(card.id, card.word) },
            isSpeaking = card.id == speakingCardId,
            titleOverride = question.titleBlank,
            exampleOverride = question.exampleBlank,
            hideMeaning = question.hideMeaning,
            revealed = state.submitted,
            speakEnabled = state.submitted || question.titleBlank == null,
            highlight = when (question.type) {
                QuestionType.BLANK, QuestionType.PREFIX -> question.correctAnswer
                else -> null
            },
        )

        Text(
            text = question.prompt,
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        AnswerOptions(
            question = question,
            selected = state.selected,
            submitted = state.submitted,
            onSelect = onSelect,
        )

        if (state.submitted) {
            AnswerFeedback(
                isCorrect = state.isCorrect,
                correctAnswer = question.correctAnswer,
                explanation = question.explanation,
            )
        }

        Spacer(Modifier.height(4.dp))
    }

    GoldButton(
        text = if (state.submitted && state.isLastCard && !state.endless) "Finish" else "Next",
        // Before an answer, Next skips the card.
        onClick = if (state.submitted) onNext else menu.onSkip,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun AnswerFeedback(isCorrect: Boolean, correctAnswer: String, explanation: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (isCorrect) "✓ Correct!" else "✗ Not quite — the answer is ${correctAnswer.noBn()}",
            color = if (isCorrect) WortArtColors.Correct else WortArtColors.Wrong,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = explanation.noBn(), color = WortArtColors.TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun SessionSummary(
    state: PracticeUiState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    val percent = if (state.total == 0) 0 else Math.round(state.correctCount * 100f / state.total)

    WortArtTopBar(title = "Flashcards", onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🎉", fontSize = 48.sp)
        Text(
            text = "Session complete",
            color = WortArtColors.TextPrimary,
            fontSize = 26.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${state.correctCount} / ${state.total} correct",
            color = WortArtColors.Gold,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$percent% success rate",
            color = WortArtColors.TextSecondary,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(12.dp))
        GoldButton(text = "Practice again", onClick = onRestart, modifier = Modifier.fillMaxWidth())
        OutlineButton(text = "Change set", onClick = onOpenSetup, modifier = Modifier.fillMaxWidth())
        OutlineButton(text = "Back to Home", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
