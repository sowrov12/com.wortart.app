package com.wortart.app.ui.practice

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.ui.components.FavoriteButton
import com.wortart.app.ui.components.FlashcardView
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.components.GoldProgressBar
import com.wortart.app.ui.components.OutlineButton
import com.wortart.app.ui.components.WortArtTopBar
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onClose)

    PracticeScreen(
        state = state,
        speakingCardId = speakingCardId,
        onBack = onClose,
        onSpeak = onSpeak,
        onToggleFavorite = viewModel::toggleFavorite,
        onSelect = viewModel::select,
        onSubmit = viewModel::submit,
        onNext = viewModel::next,
        onRestart = viewModel::startNew,
        modifier = modifier,
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
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (state.status) {
            PracticeStatus.LOADING -> {
                WortArtTopBar(title = "Flashcards", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WortArtColors.Gold)
                }
            }

            PracticeStatus.EMPTY -> {
                WortArtTopBar(title = "Flashcards", onBack = onBack)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No flashcards yet",
                        color = WortArtColors.TextPrimary,
                        fontSize = 20.sp,
                        fontFamily = WortArtFonts.Serif,
                    )
                    Text(
                        text = "The vocabulary is still loading. Try again in a moment.",
                        color = WortArtColors.TextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    OutlineButton(text = "Try again", onClick = onRestart)
                }
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
            )

            PracticeStatus.FINISHED -> SessionSummary(
                state = state,
                onBack = onBack,
                onRestart = onRestart,
            )
        }
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
) {
    val session = state.current ?: return
    val card = session.card
    val question = session.question

    WortArtTopBar(
        title = "Flashcards",
        onBack = onBack,
        trailing = { FavoriteButton(isFavorite = card.isFavorite, onClick = onToggleFavorite) },
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${state.index + 1} / ${state.total}",
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }
        GoldProgressBar(fraction = state.progress)

        FlashcardView(
            card = card,
            onSpeak = { onSpeak(card.id, card.word) },
            isSpeaking = card.id == speakingCardId,
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
        text = when {
            !state.submitted -> "Submit"
            state.isLastCard -> "Finish"
            else -> "Next"
        },
        enabled = state.submitted || state.selected != null,
        onClick = if (state.submitted) onNext else onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun AnswerFeedback(isCorrect: Boolean, correctAnswer: String, explanation: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (isCorrect) "✓ Correct!" else "✗ Not quite — the answer is $correctAnswer",
            color = if (isCorrect) WortArtColors.Correct else WortArtColors.Wrong,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = explanation, color = WortArtColors.TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun SessionSummary(state: PracticeUiState, onBack: () -> Unit, onRestart: () -> Unit) {
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
        OutlineButton(text = "Back to Home", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
