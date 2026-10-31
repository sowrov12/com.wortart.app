package com.wortart.app.ui.practice

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.domain.question.ChoiceQuestion
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.ui.theme.WortArtColors

private enum class OptionLook { Idle, Selected, Correct, Wrong, Dimmed }

/**
 * The answer choices of a [ChoiceQuestion]. The layout depends on the question:
 * prefixes as a row of chips, haben/sein as two large buttons, meanings as a list with
 * radio buttons.
 *
 * Choosing only marks an option; after the answer was submitted ([submitted]) the correct
 * option turns green, a wrong choice red, and the options are locked.
 */
@Composable
fun AnswerOptions(
    question: ChoiceQuestion,
    selected: String?,
    submitted: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun lookOf(option: String): OptionLook = when {
        !submitted -> if (option == selected) OptionLook.Selected else OptionLook.Idle
        option == question.correctAnswer -> OptionLook.Correct
        option == selected -> OptionLook.Wrong
        else -> OptionLook.Dimmed
    }

    when (question.type) {
        QuestionType.PREFIX, QuestionType.AUXILIARY -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            question.options.forEach { option ->
                ChipOption(
                    text = option,
                    look = lookOf(option),
                    enabled = !submitted,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    large = question.type == QuestionType.AUXILIARY,
                )
            }
        }

        QuestionType.MEANING, QuestionType.QUIZ -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            question.options.forEach { option ->
                RadioOption(
                    text = option,
                    look = lookOf(option),
                    enabled = !submitted,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun borderColorOf(look: OptionLook): Color {
    val target = when (look) {
        OptionLook.Idle -> WortArtColors.Border
        OptionLook.Selected -> WortArtColors.Gold
        OptionLook.Correct -> WortArtColors.Correct
        OptionLook.Wrong -> WortArtColors.Wrong
        OptionLook.Dimmed -> WortArtColors.Border.copy(alpha = 0.5f)
    }
    val animated by animateColorAsState(targetValue = target, label = "optionBorder")
    return animated
}

@Composable
private fun fillColorOf(look: OptionLook): Color {
    val target = when (look) {
        OptionLook.Selected -> WortArtColors.Gold.copy(alpha = 0.12f)
        OptionLook.Correct -> WortArtColors.Correct.copy(alpha = 0.18f)
        OptionLook.Wrong -> WortArtColors.Wrong.copy(alpha = 0.18f)
        else -> WortArtColors.Elevated
    }
    val animated by animateColorAsState(targetValue = target, label = "optionFill")
    return animated
}

private fun textColorOf(look: OptionLook): Color = when (look) {
    OptionLook.Idle -> WortArtColors.TextPrimary
    OptionLook.Selected -> WortArtColors.Gold
    OptionLook.Correct -> WortArtColors.Correct
    OptionLook.Wrong -> WortArtColors.Wrong
    OptionLook.Dimmed -> WortArtColors.TextMuted
}

@Composable
private fun ChipOption(
    text: String,
    look: OptionLook,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = if (large) 56.dp else 50.dp)
            .clip(shape)
            .background(fillColorOf(look))
            .border(1.5.dp, borderColorOf(look), shape)
            .selectable(
                selected = look == OptionLook.Selected || look == OptionLook.Correct,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColorOf(look),
            fontSize = if (large) 18.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RadioOption(
    text: String,
    look: OptionLook,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(fillColorOf(look))
            .border(1.5.dp, borderColorOf(look), shape)
            .selectable(
                selected = look == OptionLook.Selected || look == OptionLook.Correct,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(look)
        Spacer(Modifier.size(12.dp))
        Text(text = text, color = textColorOf(look), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RadioDot(look: OptionLook) {
    val ring = borderColorOf(look)
    val filled = look == OptionLook.Selected || look == OptionLook.Correct || look == OptionLook.Wrong
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (filled) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ring, CircleShape),
            )
        }
    }
}
