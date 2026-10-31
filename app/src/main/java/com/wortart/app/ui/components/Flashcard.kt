package com.wortart.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.question.shortMeaning
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts
import com.wortart.app.ui.theme.levelColor

/**
 * A vocabulary flashcard: level badge and grammar tag, the German word in serif type with an
 * audio button, the meaning in English and Bengali on one line, and an example sentence.
 *
 * Stateless. The answer buttons are shown by the screen below the card.
 *
 * @param onSpeak called when the audio button is tapped.
 * @param isSpeaking fills the audio button while pronunciation is playing.
 */
@Composable
fun FlashcardView(
    card: VocabEntity,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = WortArtColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, WortArtColors.Border),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelBadge(card.level)
                GrammarTag(card.grammarTag())
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.word,
                    color = WortArtColors.TextPrimary,
                    fontSize = 34.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                AudioButton(word = card.word, isSpeaking = isSpeaking, onClick = onSpeak)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = card.shortMeaning(),
                color = WortArtColors.TextSecondary,
                fontSize = 16.sp,
            )

            Spacer(Modifier.height(18.dp))
            ExampleBox(card)
        }
    }
}

/** The CEFR level as a colored rounded badge (A1 green, A2 gold, B1 orange, B2 pink). */
@Composable
internal fun LevelBadge(level: String) {
    Text(
        text = level,
        color = WortArtColors.OnGold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(levelColor(level), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun GrammarTag(text: String) {
    Text(
        text = text,
        color = WortArtColors.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(WortArtColors.Elevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = WortArtColors.Gold,
        )
    }
}

@Composable
private fun AudioButton(word: String, isSpeaking: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (isSpeaking) WortArtColors.Gold else Color.Transparent,
        label = "audioButtonBackground",
    )
    val iconColor = if (isSpeaking) WortArtColors.OnGold else WortArtColors.Gold

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, WortArtColors.Gold, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Play pronunciation of $word" },
        contentAlignment = Alignment.Center,
    ) {
        SpeakerIcon(color = iconColor)
    }
}

/** Speaker icon drawn on a Canvas, so no icon library is needed. */
@Composable
private fun SpeakerIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height

        val body = Path().apply {
            moveTo(w * 0.10f, h * 0.38f)
            lineTo(w * 0.30f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.18f)
            lineTo(w * 0.52f, h * 0.82f)
            lineTo(w * 0.30f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.62f)
            close()
        }
        drawPath(path = body, color = color)

        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.40f, h * 0.30f),
            size = Size(w * 0.40f, h * 0.40f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.26f, h * 0.16f),
            size = Size(w * 0.68f, h * 0.68f),
            style = stroke,
        )
    }
}

@Composable
private fun ExampleBox(card: VocabEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WortArtColors.SurfaceDark, RoundedCornerShape(14.dp))
            .border(1.dp, WortArtColors.Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = highlightWord(card.exampleDe, card.word),
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
        )
        Text(text = card.exampleEn, color = WortArtColors.TextSecondary, fontSize = 14.sp)
        Text(text = card.exampleBn, color = WortArtColors.TextSecondary, fontSize = 14.sp)
    }
}

/** Highlights the first whole-word match of [word] in [text] in gold. */
private fun highlightWord(text: String, word: String): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("(?<!\\p{L})" + Regex.escape(word) + "(?!\\p{L})", RegexOption.IGNORE_CASE)
    val match = pattern.find(text)
    if (match == null) {
        append(text)
    } else {
        append(text.substring(0, match.range.first))
        withStyle(SpanStyle(color = WortArtColors.Gold, fontWeight = FontWeight.Bold)) {
            append(text.substring(match.range.first, match.range.last + 1))
        }
        append(text.substring(match.range.last + 1))
    }
}
