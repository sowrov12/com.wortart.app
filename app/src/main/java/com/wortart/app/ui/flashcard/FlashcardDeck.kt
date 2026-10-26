package com.wortart.app.ui.flashcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.question.ChoiceQuestionBuilder
import com.wortart.app.ui.components.ChoiceQuestionView
import com.wortart.app.ui.components.FlashcardView
import com.wortart.app.ui.theme.WortArtColors

/**
 * Browses [words] one flashcard at a time with animated Previous / Next navigation.
 *
 * Verbs get answer buttons (haben/sein, or the separable prefix). The chosen answer is
 * remembered per card while the deck is on screen, and each card can be answered once.
 *
 * @param onAnswered called once per card with whether the answer was correct
 *   (Task 4.1 will use it for statistics).
 */
@Composable
fun FlashcardDeck(
    words: List<VocabEntity>,
    onSpeak: (VocabEntity) -> Unit,
    onToggleFavorite: (VocabEntity) -> Unit,
    modifier: Modifier = Modifier,
    onAnswered: (card: VocabEntity, isCorrect: Boolean) -> Unit = { _, _ -> },
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }

    if (words.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Loading words…",
                color = WortArtColors.TextSecondary,
                fontSize = 16.sp,
            )
        }
        return
    }

    val lastIndex = words.lastIndex
    val current = index.coerceIn(0, lastIndex)

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${current + 1} / ${words.size}",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(12.dp))

        // Animate on the position only, so toggling a favorite does not replay the animation.
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { width -> if (forward) width else -width } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> if (forward) -width else width } + fadeOut())
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "flashcardTransition",
        ) { position ->
            val card = words.getOrNull(position)
            if (card != null) {
                val question = remember(card.id) { ChoiceQuestionBuilder.build(card) }

                val choiceContent: (@Composable ColumnScope.() -> Unit)? =
                    if (question == null) {
                        null
                    } else {
                        {
                            ChoiceQuestionView(
                                question = question,
                                selected = answers[card.id],
                                onSelect = { option ->
                                    if (answers[card.id] == null) {
                                        answers[card.id] = option
                                        onAnswered(card, option == question.correctAnswer)
                                    }
                                },
                            )
                        }
                    }

                FlashcardView(
                    card = card,
                    onSpeak = { onSpeak(card) },
                    onToggleFavorite = { onToggleFavorite(card) },
                    modifier = Modifier.fillMaxSize(),
                    choiceContent = choiceContent,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { index = (current - 1).coerceAtLeast(0) },
                enabled = current > 0,
                modifier = Modifier.weight(1f),
                border = BorderStroke(
                    1.dp,
                    if (current > 0) WortArtColors.Gold else WortArtColors.TextMuted,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WortArtColors.Gold,
                    disabledContentColor = WortArtColors.TextMuted,
                ),
            ) {
                Text("Previous")
            }
            Button(
                onClick = { index = (current + 1).coerceAtMost(lastIndex) },
                enabled = current < lastIndex,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WortArtColors.Gold,
                    contentColor = WortArtColors.OnGold,
                    disabledContainerColor = WortArtColors.Surface,
                    disabledContentColor = WortArtColors.TextMuted,
                ),
            ) {
                Text("Next")
            }
        }
    }
}
