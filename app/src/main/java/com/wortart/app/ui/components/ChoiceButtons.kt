package com.wortart.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.domain.question.ChoiceQuestion
import com.wortart.app.ui.theme.WortArtColors

private enum class OptionState { Idle, Correct, Wrong, Dimmed }

/**
 * Shows a [ChoiceQuestion]: the prompt, option buttons (two per row) and,
 * once answered, feedback with a short explanation.
 *
 * Stateless: the caller owns [selected] (null = not answered yet) and decides
 * what happens in [onSelect]. After an answer the buttons are disabled.
 */
@Composable
fun ChoiceQuestionView(
    question: ChoiceQuestion,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered = selected != null

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = question.prompt,
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        question.options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowOptions.forEach { option ->
                    OptionButton(
                        text = option,
                        state = optionState(option, selected, question.correctAnswer),
                        enabled = !answered,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        AnimatedVisibility(visible = answered) {
            val isCorrect = selected == question.correctAnswer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isCorrect) {
                        "✓ Correct!"
                    } else {
                        "✗ Not quite — the answer is ${question.correctAnswer}"
                    },
                    color = if (isCorrect) WortArtColors.Correct else WortArtColors.Wrong,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = question.explanation,
                    color = WortArtColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun optionState(option: String, selected: String?, correct: String): OptionState = when {
    selected == null -> OptionState.Idle
    option == correct -> OptionState.Correct
    option == selected -> OptionState.Wrong
    else -> OptionState.Dimmed
}

@Composable
private fun OptionButton(
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (state) {
        OptionState.Idle -> WortArtColors.Gold
        OptionState.Correct -> WortArtColors.Correct
        OptionState.Wrong -> WortArtColors.Wrong
        OptionState.Dimmed -> WortArtColors.TextMuted
    }
    val textColor = when (state) {
        OptionState.Idle -> WortArtColors.TextPrimary
        OptionState.Correct -> WortArtColors.Correct
        OptionState.Wrong -> WortArtColors.Wrong
        OptionState.Dimmed -> WortArtColors.TextMuted
    }
    val fill by animateColorAsState(
        targetValue = when (state) {
            OptionState.Correct -> WortArtColors.Correct.copy(alpha = 0.18f)
            OptionState.Wrong -> WortArtColors.Wrong.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        label = "optionFill",
    )
    val borderColor by animateColorAsState(targetValue = accent, label = "optionBorder")
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, borderColor, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
