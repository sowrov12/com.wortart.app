package com.wortart.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.ui.theme.WortArtColors

/**
 * A vocabulary flashcard in the dark-gold style.
 *
 * Shows the German word, audio button, CEFR level badge, grammar tag, English and
 * Bengali translations, and an example sentence with translations.
 *
 * This composable is stateless: favorites and audio are handled through callbacks.
 *
 * @param onSpeak called when the audio button is tapped (Task 2.3 connects TextToSpeech).
 * @param isSpeaking highlights the audio button while pronunciation is playing.
 * @param choiceContent optional slot below the example, used by Task 2.2 for the
 *   haben/sein and prefix option buttons.
 */
@Composable
fun FlashcardView(
    card: VocabEntity,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
    choiceContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = WortArtColors.Gold.copy(alpha = 0.25f),
                spotColor = WortArtColors.Gold.copy(alpha = 0.25f),
            ),
        shape = shape,
        color = WortArtColors.Surface,
        border = BorderStroke(1.dp, WortArtColors.Gold.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelBadge(card.level)
                Spacer(Modifier.size(8.dp))
                GrammarTag(card.grammarTag())
                Spacer(Modifier.weight(1f))
                FavoriteButton(isFavorite = card.isFavorite, onClick = onToggleFavorite)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = card.word,
                color = WortArtColors.TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            AudioButton(word = card.word, isSpeaking = isSpeaking, onClick = onSpeak)

            Spacer(Modifier.height(20.dp))
            GoldDivider()
            Spacer(Modifier.height(20.dp))

            Translation(label = "ENGLISH", text = card.translationEn, color = WortArtColors.TextPrimary)
            Spacer(Modifier.height(14.dp))
            Translation(label = "বাংলা", text = card.translationBn, color = WortArtColors.GoldLight)

            Spacer(Modifier.height(24.dp))

            ExampleBox(card)

            if (choiceContent != null) {
                Spacer(Modifier.height(20.dp))
                choiceContent()
            }
        }
    }
}

@Composable
internal fun LevelBadge(level: String) {
    Text(
        text = level,
        color = WortArtColors.OnGold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(WortArtColors.Gold, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun GrammarTag(text: String) {
    Text(
        text = text,
        color = WortArtColors.Gold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
            .size(60.dp)
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
    Canvas(modifier = Modifier.size(26.dp)) {
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
private fun GoldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WortArtColors.Gold.copy(alpha = 0.25f)),
    )
}

@Composable
private fun Translation(label: String, text: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = WortArtColors.TextMuted,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = text,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExampleBox(card: VocabEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WortArtColors.SurfaceDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "EXAMPLE",
            color = WortArtColors.TextMuted,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = highlightWord(card.exampleDe, card.word),
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
        )
        Text(text = card.exampleEn, color = WortArtColors.TextSecondary, fontSize = 15.sp)
        Text(text = card.exampleBn, color = WortArtColors.GoldLight, fontSize = 15.sp)
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
